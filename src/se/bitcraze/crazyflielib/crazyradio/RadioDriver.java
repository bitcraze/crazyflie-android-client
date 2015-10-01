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

package se.bitcraze.crazyflielib.crazyradio;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.bitcraze.crazyflielib.AbstractLink;
import se.bitcraze.crazyflielib.CrazyradioLink;
import se.bitcraze.crazyflielib.IUsbLink;
import se.bitcraze.crazyflielib.crtp.CrtpPacket;

/**
 * Crazyradio link driver
 *
 */
public class RadioDriver extends AbstractLink{

    final static Logger mLogger = LoggerFactory.getLogger("RadioDriver");

    private CrazyradioLink mCradio;
    private Thread mRadioDriverThread;

    private IUsbLink mUsbInterface;

    private final BlockingDeque<CrtpPacket> mInQueue;
    private final BlockingDeque<CrtpPacket> mOutQueue;

    /**
     * Create the link driver
     */
    public RadioDriver(IUsbLink usbInterface) {
        this.mUsbInterface = usbInterface;
        this.mCradio = null;
        this.mInQueue = new LinkedBlockingDeque<CrtpPacket>();
        this.mOutQueue = new LinkedBlockingDeque<CrtpPacket>(); //TODO: Limit size of out queue to avoid "ReadBack" effect?
        this.mRadioDriverThread = null;
    }

    /* (non-Javadoc)
     * @see se.bitcraze.crazyflie.lib.crtp.CrtpDriver#connect(se.bitcraze.crazyflie.lib.crazyradio.ConnectionData)
     */
    public void connect(ConnectionData connectionData) {
        if(mCradio == null) {
            this.mCradio = new CrazyradioLink(mUsbInterface);
        } else {
            mLogger.error("Crazyradio already open");
        }

        /*
        if self.cradio is None:
            self.cradio = Crazyradio(devid=int(uri_data.group(1)))
        else:
            raise Exception("Link already open!")
        */

        if (this.mCradio.getVersion() >= 0.4) {
            this.mCradio.setAutoRetryARC(10);
        } else {
            mLogger.warn("Radio version <0.4 will be obsolete soon!");
        }

        this.mCradio.setRadioChannel(connectionData.getChannel());
        this.mCradio.setDataRate(connectionData.getDataRate());

        /*
        if uri_data.group(9):
            addr = "{:X}".format(int(uri_data.group(9)))
            new_addr = struct.unpack("<BBBBB", binascii.unhexlify(addr))
            self.cradio.set_address(new_addr)
         */

        // Launch the comm thread
        if (mRadioDriverThread == null) {
            //self._thread = _RadioDriverThread(self.cradio, self.in_queue, self.out_queue, link_quality_callback, link_error_callback)
            RadioDriverThread rDT = new RadioDriverThread();
            mRadioDriverThread = new Thread(rDT);
            mRadioDriverThread.start();
        }
    }

