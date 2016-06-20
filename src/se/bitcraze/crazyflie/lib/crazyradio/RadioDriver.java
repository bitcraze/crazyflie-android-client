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

package se.bitcraze.crazyflie.lib.crazyradio;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.bitcraze.crazyflie.lib.crtp.CrtpDriver;
import se.bitcraze.crazyflie.lib.crtp.CrtpPacket;
import se.bitcraze.crazyflie.lib.usb.CrazyUsbInterface;

/**
 * Crazyradio link driver
 *
 */
public class RadioDriver extends CrtpDriver {

    final static Logger mLogger = LoggerFactory.getLogger("RadioDriver");

    private Crazyradio mCradio;
    private Thread mRadioDriverThread;

    private CrazyUsbInterface mUsbInterface;

    private final BlockingQueue<CrtpPacket> mInQueue;
    private final BlockingQueue<CrtpPacket> mOutQueue;

    /**
     * Create the link driver
     */
    public RadioDriver(CrazyUsbInterface usbInterface) {
        this.mUsbInterface = usbInterface;
        this.mCradio = null;
        this.mInQueue = new LinkedBlockingQueue<CrtpPacket>();
        this.mOutQueue = new LinkedBlockingQueue<CrtpPacket>(); //TODO: Limit size of out queue to avoid "ReadBack" effect?
        this.mRadioDriverThread = null;
    }

    /* (non-Javadoc)
     * @see se.bitcraze.crazyflie.lib.crtp.CrtpDriver#connect(se.bitcraze.crazyflie.lib.crazyradio.ConnectionData)
     */
    public void connect(ConnectionData connectionData) {
        this.mConnectionData = connectionData;
        if(mCradio == null) {
            notifyConnectionRequested();
//            try {
//                mUsbInterface.initDevice(Crazyradio.CRADIO_VID, Crazyradio.CRADIO_PID);
//            } catch (IOException e) {
//                throw new IOException("Make sure that the Crazyradio (PA) is connected.");
//            }
            this.mCradio = new Crazyradio(mUsbInterface);
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
            this.mCradio.setArc(10);
        } else {
            mLogger.warn("Radio version <0.4 will be obsolete soon!");
        }

        this.mCradio.setChannel(connectionData.getChannel());
        this.mCradio.setDatarate(connectionData.getDataRate());

        /*
        if uri_data.group(9):
            addr = "{:X}".format(int(uri_data.group(9)))
            new_addr = struct.unpack("<BBBBB", binascii.unhexlify(addr))
            self.cradio.set_address(new_addr)
         */

        // Launch the comm thread
        startSendReceiveThread();
    }

    /*
     *  Receive a packet though the link. This call is blocking but will
     *  timeout and return None if a timeout is supplied.
     */
    @Override
    public CrtpPacket receivePacket(int time) {
        try {
            return mInQueue.poll((long) time, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            mLogger.error("InterruptedException: " + e.getMessage());
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
            mLogger.error("InterruptedException: " + e.getMessage());
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
        mLogger.debug("disconnect()");
        // Stop the comm thread
        stopSendReceiveThread();
        // Avoid NPE because packets are still processed
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            mLogger.error("Interrupted during disconnect: " + e.getMessage());
        }
        if(this.mCradio != null) {
            this.mCradio.disconnect();
            this.mCradio = null;
        }
        notifyDisconnected();
    }

    public List<ConnectionData> scanInterface() {
        return scanInterface(mCradio, mUsbInterface);
    }

    /**
     * Scan interface for Crazyflies
     */
    public static List<ConnectionData> scanInterface(Crazyradio crazyRadio, CrazyUsbInterface crazyUsbInterface) {
        List<ConnectionData> connectionDataList = new ArrayList<ConnectionData>();

        if(crazyRadio == null) {
            crazyRadio = new Crazyradio(crazyUsbInterface);
        } else {
            mLogger.error("Cannot scan for links while the link is open!");
            //TODO: throw exception?
        }

        mLogger.info("Found Crazyradio with version " + crazyRadio.getVersion() + " and serial number " + crazyRadio.getSerialNumber());

        crazyRadio.setArc(1);

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
            mLogger.error(e.getMessage());
        }

//        crazyRadio.close();
//        crazyRadio = null;

        return connectionDataList;
    }

    public boolean scanSelected(int channel, int datarate, byte[] packet) {
        if (mCradio == null) {
            mCradio = new Crazyradio(mUsbInterface);
        }
        return mCradio.scanSelected(channel, datarate, packet);
    }

    public Crazyradio getRadio() {
        return this.mCradio;
    }


    public void startSendReceiveThread() {
        if (mRadioDriverThread == null) {
            //self._thread = _RadioDriverThread(self.cradio, self.in_queue, self.out_queue, link_quality_callback, link_error_callback)
            RadioDriverThread rDT = new RadioDriverThread();
            mRadioDriverThread = new Thread(rDT);
            mRadioDriverThread.start();
        }
    }

    public void stopSendReceiveThread() {
        if (this.mRadioDriverThread != null) {
            this.mRadioDriverThread.interrupt();
            this.mRadioDriverThread = null;
        }
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
            byte[] dataOut = Crazyradio.NULL_PACKET;

            double waitTime = 0;
            int emptyCtr = 0;

            while(mCradio != null && !Thread.currentThread().isInterrupted()) {
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
                        notifyConnectionLost("Dongle communication error (ackStatus == null)");
                        mLogger.warn("Dongle communication error (ackStatus == null)");
                        continue;
                    }

                    notifyLinkQualityUpdated((10 - ackStatus.getRetry()) * 10);

                    // If no copter, retry
                    //TODO: how is this actually possible?
                    if (!ackStatus.isAck()) {
                        this.mRetryBeforeDisconnect--;
                        if (this.mRetryBeforeDisconnect == 0) {
                            notifyConnectionLost("Too many packets lost");
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
                            // Relaxation time if the last 10 packets where empty
                            waitTime = 0.01;
                        } else {
                            waitTime = 0;
                        }
                    }

                    // get the next packet to send after relaxation (wait 10ms)
                    CrtpPacket outPacket = null;
                    outPacket = mOutQueue.poll((long) waitTime, TimeUnit.SECONDS);

                    if (outPacket != null) {
                        dataOut = outPacket.toByteArray();
                    } else {
                        dataOut = Crazyradio.NULL_PACKET;
                    }
//                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    mLogger.debug("RadioDriverThread was interrupted.");
                    notifyLinkQualityUpdated(0);
                    break;
                }
            }

        }
    }

    @Override
    public boolean isConnected() {
        return this.mRadioDriverThread != null;
    }

}
