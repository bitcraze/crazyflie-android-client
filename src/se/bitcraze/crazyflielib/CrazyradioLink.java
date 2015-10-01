/**
 *    ||          ____  _ __
 * +------+      / __ )(_) /_______________ _____  ___
 * | 0xBC |     / __  / / __/ ___/ ___/ __ `/_  / / _ \
 * +------+    / /_/ / / /_/ /__/ /  / /_/ / / /_/  __/
 *  ||  ||    /_____/_/\__/\___/_/   \__,_/ /___/\___/
 *
 * Copyright (C) 2013 Bitcraze AB
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

package se.bitcraze.crazyflielib;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.bitcraze.crazyflielib.crazyradio.ConnectionData;
import se.bitcraze.crazyflielib.crtp.CrtpPacket;

public class CrazyradioLink extends AbstractLink {

    final Logger mLogger = LoggerFactory.getLogger("CrazyradioLink");

    /**
     * Vendor ID of the Crazyradio USB dongle.
     */
    public static final int VENDOR_ID = 0x1915;

    /**
     * Product ID of the Crazyradio USB dongle.
     */
    public static final int PRODUCT_ID = 0x7777;

    /**
     * Number of packets without acknowledgment before marking the connection as
     * broken and disconnecting.
     */
    public static final int RETRYCOUNT_BEFORE_DISCONNECT = 10;

    /**
     * This number of packets should be processed between reports of the link quality.
     */
    public static final int PACKETS_BETWEEN_LINK_QUALITY_UPDATE = 5;

    // Dongle configuration requests
    // See http://wiki.bitcraze.se/projects:crazyradio:protocol for documentation
    private static final int REQUEST_SET_RADIO_CHANNEL = 0x01;
    private static final int REQUEST_SET_RADIO_ADDRESS = 0x02;
    private static final int REQUEST_SET_DATA_RATE = 0x03;
    private static final int REQUEST_SET_RADIO_POWER = 0x04;
    private static final int REQUEST_SET_RADIO_ARD = 0x05;
    private static final int REQUEST_SET_RADIO_ARC = 0x06;
    private static final int REQUEST_ACK_ENABLE = 0x10;
    private static final int REQUEST_SET_CONT_CARRIER = 0x20;
    private static final int REQUEST_START_SCAN_CHANNELS = 0x21;
    private static final int REQUEST_GET_SCAN_CHANNELS = 0x21;
    private static final int REQUEST_LAUNCH_BOOTLOADER = 0xFF;

    private Thread mRadioLinkThread;

    private final BlockingDeque<CrtpPacket> mSendQueue;

    private IUsbLink mUsbLink;

    /**
     * Create a new link using the Crazyradio.
     *
     * @param usbLink
     */
    public CrazyradioLink(IUsbLink usbLink) {
        this.mUsbLink = usbLink;
        this.mSendQueue = new LinkedBlockingDeque<CrtpPacket>();
    }

    /**
     * Scan for available channels.
     *
     * @return array containing the found channels and datarates.
     * @throws IOException
     * @throws IllegalStateException if the Crazyradio is not attached (the connection is <code>null</code>).
     */

    public ConnectionData[] scanChannels() throws IOException {
        return scanChannels(false);
    }

    /**
     * Scan for available channels.
     *
     * @param useSlowScan
     * @return array containing the found channels and datarates.
     * @throws IOException
     * @throws IllegalStateException if the Crazyradio is not attached (the connection is <code>null</code>).
     */
    public ConnectionData[] scanChannels(boolean useSlowScan) throws IOException {
        List<ConnectionData> result = new ArrayList<ConnectionData>();
        if (mUsbLink.isUsbConnected()) {
            // null packet
            final byte[] packet = CrtpPacket.NULL_PACKET.toByteArray();
            final byte[] rdata = new byte[64];

            mLogger.debug("Scanning...");
            // scan for all 3 data rates
            for (int datarate = 0; datarate < 3; datarate++) {
                // set data rate
                mUsbLink.sendControlTransfer(0x40, REQUEST_SET_DATA_RATE, datarate, 0, null);
                if (useSlowScan) {
                    result.addAll(scanChannelsSlow(datarate));
                } else {
                    mLogger.debug("Fast firmware scan...");
                    //long transfer timeout (1000) is important!
                    mUsbLink.sendControlTransfer(0x40, REQUEST_START_SCAN_CHANNELS, 0, 125, packet);
                    final int nfound = mUsbLink.sendControlTransfer(0xc0, REQUEST_GET_SCAN_CHANNELS, 0, 0, rdata);
                    for (int i = 0; i < nfound; i++) {
                        result.add(new ConnectionData(rdata[i], datarate));
                        mLogger.debug("Found channel: " + rdata[i] + " Data rate: " + datarate);
                    }
                }
            }
        } else {
            mLogger.debug("connection is null");
            throw new IllegalStateException("Crazyradio not attached");
        }
        return result.toArray(new ConnectionData[result.size()]);
    }

    /**
     * Slow manual scan
     *
     * @param datarate
     * @throws IOException
     */
    private List<ConnectionData> scanChannelsSlow(int datarate) throws IOException {
        mLogger.debug("Slow manual scan...");
        List<ConnectionData> result = new ArrayList<ConnectionData>();

        for (int channel = 0; channel < 126; channel++) {
            // set channel
            mUsbLink.sendControlTransfer(0x40, REQUEST_SET_RADIO_CHANNEL, channel, 0, null);

            byte[] receiveData = new byte[33];
            final byte[] sendData = CrtpPacket.NULL_PACKET.toByteArray();

            mUsbLink.sendBulkTransfer(sendData, receiveData);
            if ((receiveData[0] & 1) != 0) { // check if ack received
                result.add(new ConnectionData(channel, datarate));
                mLogger.debug("Channel found: " + channel + " Data rate: " + datarate);
            }
            try {
                Thread.sleep(20, 0);
            } catch (InterruptedException e) {
                mLogger.error("scanChannelsSlow InterruptedException");
            }
        }
        return result;
    }

    /**
     * Connect to the Crazyflie.
     *
     * @throws IllegalStateException if the Crazyradio is not attached
     */
    @Override
    public void connect(ConnectionData connectionData) throws IllegalStateException {
        setRadioChannel(connectionData.getChannel());
        setDataRate(connectionData.getDataRate());

        mLogger.debug("connect()");
        notifyConnectionRequested();

        if (mUsbLink != null && mUsbLink.isUsbConnected()) {
            if (mRadioLinkThread == null) {
                mRadioLinkThread = new Thread(radioControlRunnable);
                mRadioLinkThread.start();
            }
        } else {
            mLogger.debug("mConnection is null");
            notifyConnectionFailed("Crazyradio not attached");
            throw new IllegalStateException("Crazyradio not attached");
        }
    }

    @Override
    public void disconnect() {
        mLogger.debug("disconnect()");
        if (mRadioLinkThread != null) {
            mRadioLinkThread.interrupt();
            mRadioLinkThread = null;
        }

        notifyDisconnected();
    }

    @Override
    public boolean isConnected() {
        return mRadioLinkThread != null;
    }

    @Override
    public void sendPacket(CrtpPacket p) {
        this.mSendQueue.addLast(p);
    }

    /**
     * Set the radio channel.
     *
     * @param channel the new channel. Must be in range 0-125.
     */
    public void setRadioChannel(int channel) {
        mUsbLink.sendControlTransfer(0x40, REQUEST_SET_RADIO_CHANNEL, channel, 0, null);
    }

    /**
     * Set the data rate.
     *
     * @param rate new data rate. Possible values are in range 0-2.
     */
    public void setDataRate(int rate) {
        mUsbLink.sendControlTransfer(0x40, REQUEST_SET_DATA_RATE, rate, 0, null);
    }

    /**
     * Set the radio address. The same address must be configured in the
     * receiver for the communication to work.
     *
     * @param address the new address with a length of 5 byte.
     * @throws IllegalArgumentException if the length of the address doesn't equal 5 bytes
     */
    public void setRadioAddress(byte[] address) {
        if (address.length != 5) {
            throw new IllegalArgumentException("radio address must be 5 bytes long");
        }
        mUsbLink.sendControlTransfer(0x40, REQUEST_SET_RADIO_ADDRESS, 0, 0, address);
    }

    /**
     * Set the continuous carrier mode. When enabled the radio chip provides a
     * test mode in which a continuous non-modulated sine wave is emitted. When
     * this mode is activated the radio dongle does not transmit any packets.
     *
     * @param continuous <code>true</code> to enable the continuous carrier mode
     */
    public void setContinuousCarrier(boolean continuous) {
        mUsbLink.sendControlTransfer(0x40, REQUEST_SET_CONT_CARRIER, (continuous ? 1 : 0), 0, null);
    }

    /**
     * Configure the time the radio waits for the acknowledge.
     *
     * @param us microseconds to wait. Will be rounded to the closest possible value supported by the radio.
     */
    public void setAutoRetryADRTime(int us) {
        int param = (int) Math.round(us / 250.0) - 1;
        if (param < 0) {
            param = 0;
        } else if (param > 0xF) {
            param = 0xF;
        }
        mUsbLink.sendControlTransfer(0x40, REQUEST_SET_RADIO_ARD, param, 0, null);
    }

    /**
     * Set the length of the ACK payload.
     *
     * @param bytes number of bytes in the payload.
     * @throws IllegalArgumentException if the payload length is not in range 0-32.
     */
    public void setAutoRetryADRBytes(int bytes) {
        if (bytes < 0 || bytes > 32) {
            throw new IllegalArgumentException("payload length must be in range 0-32");
        }
        mUsbLink.sendControlTransfer(0x40, REQUEST_SET_RADIO_ARD, 0x80 | bytes, 0, null);
    }

    /**
     * Set how often the radio will retry a transfer if the ACK has not been
     * received.
     *
     * @param count the number of retries.
     * @throws IllegalArgumentException if the number of retries is not in range 0-15.
     */
    public void setAutoRetryARC(int count) {
        if (count < 0 || count > 15) {
            throw new IllegalArgumentException("count must be in range 0-15");
        }
        mUsbLink.sendControlTransfer(0x40, REQUEST_SET_RADIO_ARC, count, 0, null);
    }

    /**
     * Handles communication with the dongle to send and receive packets
     */
    private final Runnable radioControlRunnable = new Runnable() {
        @Override
        public void run() {
            int retryBeforeDisconnectRemaining = RETRYCOUNT_BEFORE_DISCONNECT;
            int nextLinkQualityUpdate = PACKETS_BETWEEN_LINK_QUALITY_UPDATE;

            notifyConnected();

            while (mUsbLink != null && mUsbLink.isUsbConnected()) {
                try {
                    CrtpPacket p = mSendQueue.pollFirst(5, TimeUnit.MILLISECONDS);
                    if (p == null) { // if no packet was available in the send queue
                        p = CrtpPacket.NULL_PACKET;
                    }

                    byte[] receiveData = new byte[33];
                    final byte[] sendData = p.toByteArray();

                    final int receivedByteCount = mUsbLink.sendBulkTransfer(sendData, receiveData);

                    //TODO: extract link quality calculation
                    if (receivedByteCount >= 1) {
                        // update link quality status
                        if (nextLinkQualityUpdate <= 0) {
                            final int retransmission = receiveData[0] >> 4;
                            notifyLinkQualityUpdated(Math.max(0, (10 - retransmission) * 10));
                            nextLinkQualityUpdate = PACKETS_BETWEEN_LINK_QUALITY_UPDATE;
                        } else {
                            nextLinkQualityUpdate--;
                        }

                        if ((receiveData[0] & 1) != 0) { // check if ack received
                            retryBeforeDisconnectRemaining = RETRYCOUNT_BEFORE_DISCONNECT;
                            if (receivedByteCount > 1) {
                                CrtpPacket inPacket = new CrtpPacket(Arrays.copyOfRange(receiveData, 1, 1 + (receivedByteCount - 1)));
                                notifyDataListeners(inPacket);
                            }
                        } else {
                            // count lost packets
                            retryBeforeDisconnectRemaining--;
                            if (retryBeforeDisconnectRemaining <= 0) {
                                notifyConnectionLost("Too many packets lost");
                                disconnect();
                                break;
                            }
                        }
                    } else {
                        mLogger.debug("CrazyradioLink comm error - didn't receive answer");
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    };

}
