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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.bitcraze.crazyflie.lib.bootloader.Target.TargetTypes;
import se.bitcraze.crazyflie.lib.bootloader.Utilities.BootVersion;
import se.bitcraze.crazyflie.lib.crazyradio.ConnectionData;
import se.bitcraze.crazyflie.lib.crazyradio.Crazyradio;
import se.bitcraze.crazyflie.lib.crazyradio.RadioAck;
import se.bitcraze.crazyflie.lib.crazyradio.RadioDriver;
import se.bitcraze.crazyflie.lib.crtp.CrtpDriver;
import se.bitcraze.crazyflie.lib.crtp.CrtpPacket;
import se.bitcraze.crazyflie.lib.crtp.CrtpPort;
import se.bitcraze.crazyflie.lib.crtp.CrtpPacket.Header;

/**
 * Crazyflie radio bootloader for flashing firmware.
 *
 * Bootloader utility for the Crazyflie
 *
 */
//TODO: fix resetToBootloader methods
//TODO: fix callbacks
//TODO: use retryCounter
//TODO: new Address
public class Cloader {

    final Logger mLogger = LoggerFactory.getLogger("Cloader");

    private CrtpDriver mDriver;
    private List<ConnectionData> mAvailableBootConnections = new ArrayList<ConnectionData>();

    private Map<Integer, Target> mTargets = new HashMap<Integer, Target>();
    private String mErrorMessage = "";
    private int mProtocolVersion = 0xFF;

    private boolean mCancelled = false;

    // Bootloader commands
    public static int GET_INFO = 0x10;
    public static int SET_ADDRESS = 0x11; // Only implemented on Crazyflie version 0x00
    public static int GET_MAPPING = 0x12; // Only implemented in version 0x10 target 0xFF
    public static int LOAD_BUFFER = 0x14;
    public static int WRITE_FLASH = 0x18;
    public static int READ_FLASH = 0x1C;


    /**
     * Init the communication class by starting to communicate with the link given.
     * clink is the link address used after resetting to the bootloader.
     *
     * The device is actually considered to be in firmware mode.
     */
    // def __init__(self, link, info_cb=None, in_boot_cb=None):
    public Cloader(CrtpDriver driver) {
        this.mDriver = driver;

        //self._available_boot_uri = ("radio://0/110/2M", "radio://0/0/2M")
        mAvailableBootConnections.add(new ConnectionData(110, Crazyradio.DR_2MPS));
        mAvailableBootConnections.add(new ConnectionData(0, Crazyradio.DR_2MPS));
    }

    /**
     * Close the link
     */
    public void close() {
        if (this.mDriver != null) {
            this.mDriver.disconnect();
        }
    }

    /**
     * Scans for bootloader with the predefined channel/datarate combinations<br/>
     * Timeout is 10 seconds.
     *
     * @return always the first available bootloader connection
     */
    public ConnectionData scanForBootloader() {
        long startTime = System.currentTimeMillis();
        List<ConnectionData> resultList = new ArrayList<ConnectionData>();
        while (resultList.size() == 0 && (System.currentTimeMillis() - startTime) < 10000) {
            for (ConnectionData cd : mAvailableBootConnections) {
                if(this.mDriver.scanSelected(cd.getChannel(), cd.getDataRate(), new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF})) {
                    resultList.add(cd);
                }
            }
        }
        mDriver.disconnect();