    /*
     *  Receive a packet though the link. This call is blocking but will
     *  timeout and return None if a timeout is supplied.
     */
    @Override
    public CrtpPacket receivePacket(int time) {
        try {
            return mInQueue.pollFirst((long) time, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            //TODO: does this needs to be dealt with?
            //e.printStackTrace();
            return null;
        }
    }

    //TODO: Remove
    public int getInQueueSize() {
        return mInQueue.size();
    }

    /*
     * Send the packet though the link
     *
     *  (non-Javadoc)
     * @see cflib.crtp.CrtpDriver#sendPacket(cflib.crtp.CrtpPacket)
     */
    @Override
    public void sendPacket(CrtpPacket packet) {
        if (this.mCradio == null) {
            return;
        }

        //TODO: does it make sense to be able to queue packets even though
        //the connection is not established yet?

        /*
        try:
            self.out_queue.put(pk, True, 2)
        except Queue.Full:
            if self.link_error_callback:
                self.link_error_callback("RadioDriver: Could not send packet to copter")
        */

        // this.mOutQueue.addLast(packet);
        try {
            this.mOutQueue.put(packet);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /*
     *  Close the link.
     *
     * (non-Javadoc)
     * @see cflib.crtp.CrtpDriver#close()
     */
    @Override
    public void disconnect() {
        mLogger.debug("disconnect");
        // Stop the comm thread
        if (this.mRadioDriverThread != null) {
            this.mRadioDriverThread.interrupt();
            this.mRadioDriverThread = null;
        }
        if(this.mCradio != null) {
            this.mCradio.disconnect();
            this.mCradio = null;
        }
        if(this.mUsbInterface != null) {
            this.mUsbInterface.releaseInterface();
//            this.mUsbInterface = null;
        }
    }

    public List<ConnectionData> scanInterface() {
        return scanInterface(mCradio, mUsbInterface);
    }

    /**
     * Scan interface for Crazyflies
     */
    public static List<ConnectionData> scanInterface(CrazyradioLink crazyRadio, IUsbLink crazyUsbInterface) {
        List<ConnectionData> connectionDataList = new ArrayList<ConnectionData>();

        if(crazyRadio == null) {
            crazyRadio = new CrazyradioLink(crazyUsbInterface);
        } else {
            mLogger.error("Cannot scan for links while the link is open!");
            //TODO: throw exception?
        }

        mLogger.info("Found Crazyradio with version " + crazyRadio.getVersion() + " and serial number " + crazyRadio.getSerialNumber());

        crazyRadio.setAutoRetryARC(1);

//        crazyRadio.setDataRate(CrazyradioLink.DR_250KPS);
//        List<Integer> scanRadioChannels250k = crazyRadio.scanChannels();
//        for(Integer channel : scanRadioChannels250k) {
//            connectionDataList.add(new ConnectionData(channel, CrazyradioLink.DR_250KPS));
//        }
//        crazyRadio.setDataRate(CrazyradioLink.DR_1MPS);
//        List<Integer> scanRadioChannels1m = crazyRadio.scanChannels();
//        for(Integer channel : scanRadioChannels1m) {
//            connectionDataList.add(new ConnectionData(channel, CrazyradioLink.DR_1MPS));
//        }
//        crazyRadio.setDataRate(CrazyradioLink.DR_2MPS);
//        List<Integer> scanRadioChannels2m = crazyRadio.scanChannels();
//        for(Integer channel : scanRadioChannels2m) {
//            connectionDataList.add(new ConnectionData(channel, CrazyradioLink.DR_2MPS));
//        }

        try {
            connectionDataList = Arrays.asList(crazyRadio.scanChannels());
        } catch (IOException e) {
            e.printStackTrace();
        }

//        crazyRadio.close();
//        crazyRadio = null;

        return connectionDataList;
    }

    public boolean scanSelected(int channel, int datarate, byte[] packet) {
        if (mCradio == null) {
            mCradio = new CrazyradioLink(mUsbInterface);
        }
        return mCradio.scanSelected(channel, datarate, packet);
    }

    public CrazyradioLink getRadio() {
        return this.mCradio;
    }

    /**
     * Radio link receiver thread is used to read data from the Crazyradio USB driver.
     */
    public class RadioDriverThread implements Runnable {

        final Logger mLogger = LoggerFactory.getLogger(this.getClass().getSimpleName());

        private final static int RETRYCOUNT_BEFORE_DISCONNECT = 10;
        private int mRetryBeforeDisconnect;

        /**
         * Create the object
         */
        public RadioDriverThread() {
            this.mRetryBeforeDisconnect = RETRYCOUNT_BEFORE_DISCONNECT;
        }

        /**
         * Run the receiver thread
         *
         * (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        public void run() {
            byte[] dataOut = new byte[] {(byte) 0xFF}; //Null packet

            double waitTime = 0;
            int emptyCtr = 0;

            while(mCradio != null) {
                try {

                    /*
                    try:
                        ackStatus = self.cradio.send_packet(dataOut)
                    except Exception as e:
                        import traceback
                        self.link_error_callback("Error communicating with crazy radio"
                                                 " ,it has probably been unplugged!\n"
                                                 "Exception:%s\n\n%s" % (e,
                                                 traceback.format_exc()))
                    */
                    RadioAck ackStatus = mCradio.sendPacket(dataOut);

                    // Analyze the data packet
                    if (ackStatus == null) {
                        notifyLinkError("Dongle communication error (ackStatus == null)");
                        mLogger.warn("Dongle communication error (ackStatus == null)");
                        continue;
                    }

                    notifyLinkQualityUpdated((10 - ackStatus.getRetry()) * 10);

                    // If no copter, retry
                    //TODO: how is this actually possible?
                    if (!ackStatus.isAck()) {
                        this.mRetryBeforeDisconnect--;
                        if (this.mRetryBeforeDisconnect == 0) {
                            notifyLinkError("Too many packets lost");
                            mLogger.warn("Too many packets lost");
                        }
                        continue;
                    }
                    this.mRetryBeforeDisconnect = RETRYCOUNT_BEFORE_DISCONNECT;

                    byte[] data = ackStatus.getData();

                    // if there is a copter in range, the packet is analyzed and the next packet to send is prepared
                    if (data != null && data.length > 0) {
                        CrtpPacket inPacket = new CrtpPacket(data);
                        mInQueue.put(inPacket);

                        waitTime = 0;
                        emptyCtr = 0;
                    } else {
                        emptyCtr += 1;
                        if (emptyCtr > 10) {
                            emptyCtr = 10;
                            // Relaxation time if the last 10 packet where empty
                            waitTime = 0.01;
                        } else {
                            waitTime = 0;
                        }
                    }

                    // get the next packet to send of relaxation (wait 10ms)
                    CrtpPacket outPacket = null;
                    outPacket = mOutQueue.pollFirst((long) waitTime, TimeUnit.MILLISECONDS);

                    if (outPacket != null) {
                        dataOut = outPacket.toByteArray();
                    } else {
                        dataOut = new byte[]{(byte) 0xFF};
                    }
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    mLogger.debug("RadioDriverThread was interrupted.");
                    break;
                }
            }

        }
    }

    @Override
    public boolean isConnected() {
        // TODO Auto-generated method stub
        return false;
    }

}
