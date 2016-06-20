/**
 *    ||          ____  _ __
 * +------+      / __ )(_) /_______________ _____  ___
 * | 0xBC |     / __  / / __/ ___/ ___/ __ `/_  / / _ \
 * +------+    / /_/ / / /_/ /__/ /  / /_/ / / /_/  __/
 *  ||  ||    /_____/_/\__/\___/_/   \__,_/ /___/\___/
 *
 * Copyright (C) 2015 Bitcraze AB
 *
 * Crazyflie Nano Quadcopter Client
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */

package se.bitcraze.crazyflie.lib.bootloader;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import se.bitcraze.crazyflie.lib.bootloader.Target.TargetTypes;
import se.bitcraze.crazyflie.lib.bootloader.Utilities.BootVersion;
import se.bitcraze.crazyflie.lib.crazyradio.ConnectionData;
import se.bitcraze.crazyflie.lib.crtp.CrtpDriver;

/**
 * Bootloading utilities for the Crazyflie.
 *
 */
//TODO: fix targetId and addr confusion
//TODO: fix warmboot
public class Bootloader {

    final Logger mLogger = LoggerFactory.getLogger("Bootloader");

    private static ObjectMapper mMapper = new ObjectMapper(); // can reuse, share globally
    private static final String MANIFEST_FILENAME = "manifest.json";
    private Cloader mCload;
    private boolean mCancelled = false;
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
        mMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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
                try {
                    this.mCload.openBootloaderConnection(bootloaderConnection);
                    started = this.mCload.checkLinkAndGetInfo(TargetTypes.STM32); //TODO: what is the real parameter for this?
                } catch (IOException e) {
                    mLogger.warn(e.getMessage());
                    started = false;
                }
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
        FlashTarget toFlash = new FlashTarget(target, data, "CF1 config", configPage);
        internalFlash(toFlash);
    }

    //TODO: improve
    public static byte[] readFile(File file) throws IOException {
        byte[] fileData = new byte[(int) file.length()];
        Logger logger = LoggerFactory.getLogger("Bootloader");
        logger.debug("readFile: " + file.getName() +  ", size: " +  file.length());
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file.getAbsoluteFile(), "r");
            raf.readFully(fileData);
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException ioe) {
                    logger.error(ioe.getMessage());
                }
            }
        }
        return fileData;
    }

    public boolean flash(File file) throws IOException {
        // assume stm32 if no target name is specified and file extension is ".bin"
        if (file.getName().endsWith(".bin")) {
            mLogger.info("Assuming STM32 for file " + file.getName() + ".");
            return flash(file, "stm32");
        }
        return flash(file, "");
    }

    public boolean flash(File file, String... targetNames) throws IOException {
        List<FlashTarget> filesToFlash = getFlashTargets(file, targetNames);
        if (filesToFlash.isEmpty()) {
            mLogger.error("Found no files to flash.");
            return false;
        }
        int fileCounter = 0;
        for (FlashTarget ft : filesToFlash) {
            internalFlash(ft, fileCounter, filesToFlash.size());
            fileCounter++;
        }
        return true;
    }

    private List<FlashTarget> getFlashTargets(File file, String... targetNames) throws IOException {
        List<FlashTarget> filesToFlash = new ArrayList<FlashTarget>();

        if (!file.exists()) {
            mLogger.error(file + " not found.");
            return filesToFlash;
        }

        // check if supplied targetNames are known TargetTypes, if so, continue, else return

        if (isZipFile(file)) {
            // unzip
            unzip(file);

            // read manifest.json
            File basePath = new File(file.getAbsoluteFile().getParent() + "/" + getFileNameWithoutExtension(file));
            File manifestFile = new File(basePath.getAbsolutePath(), MANIFEST_FILENAME);
            if (basePath.exists() && manifestFile.exists()) {
                Manifest mf = null;
                try {
                    mf = readManifest(manifestFile);
                } catch (IOException ioe) {
                    mLogger.error("Error while trying to read manifest file:\n" + ioe.getMessage());
                }
                //TODO: improve error handling
                if (mf == null) {
                    return filesToFlash;
                }
                Set<String> files = mf.getFiles().keySet();

                // iterate over file names in manifest.json
                for (String fileName : files) {
                    FirmwareDetails firmwareDetails = mf.getFiles().get(fileName);
                    Target t = this.mCload.getTargets().get(TargetTypes.fromString(firmwareDetails.getTarget()));
                    if (t != null) {
                        // use path to extracted file
                        //File flashFile = new File(file.getParent() + "/" + file.getName() + "/" + fileName);
                        File flashFile = new File(basePath.getAbsolutePath(), fileName);
                        FlashTarget ft = new FlashTarget(t, readFile(flashFile), firmwareDetails.getType(), t.getStartPage()); //TODO: does startPage HAVE to be an extra argument!? (it's already included in Target)
                        // add flash target
                        // if no target names are specified, flash everything
                        if (targetNames == null || targetNames.length == 0 || targetNames[0].isEmpty()) {
                            // deal with different platforms (CF1, CF2)
                            // TODO: simplify
                            if (t.getFlashPages() == 128 && "cf1".equalsIgnoreCase(firmwareDetails.getPlatform())) { //128 = CF 1.0
                                filesToFlash.add(ft);
                                // deal with STM32 and NRF51 for CF2 (different no of flash pages)
                            } else if ((t.getFlashPages() == 1024 || t.getFlashPages() == 232) && "cf2".equalsIgnoreCase(firmwareDetails.getPlatform())) { //1024 = CF 2.0
                                filesToFlash.add(ft);
                            }
                        } else {
                            // else flash only files whose targets are contained in targetNames
                            if (Arrays.asList(targetNames).contains(firmwareDetails.getTarget())) {
                                filesToFlash.add(ft);
                            }
                        }
                    } else {
                        mLogger.error("No target found for " + firmwareDetails.getTarget());
                    }
                }
            } else {
                mLogger.error("Zip file " + file.getName() + " does not include a " + MANIFEST_FILENAME);
            }
        } else { // File is not a Zip file
            // add single flash target
            if (targetNames == null || targetNames.length != 1) {
                mLogger.error("Not an archive, must supply ONE target to flash.");
            } else {
                for (String tn : targetNames) {
                    if (!tn.isEmpty()) {
                        Target target = this.mCload.getTargets().get(TargetTypes.fromString(tn));
                        FlashTarget ft = new FlashTarget(target, readFile(file), "binary", target.getStartPage());
                        filesToFlash.add(ft);
                    }
                }
            }
        }
        return filesToFlash;
    }

    public void unzip(File zipFile) {
        mLogger.debug("Trying to unzip " + zipFile + "...");
        InputStream fis = null;
        ZipInputStream zis = null;
        FileOutputStream fos = null;
        String parent = zipFile.getAbsoluteFile().getParent();

        try {
            fis = new FileInputStream(zipFile);
            zis = new ZipInputStream(new BufferedInputStream(fis));
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int count;
                while ((count = zis.read(buffer)) != -1) {
                    baos.write(buffer, 0, count);
                }
                String filename = ze.getName();
                byte[] bytes = baos.toByteArray();
                // write files
                File filePath = new File(parent + "/" + getFileNameWithoutExtension(zipFile) + "/" + filename);
                // create subdir
                filePath.getParentFile().mkdirs();
                fos = new FileOutputStream(filePath);
                fos.write(bytes);
                //check
                if(filePath.exists() && filePath.length() > 0) {
                    mLogger.debug(filename + " successfully extracted to " + filePath.getAbsolutePath());
                } else {
                    mLogger.error(filename + " was not extracted.");
                }
            }
        } catch (FileNotFoundException ffe) {
            mLogger.error(ffe.getMessage());
        } catch (IOException ioe) {
            mLogger.error(ioe.getMessage());
        } finally {
            if (zis != null) {
                try {
                    zis.close();
                } catch (IOException e) {
                    mLogger.error(e.getMessage());
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    mLogger.error(e.getMessage());
                }
            }

        }
    }

    public static String getFileNameWithoutExtension (File file) {
        return file.getName().substring(0, file.getName().length()-4);
    }

    /**
     * Basic check if a file is a Zip file
     *
     * @param file
     * @return true if file is a Zip file, false otherwise
     */
    //TODO: how can this be improved?
    private boolean isZipFile(File file) {
        if (file != null && file.exists() && file.getName().endsWith(".zip")) {
            ZipFile zf = null;
            try {
                zf = new ZipFile(file);
                return zf.size() > 0;
            } catch (ZipException e) {
                mLogger.error(e.getMessage());
            } catch (IOException e) {
                mLogger.error(e.getMessage());
            } finally {
                if (zf != null) {
                    try {
                        zf.close();
                    } catch (IOException e) {
                        mLogger.error(e.getMessage());
                    }
                }
            }
        }
        return false;
    }

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

    public void internalFlash(FlashTarget target) {
        internalFlash(target, 1, 1);
    }

    // def _internal_flash(self, target, current_file_number=1, total_files=1):
    public void internalFlash(FlashTarget flashTarget, int currentFileNo, int totalFiles) {
        Target t_data = flashTarget.getTarget();
        byte[] image = flashTarget.getData();
        int pageSize = t_data.getPageSize();
        int startPage = flashTarget.getStartPage();

        String flashingTo = "Flashing to " + TargetTypes.toString(t_data.getId()) + " (" + flashTarget.getType() + ")";
        mLogger.info(flashingTo);
        notifyUpdateStatus(flashingTo);

        //if len(image) > ((t_data.flash_pages - start_page) * t_data.page_size):
        if (image.length > ((t_data.getFlashPages() - startPage) * pageSize)) {
            mLogger.error("Error: Not enough space to flash the image file.");
            //raise Exception()
            return;
        }

        int noOfPages = (image.length / pageSize) + 1;
        mLogger.info(image.length - 1 + " bytes (" + noOfPages + " pages) ");

        // For each page
        int bufferCounter = 0; // Buffer counter
        int i = 0;
        for (i = 0; i < ((image.length - 1) / pageSize) + 1; i++) {
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

            notifyUpdateProgress(i+1, noOfPages);

            if (isCancelled()) {
                break;
            }

            this.mCload.uploadBuffer(t_data.getId(), bufferCounter, 0, buffer);

            bufferCounter++;

            // Flash when the complete buffers are full
            if (bufferCounter >= t_data.getBufferPages()) {
                String buffersFull = "Flashing page " + (i+1) + "...";
                mLogger.info(buffersFull);
                notifyUpdateStatus(buffersFull);
                notifyUpdateProgress(i+1, noOfPages);
                if (!this.mCload.writeFlash(t_data.getId(), 0, startPage + i - (bufferCounter - 1), bufferCounter)) {
                    handleFlashError();
                    //raise Exception()
                    return;
                }
                bufferCounter = 0;
            }
        }
        if (isCancelled()) {
            mLogger.info("Flashing cancelled!");
            return;
        }
        if (bufferCounter > 0) {
            mLogger.info("BufferCounter: " + bufferCounter);
            notifyUpdateProgress(i, noOfPages);
            if (!this.mCload.writeFlash(t_data.getId(), 0, (startPage + ((image.length - 1) / pageSize)) - (bufferCounter - 1), bufferCounter)) {
                handleFlashError();
                //raise Exception()
                return;
            }
        }
        mLogger.info("Flashing done!");
        notifyUpdateStatus("Flashing done!");
    }

    private boolean isCancelled() {
        return mCancelled;
    }

    public void cancel() {
        this.mCancelled = true;
        if (mCload != null) {
            mCload.cancel();
        }
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

    public void notifyUpdateProgress(int progress, int max) {
        for (BootloaderListener bootloaderListener : mBootloaderListeners) {
            bootloaderListener.updateProgress(progress, max);
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

        public void updateProgress(int progress, int max);

        public void updateStatus(String status);

        public void updateError(String error);

    }

    public class FlashTarget {

        private Target mTarget;
        private byte[] mData = new byte[0];
        private String mType = "";
        private int mStartPage;

        public FlashTarget(Target target, byte[] data, String type, int startPage) {
            this.mTarget = target;
            this.mData = data;
            this.mType = type;
            this.mStartPage = startPage;
        }

        public byte[] getData() {
            return mData;
        }

        public Target getTarget() {
            return mTarget;
        }

        public int getStartPage() {
            return mStartPage;
        }

        public String getType() {
            return mType;
        }

        @Override
        public String toString() {
            return "FlashTarget [target ID=" + TargetTypes.toString(mTarget.getId()) + ", data.length=" + mData.length + ", type=" + mType + ", startPage=" + mStartPage + "]";
        }

    }

    public static Manifest readManifest (File file) throws IOException {
        String errorMessage = "";
        try {
            return mMapper.readValue(file, Manifest.class);
        } catch (JsonParseException jpe) {
            errorMessage = jpe.getMessage();
        } catch (JsonMappingException jme) {
            errorMessage = jme.getMessage();
        }
        LoggerFactory.getLogger("Bootloader").error("Error while parsing manifest " + file.getName() + ": " + errorMessage);
        return null;
    }

    public static void writeManifest (String fileName, Manifest manifest) throws IOException {
        String errorMessage = "";
        mMapper.enable(SerializationFeature.INDENT_OUTPUT);
        try {
            mMapper.writeValue(new File(fileName), manifest);
            return;
        } catch (JsonGenerationException jge) {
            errorMessage = jge.getMessage();
        } catch (JsonMappingException jme) {
            errorMessage = jme.getMessage();
        }
        LoggerFactory.getLogger("Bootloader").error("Could not save manifest to file " + fileName + ".\n" + errorMessage);
    }
}