        if (resultList.size() > 0) {
            return resultList.get(0);
        }
        return null;
    }

    public boolean resetToBootloader(int targetId) {
        int retryCounter = 5;

        sendBootloaderPacket(new byte[]{(byte) targetId, (byte) 0xFF});

        CrtpPacket replyPk = this.mDriver.receivePacket(1);

        //while ((not pk or pk.header != 0xFF or struct.unpack("<BB", pk.data[0:2]) != (target_id, 0xFF)) and retry_counter >= 0 ):
        while(!isBootloaderReplyPacket(replyPk, targetId, 0xFF) /*&& retryCounter >= 0*/) {
            replyPk = this.mDriver.receivePacket(1);
            //retryCounter -= 1;
        }

        if (replyPk != null) {
            //TODO: externalise flipping of array fields
            // new_address = (0xb1, ) + struct.unpack("<BBBB", pk.data[2:6][::-1])
            ByteBuffer bb = ByteBuffer.wrap(replyPk.getPayload()).order(ByteOrder.LITTLE_ENDIAN);
            byte[] newAddress = new byte[] {(byte) 0xb1, bb.get(5), bb.get(4), bb.get(3), bb.get(2)};

            sendBootloaderPacket(new byte[]{(byte) targetId, (byte) 0xF0, (byte) 0x00});

            //TODO: addr = int(struct.pack("B"*5, *new_address).encode('hex'), 16)

            // Thread sleep is important here, otherwise the last packet is not sent out
            // time.sleep(0.2)
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                mLogger.error("InterruptedException: " + e.getMessage());
            }
            this.mDriver.disconnect();
            //TODO: time.sleep(0.2)
            //TODO: self.link = cflib.crtp.get_link_driver("radio://0/0/2M/{}".format(addr))
            return true;
        }
        //TODO: fix dead code warning
        return false;
    }

    /**
     * Reset to the bootloader
     *
     * The parameter cpuid shall correspond to the device to reset.
     *
     * Return true if the reset has been done and the contact with the
     * bootloader is established.
     */
    //TODO: cpu_id = target id?? NO!
    //TODO: currently not used in python cflib
    public boolean resetToBootloader1(int cpuId) {
        /*
         * Send an echo request and wait for the answer
         * Mainly aim to bypass a bug of the crazyflie firmware that prevents
         * reset before normal CRTP communication
         */
        Header header = new Header(0, CrtpPort.LINKCTRL);
        CrtpPacket pk = new CrtpPacket(header.getByte(), new byte[]{1, 2, 3, (byte) cpuId});
        this.mDriver.sendPacket(pk);

        // Wait for reply
        CrtpPacket replyPk = null;
        while(true) {
            replyPk = this.mDriver.receivePacket(2);
            if (replyPk == null) {
                return false;
            }
            if (replyPk.getHeader().getPort() == CrtpPort.LINKCTRL) {
                break;
            }
        }

        // Send the reset to bootloader request
        //pk.data = (0xFF, 0xFE) + cpu_id
        sendBootloaderPacket(new byte[]{(byte) 0xFF, (byte) 0xFE, (byte) cpuId});

        //Wait to ack the reset ...
        CrtpPacket replyPk2 = null;
        while(true) {
            replyPk2 = this.mDriver.receivePacket(2);
            if (replyPk2 == null) {
                return false;
            }
            // if pk.port == 0xFF and tuple(pk.data) == (0xFF, 0xFE) + cpu_id:
            if(replyPk2.getHeader().getPort() == CrtpPort.ALL &&
                    replyPk2.getPayload()[0] == (byte) 0xFF &&
                    replyPk2.getPayload()[1] == (byte) 0xFE &&
                    replyPk2.getPayload()[2] == (byte) cpuId) {

                sendBootloaderPacket(new byte[]{(byte) 0xFF, (byte) 0xF0, (byte) cpuId});
                break;
            }
        }

        //time.sleep(0.1)
        this.mDriver.disconnect();
        //TODO: self.link = cflib.crtp.get_link_driver(self.clink_address)
        //time.sleep(0.1)

        return updateInfo(TargetTypes.STM32); //TODO: which targetId??
    }

    /**
     * Reset to firmware.
     *
     * @param targetId
     * @return true if the reset has been done
     */
    public boolean resetToFirmware(int targetId) {
        /*
         * The fake CPU ID is legacy from the Crazyflie 1.0
         * In order to reset the CPU ID had to be sent, but this
         * was removed before launching it. But the length check is
         * still in the bootloader. So to work around this bug so
         * some extra data needs to be sent.
         * fake_cpu_id = (1, 2, 4, 5, 6, 7, 8, 9, 10, 11, 12)
         */

        // Send the reset to bootloader request
        //pk.data = (target_id, 0xFF) + fake_cpu_id
        sendBootloaderPacket(new byte[]{(byte) targetId, (byte) 0xFF, 1, 2, 4, 5, 6, 7, 8, 9, 10, 11, 12});

        // Wait to ack the reset ...
        CrtpPacket replyPk = null;
        while(true) {
            replyPk = this.mDriver.receivePacket(2);

            if (!isBootloaderReplyPacket(replyPk, targetId, 0xFF)) {
                // Difference in CF1 and CF2 (CPU ID)
                byte[] data = null;
                if (targetId == TargetTypes.NRF51) { // CF2
                    data = new byte[] {(byte) targetId, (byte) 0xF0, (byte) 0x01};
                } else { // CF1
                    // pk.data = (target_id, 0xF0) + fake_cpu_id
                    data = new byte[] {(byte) targetId, (byte) 0xF0, 1, 2, 4, 5, 6, 7, 8, 9, 10, 11, 12};
                }
                sendBootloaderPacket(data);
                break;
            }
        }
        //time.sleep(0.1)
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            mLogger.error("InterruptedException: " + e.getMessage());
        }
        return true;
    }

    public void openBootloaderConnection(ConnectionData connectionData) throws IOException {
        if (this.mDriver != null) {
            this.mDriver.disconnect();
        }
        if (connectionData != null) {
            this.mDriver.connect(connectionData);
        } else {
            // self.link = cflib.crtp.get_link_driver(self.clink_address)
        }
    }

    /**
     * Try to get a connection with the bootloader by requesting info
     * 5 times. This let roughly 10 seconds to boot the copter ...
     */
    //def check_link_and_get_info(self, target_id=0xFF):
    public boolean checkLinkAndGetInfo(int targetId) {
        for (int i = 0; i < 5; i++) {
            if (updateInfo(targetId)) {

                /*
                if self._in_boot_cb:
                    self._in_boot_cb.call(True, self.targets[target_id].protocol_version)
                if self._info_cb:
                    self._info_cb.call(self.targets[target_id])
                */
                if (this.mProtocolVersion != BootVersion.CF1_PROTO_VER_1) {
                    return true;
                }

                // Set radio link to a random address
                /*
                addr = [0xbc] + map(lambda x: random.randint(0, 255), range(4))
                return self._set_address(addr)
                */
                byte[] newAddress = new byte[5];
                newAddress[0] = (byte) 0xbc;
                for (int n = 1; n < 5; n++) {
                    newAddress[n] = (byte) ((Math.random() * 1000) % 255);  //TODO: test
                }
                //return setAddress(newAddress);
                return true;
            }

            //TODO: is this necessary?
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                mLogger.error("InterruptedException: " + e.getMessage());
            }
        }
        return false;
    }

    /**
     * Change copter radio address.
     * This function works only with crazyradio CRTP link.
     */
    public boolean setAddress(byte[] newAddress) {
        if (newAddress.length != 5) {
            mLogger.error("Radio address should be 5 bytes long");
            return false;
        }

        mLogger.debug("Setting bootloader radio address to " + Utilities.getHexString(newAddress));

        // self.link.pause()
        this.mDriver.stopSendReceiveThread();

        Crazyradio crazyRadio = ((RadioDriver) this.mDriver).getRadio();
        //TODO: is there a more elegant way to do this?
        //pkdata = (0xFF, 0xFF, 0x11) + tuple(new_address)
        byte[] pkData = new byte[newAddress.length + 3];
        pkData[0] = (byte) 0xFF;
        pkData[1] = (byte) 0xFF;
        pkData[2] = (byte) SET_ADDRESS;

        for (int i = 0; i < 10; i++) {
            mLogger.debug("Trying to set new radio address");
            //self.link.cradio.set_address((0xE7,) * 5)
            crazyRadio.setAddress(new byte[]{(byte) 0xE7, (byte) 0xE7, (byte) 0xE7, (byte) 0xE7, (byte) 0xE7});

            System.arraycopy(newAddress, 0, pkData, 3, newAddress.length);
            crazyRadio.sendPacket(pkData);

            //self.link.cradio.set_address(tuple(new_address))
            crazyRadio.setAddress(newAddress);

            //if self.link.cradio.send_packet((0xff,)).ack:
            RadioAck ack = crazyRadio.sendPacket(new byte[] {(byte) 0xFF});
            if (ack != null) {
                mLogger.info("Bootloader set to radio address " + Utilities.getHexString(newAddress));;
                this.mDriver.startSendReceiveThread();
                return true;
            }
        }
        //this.mDriver.restart();
        this.mDriver.startSendReceiveThread();
        return false;
    }

    public Target requestInfoUpdate(int targetId) {
        if (!this.mTargets.containsKey(targetId)) {
            updateInfo(targetId);
        }
        /*
        if self._info_cb:
            self._info_cb.call(self.targets[target_id])
        */
        return this.mTargets.get(targetId);
    }

    /**
     * Call the command getInfo and fill up the information received in the fields of the object
     */
    public boolean updateInfo(int targetId) {
        // Call getInfo ...
        // pk.data = (target_id, 0x10)
        mLogger.info("Send update info packet");
        sendBootloaderPacket(new byte[]{(byte) targetId, (byte) GET_INFO});

        // Wait for the answer
        //TODO: retryCount?
        CrtpPacket replyPk = this.mDriver.receivePacket(2000);
        while(!isBootloaderReplyPacket(replyPk, targetId, GET_INFO)) {
            replyPk = this.mDriver.receivePacket(2000);
        }

        if(replyPk != null) {
            Target target = new Target(targetId);
            target.parseData(replyPk.getPayload());
            // set protocol version only for STM32
            if (targetId == TargetTypes.STM32) {
                this.mProtocolVersion = target.getProtocolVersion();
            }
            this.mTargets.put(targetId, target);

            // Update mapping (CF 2.0 only)
            if (target.getProtocolVersion() == (byte) BootVersion.CF2_PROTO_VER && targetId == TargetTypes.STM32) {
//                updateMapping(targetId);
            }
            return true;
        } else {
            mLogger.error("Payload problem");
        }
        return false;
    }

    public Integer[] updateMapping(int targetId) {
        sendBootloaderPacket(new byte[]{(byte) targetId, (byte) GET_MAPPING});

        CrtpPacket replyPk = this.mDriver.receivePacket(2);
        while (!isBootloaderReplyPacket(replyPk, targetId, GET_MAPPING)){
            replyPk = this.mDriver.receivePacket(2);
        }

        //m = pk.datat[2:]
        byte[] mappingData = Utilities.strip(replyPk.getPayload(), 2);

        if (mappingData.length % 2 != 0){
            //raise Exception("Malformed flash mapping packet")
            mLogger.error("Malformed flash mapping packet: length is not even (" + mappingData.length + ")");
            //TODO: why is the length not even?
            //return new Integer[0];
        }

        /*
        self.mapping = []
        page = 0
        for i in range(len(m)/2):
            for j in range(m[2*i]):
                self.mapping.append(page)
                page += m[(2*i)+1]
        */
        List<Integer> mapping = new ArrayList<Integer>();
        int page = 0;
        for (int i = 0; i < mappingData.length/2; i++) {
            for (int j = 0; j < mappingData[2*i]; j++) {
                mapping.add(page);
                page += mappingData[(2*i)+1] & 0xFF; // "& 0xFF" deals with unsigned byte
            }
        }
        return (Integer[]) mapping.toArray(new Integer[mapping.size()]);
    }

    /**
     * Upload data into a buffer on the Crazyflie
     */
    public void uploadBuffer(int targetId, int page, int address, byte[] buff) {
        int count = 0;
        //pk.data = struct.pack("=BBHH", target_id, 0x14, page, address)
        ByteBuffer bb = ByteBuffer.allocate(31).order(ByteOrder.LITTLE_ENDIAN);
        bb.put((byte) targetId);
        bb.put((byte) LOAD_BUFFER);
        bb.putChar((char) page);
        bb.putChar((char) address);

        for (int i = 0; i < buff.length; i++) {
            bb.put(buff[i]);

            count++;

            if (isCancelled()) {
                break;
            }

            if (count > 24) {
                sendBootloaderPacket(bb.array());
                count = 0;

                //pk.data = struct.pack("=BBHH", target_id, 0x14, page, i + address + 1)
                //TODO: bb.clear() did not work as intended
                bb = ByteBuffer.allocate(31).order(ByteOrder.LITTLE_ENDIAN);
                bb.put((byte) targetId);
                bb.put((byte) LOAD_BUFFER);
                bb.putChar((char) page);
                bb.putChar((char) (i + address + 1));
            }
        }
        //mLogger.debug("Sending buffer packet: " + Utilities.getHexString(bb.array()));
        sendBootloaderPacket(bb.array());
    }

    /**
     * Read back a flash page from the Crazyflie and return it
     */
    //def read_flash(self, addr=0xFF, page=0x00):
    public byte[] readFlash(int addr, int page) {
        ByteBuffer buff = null;

        Target target = this.mTargets.get(addr);
        if (target != null) {
            int pageSize = target.getPageSize();
            buff = ByteBuffer.allocate(pageSize + 1);

            for (int i = 0; i < Math.ceil(pageSize / 25.0); i++) {
                CrtpPacket replyPk = null;
                int retryCounter = 5;

                while (retryCounter >= 0) {
                    ByteBuffer bb = ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN);
                    bb.put((byte) addr);
                    bb.put((byte) READ_FLASH);
                    bb.putChar((char) page);
                    bb.putChar((char) (i*25));

                    sendBootloaderPacket(bb.array());

                    //System.out.println("ByteString send: " + getHexString(pk.getPayload()) + " " + UsbLinkJava.getByteString(pk.getPayload()));

                    //TODO: why is this different than in Python?
                    //does it have something to do with the queue size??
                    //yes, the queue is filled with empty packets
                    //how can this be avoided?
                    while(!isBootloaderReplyPacket(replyPk, addr, READ_FLASH)) {
                        replyPk = this.mDriver.receivePacket(10);
                    }
                    if (replyPk != null) {
                        break;
                    }
                    retryCounter--;
                }
                if (retryCounter < 0) {
                    System.out.println("Returning null...");
                    return null;
                } else {
                    buff.put(replyPk.getPayload(), 6, replyPk.getPayload().length - 6);
                }
            }

        }
        //return buff[0:page_size]  # For some reason we get one byte extra here...
        //-> because of the ceil function?
        return buff.array();
    }

    /**
     * Initiate flashing of data in the buffer to flash.
     */
    public boolean writeFlash(int addr, int pageBuffer, int targetPage, int pageCount) {
        /*
        #print "Write page", flashPage
        #print "Writing page [%d] and [%d] forward" % (flashPage, nPage)
        */
        CrtpPacket replyPk = null;
        int retryCounter = 5;
        //#print "Flashing to 0x{:X}".format(addr)

        //pk.data = struct.pack("<BBHHH", addr, 0x18, page_buffer, target_page, page_count)
        ByteBuffer bb = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        bb.put((byte) addr);
        bb.put((byte) WRITE_FLASH);
        bb.putChar((char) pageBuffer);
        bb.putChar((char) targetPage);
        bb.putChar((char) pageCount);
        sendBootloaderPacket(bb.array());

        while(!isBootloaderReplyPacket(replyPk, addr, WRITE_FLASH) && retryCounter >= 0 && !isCancelled()) {
            replyPk = this.mDriver.receivePacket(1);
            //TODO: why does it not work, when the retryCounter is activated?
//            retryCounter--;
        }

        if (retryCounter < 0) {
            mLogger.debug("RetryCounter is < 0!");
            //self.error_code = -1
            return false;
        }

        // handle error code
        int errorCode = replyPk.getPayload()[3];
        switch (errorCode) {
        case 1:
            mErrorMessage = "Addresses are outside of authorized boundaries";
            break;
        case 2:
            mErrorMessage = "Flash erase failed";
            break;
        case 3:
            mErrorMessage = "Flash programming failed";
            break;
        default:
            mErrorMessage = "";
            break;
        }
        if (errorCode != 0) {
          //TODO: also call listener
          mLogger.error(mErrorMessage + " (error code: " + errorCode + ")");
        }

        return replyPk.getPayload()[2] == 1;
    }

    //decode_cpu_id has not been implemented, because it's not used anywhere

    private boolean isCancelled() {
        return this.mCancelled;
    }

    public void cancel() {
        this.mCancelled = true;
    }

    public String getErrorMessage() {
        return this.mErrorMessage;
    }

    public int getProtocolVersion() {
        return this.mProtocolVersion;
    }

    public void sendBootloaderPacket(byte[] data) {
        Header header = new Header((byte) 0xFF);
        CrtpPacket pk = new CrtpPacket(header.getByte(), data);
        this.mDriver.sendPacket(pk);
    }

    public boolean isBootloaderReplyPacket(CrtpPacket paket, int firstByte, int secondByte) {
        if (paket == null) {
            return false;
        }
        return paket.getHeaderByte() == (byte) 0xFF && paket.getPayload()[0] == (byte) firstByte && paket.getPayload()[1] == (byte) secondByte;
    }

    public List<Target> getTargetsAsList() {
        List<Target> targets = new ArrayList<Target>();
        for (Target t : mTargets.values()) {
            targets.add(t);
        }
        return targets;
    }

    public Map<Integer, Target> getTargets() {
        return this.mTargets;
    }

}