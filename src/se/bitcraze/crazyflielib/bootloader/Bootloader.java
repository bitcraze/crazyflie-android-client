package se.bitcraze.crazyflielib.bootloader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.bitcraze.crazyflielib.bootloader.Target.TargetTypes;
import se.bitcraze.crazyflielib.bootloader.Utilities.BootVersion;
import se.bitcraze.crazyflielib.crazyradio.ConnectionData;
import se.bitcraze.crazyflielib.crtp.CrtpDriver;

/**
 * Bootloading utilities for the Crazyflie.
 *
 */
//TODO: fix targetId and addr confusion
//TODO: add flash method (for multiple targets)
//TODO: fix warmboot
public class Bootloader {

    final Logger mLogger = LoggerFactory.getLogger("Bootloader");

    private Cloader mCload;

    private List<BootloaderListener> mBootloaderListeners;

    /**
     * Init the communication class by starting to communicate with the
     * link given. clink is the link address used after resetting to the
     * bootloader.
     *
     * The device is actually considered in firmware mode.
     */
    public Bootloader(CrtpDriver driver) {
        this.mCload = new Cloader(driver);
        this.mBootloaderListeners = Collections.synchronizedList(new LinkedList<BootloaderListener>());
    }

    public Cloader getCloader() {
        return this.mCload;
    }

    public boolean startBootloader(boolean warmboot) {
        boolean started = false;

        if (warmboot) {
            mLogger.info("startBootloader: warmboot");

            //TODO
            //self._cload.open_bootloader_uri(self.clink)
            //this.mCload.openBootloaderConnection(connectionData);
            started = this.mCload.resetToBootloader(TargetTypes.NRF51); //is NRF51 correct here?
            if (started) {
                started = this.mCload.checkLinkAndGetInfo(TargetTypes.STM32);
            }
        } else {
            mLogger.info("startBootloader: coldboot");
            ConnectionData bootloaderConnection = this.mCload.scanForBootloader();

            // Workaround for libusb on Windows (open/close too fast)
            //time.sleep(1)

            if (bootloaderConnection != null) {
                mLogger.info("startBootloader: bootloader connection found");
                this.mCload.openBootloaderConnection(bootloaderConnection);
                started = this.mCload.checkLinkAndGetInfo(TargetTypes.STM32); //TODO: what is the real parameter for this?
            } else {
                mLogger.info("startBootloader: bootloader connection NOT found");
                started = false;
            }

            if (started) {
                int protocolVersion = this.mCload.getProtocolVersion();
                if (protocolVersion == BootVersion.CF1_PROTO_VER_0 ||
                    protocolVersion == BootVersion.CF1_PROTO_VER_1) {
                    // Nothing to do
                } else if (protocolVersion == BootVersion.CF2_PROTO_VER) {
                    this.mCload.requestInfoUpdate(TargetTypes.NRF51);
                } else {
                    mLogger.debug("Bootloader protocol " + String.format("0x%02X", protocolVersion) + " not supported!");
                }

                mLogger.info("startBootloader: started");
            } else {
                mLogger.info("startBootloader: not started");
            }
        }
        return started;
    }

    public Target getTarget(int targetId) {
        return this.mCload.requestInfoUpdate(targetId);
    }

    public int getProtocolVersion() {
        return this.mCload.getProtocolVersion();
    }

    /**
     * Read a flash page from the specified target
     */
    public byte[] readCF1Config() {
        Target target = this.mCload.getTargets().get(TargetTypes.STM32); //CF1
        int configPage = target.getFlashPages() - 1;

        return this.mCload.readFlash(0xFF, configPage);
    }

    public void writeCF1Config(byte[] data) {
        Target target = this.mCload.getTargets().get(TargetTypes.STM32); //CF1
        int configPage = target.getFlashPages() - 1;

        //to_flash = {"target": target, "data": data, "type": "CF1 config", "start_page": config_page}
        internalFlash(target, data, "CF1 config", configPage);
    }

    public void flash(File file, int targetId) {
        if (!file.exists()) {
            mLogger.error("File " + file + " does not exist.");
            return;
        }

        Target target = this.mCload.getTargets().get(targetId);
        byte[] fileData = readFile(file);
        if (fileData.length > 0) {
            internalFlash(target, fileData, "Firmware", target.getStartPage());
        } else {
            mLogger.error("File size is 0.");
        }
    }

    //TODO: improve
    private byte[] readFile(File file) {
        byte[] fileData = new byte[(int) file.length()];
        mLogger.debug("File size: " +  file.length());
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file.getAbsoluteFile(), "r");
            raf.readFully(fileData);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return fileData;
    }

    // TODO: def flash(self, filename, targets):


    /**
     * Reset to firmware depending on protocol version
     *
     * @return
     */
    public boolean resetToFirmware() {
        int targetType = -1;
        if (this.mCload.getProtocolVersion() == BootVersion.CF2_PROTO_VER) {
            targetType = TargetTypes.NRF51;
        } else {
            targetType = TargetTypes.STM32;
        }
        return this.mCload.resetToFirmware(targetType);
    }

    public void close() {
        mLogger.debug("Bootloader close()");
        if (this.mCload != null) {
            this.mCload.close();
        }
    }

    // def _internal_flash(self, target, current_file_number=1, total_files=1):
    public void internalFlash(Target target, byte[] data, String type, int startPage) {
        byte[] image = data;
        Target t_data = target;
        int pageSize = t_data.getPageSize();

        String flashingTo = "Flashing to " + TargetTypes.toString(t_data.getId()) + " (" + type + ")";
        mLogger.info(flashingTo);
        notifyUpdateStatus(flashingTo);

        //if len(image) > ((t_data.flash_pages - start_page) * t_data.page_size):
        if (image.length > ((t_data.getFlashPages() - startPage) * pageSize)) {
            mLogger.error("Error: Not enough space to flash the image file.");
            //raise Exception()
            return;
        }

        mLogger.info(image.length - 1 + " bytes (" + ((image.length / pageSize) + 1) + " pages) ");

        // For each page
        int bufferCounter = 0; // Buffer counter
        for (int i = 0; i < ((image.length - 1) / pageSize) + 1; i++) {
            // Load the buffer
            int end = 0;
            if (((i + 1) * pageSize) > image.length) {
                //buff = image[i * t_data.page_size:]
                end = image.length;
            } else {
                //buff = image[i * t_data.page_size:(i + 1) * t_data.page_size])
                end = (i + 1) * pageSize;
            }
            byte[] buffer = Arrays.copyOfRange(image, i * pageSize, end);
            this.mCload.uploadBuffer(t_data.getId(), bufferCounter, 0, buffer);

            bufferCounter++;

            // Flash when the complete buffers are full
            if (bufferCounter >= t_data.getBufferPages()) {
                String buffersFull = "Buffers full. Flashing page " + (i+1) + "...";
                mLogger.info(buffersFull);
                notifyUpdateStatus(buffersFull);
                notifyUpdateProgress(i+1);
                if (!this.mCload.writeFlash(t_data.getId(), 0, startPage + i - (bufferCounter - 1), bufferCounter)) {
                    handleFlashError();
                    //raise Exception()
                    return;
                }
                bufferCounter = 0;
            }
        }
        if (bufferCounter > 0) {
            mLogger.info("BufferCounter: " + bufferCounter);
            if (!this.mCload.writeFlash(t_data.getId(), 0, (startPage + ((image.length - 1) / pageSize)) - (bufferCounter - 1), bufferCounter)) {
                handleFlashError();
                //raise Exception()
                return;
            }
        }
        mLogger.info("Flashing done!");
        notifyUpdateStatus("Flashing done!");
    }

    private void handleFlashError() {
        String errorMessage = "Error during flash operation (" + this.mCload.getErrorMessage() + "). Maybe wrong radio link?";
        mLogger.error(errorMessage);
        notifyUpdateError(errorMessage);
    }



    public void addBootloaderListener(BootloaderListener bl) {
        this.mBootloaderListeners.add(bl);
    }

    public void removeBootloaderListener(BootloaderListener bl) {
        this.mBootloaderListeners.remove(bl);
    }

    public void notifyUpdateProgress(int progress) {
        for (BootloaderListener bootloaderListener : mBootloaderListeners) {
            bootloaderListener.updateProgress(progress);
        }
    }

    public void notifyUpdateStatus(String status) {
        for (BootloaderListener bootloaderListener : mBootloaderListeners) {
            bootloaderListener.updateStatus(status);
        }
    }

    public void notifyUpdateError(String error) {
        for (BootloaderListener bootloaderListener : mBootloaderListeners) {
            bootloaderListener.updateError(error);
        }
    }

    public interface BootloaderListener {

        public void updateProgress(int progress);

        public void updateStatus(String status);

        public void updateError(String error);

    }
}
