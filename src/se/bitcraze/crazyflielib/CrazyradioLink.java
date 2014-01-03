
package se.bitcraze.crazyflielib;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import se.bitcraze.crazyflielib.crtp.CrtpPacket;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

public class CrazyradioLink extends AbstractLink {

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
     * This number of packets should be processed between reports of the link
     * quality.
     */
    public static final int PACKETS_BETWEEN_LINK_QUALITY_UPDATE = 5;

    // Dongle configuration requests
    // See http://wiki.bitcraze.se/projects:crazyradio:protocol for
    // documentation
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

    private static int TRANSFER_TIMEOUT = 100;

    private UsbManager mUsbManager;
    private static PendingIntent mPermissionIntent;

    private static final String LOG_TAG = "Crazyflie_CrazyradioLink";

    private final UsbDevice mUsbDevice;
    private UsbInterface mIntf;
    private UsbEndpoint mEpIn;
    private UsbEndpoint mEpOut;
    private UsbDeviceConnection mConnection;

    private Thread mRadioLinkThread;

    private final BlockingDeque<CrtpPacket> mSendQueue;

    /**
     * Holds information about a specific connection.
     */
    public static class ConnectionData {
        private final int channel;
        private final int dataRate;

        public ConnectionData(int channel, int dataRate) {
            this.channel = channel;
            this.dataRate = dataRate;
        }

        public int getChannel() {
            return channel;
        }

        public int getDataRate() {
            return dataRate;
        }
    }

    /**
     * Create a new link using the Crazyradio.
     * 
     * @param usbManager
     * @param usbDevice
     * @param connectionData connection data to initialize the link
     * @throws IllegalArgumentException if usbManager or usbDevice is <code>null</code>
     * @throws IOException if the device cannot be opened
     */
    public CrazyradioLink(Context context, ConnectionData connectionData) throws IOException {
        this.mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        this.mUsbDevice = searchForCrazyradio(context, mUsbManager);
        if (mUsbManager == null || mUsbDevice == null) {
            throw new IllegalArgumentException("USB manager and device must not be null");
        }
        initDevice();

        setRadioChannel(connectionData.getChannel());
        setDataRate(connectionData.getDataRate());

        this.mSendQueue = new LinkedBlockingDeque<CrtpPacket>();
    }

    /**
     * Initialize the USB device. Determines endpoints and prepares
     * communication.
     * 
     * @param usbManager
     * @throws IOException if the device cannot be opened
     */
    private void initDevice() throws IOException {
        Log.d(LOG_TAG, "setDevice " + this.mUsbDevice);
        // find interface
        if (this.mUsbDevice.getInterfaceCount() != 1) {
            Log.e(LOG_TAG, "Could not find interface");
            return;
        }
        mIntf = this.mUsbDevice.getInterface(0);
        // device should have two endpoints
        if (mIntf.getEndpointCount() != 2) {
            Log.e(LOG_TAG, "Could not find endpoints");
            return;
        }
        // endpoints should be of type bulk
        UsbEndpoint ep = mIntf.getEndpoint(0);
        if (ep.getType() != UsbConstants.USB_ENDPOINT_XFER_BULK) {
            Log.e(LOG_TAG, "Endpoint is not of type bulk");
            return;
        }
        // check endpoint direction
        if (ep.getDirection() == UsbConstants.USB_DIR_IN) {
            mEpIn = mIntf.getEndpoint(0);
            mEpOut = mIntf.getEndpoint(1);
        } else {
            mEpIn = mIntf.getEndpoint(1);
            mEpOut = mIntf.getEndpoint(0);
        }

        UsbDeviceConnection connection = mUsbManager.openDevice(mUsbDevice);
        if (connection != null && connection.claimInterface(mIntf, true)) {
            Log.d(LOG_TAG, "open SUCCESS");
            mConnection = connection;
        } else {
            Log.d(LOG_TAG, "open FAIL");
            throw new IOException("could not open usb connection");
        }
    }

    /**
     * Iterate over all attached USB devices and look for Crazyradio. If
     * Crazyradio is found, request permission.
     */
    private static UsbDevice searchForCrazyradio(Context context, UsbManager usbManager) {
        UsbDevice device = null;
        
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        // Iterate over USB devices
        for (Entry<String, UsbDevice> e : deviceList.entrySet()) {
            Log.i(LOG_TAG, "String: " + e.getKey() + " " + e.getValue().getVendorId() + " " + e.getValue().getProductId());
            if (isCrazyradio(e.getValue())) {
                device = e.getValue();
                break; // stop after first matching device is found
            }
        }

        if (device != null && !usbManager.hasPermission(device)) {
            Log.d(LOG_TAG, "Request permission");
            mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(context.getPackageName()+".USB_PERMISSION"), 0);
            usbManager.requestPermission(device, mPermissionIntent);
        } else if (device != null && usbManager.hasPermission(device)) {
            Log.d(LOG_TAG, "Has permission");
        } else {
            Log.d(LOG_TAG, "device == null");
        }
        return device;
    }

    /**
     * Scan for available channels.
     * 
     * @param usbManager
     * @param usbDevice
     * @return array containing the found channels and datarates.
     * @throws IllegalStateException if the Crazyradio is not attached
     */
    public static ConnectionData[] scanChannels(Context context) throws IllegalStateException {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        UsbDevice usbDevice = searchForCrazyradio(context, usbManager);
        if (usbDevice == null) {
            Log.d(LOG_TAG, "usbDevice is null");
            throw new IllegalStateException("Crazyradio not attached");
        }

        final UsbDeviceConnection connection = usbManager.openDevice(usbDevice);
        return CrazyradioLink.scanChannels(connection);
    }

    /**
     * Scan for available channels.
     * 
     * @return array containing the found channels and datarates.
     */
    public ConnectionData[] scanChannels() {
        return CrazyradioLink.scanChannels(mConnection);
    }

    /**
     * Scan for available channels.
     * 
     * @param connection connection to the USB dongle.
     * @return array containing the found channels and datarates.
     * @throws IllegalStateException if the Crazyradio is not attached (the connection is <code>null</code>).
     */
    protected static ConnectionData[] scanChannels(UsbDeviceConnection connection) {
        List<ConnectionData> result = new ArrayList<ConnectionData>();
        if (connection != null) {
            // null packet
            final byte[] packet = CrtpPacket.NULL_PACKET.toByteArray();
            final byte[] rdata = new byte[64];

            Log.d(LOG_TAG, "Scanning...");
            // scan for all 3 data rates
            for (int b = 0; b < 3; b++) {
                // set data rate
                connection.controlTransfer(0x40, REQUEST_SET_DATA_RATE, b, 0, null, 0, 100);
                connection.controlTransfer(0x40, REQUEST_START_SCAN_CHANNELS, 0, 125, packet, packet.length, 1000);
                final int nfound = connection.controlTransfer(0xc0, REQUEST_GET_SCAN_CHANNELS, 0, 0, rdata, rdata.length, 1000);
                for (int i = 0; i < nfound; i++) {
                    result.add(new ConnectionData(rdata[i], b));
                    Log.d(LOG_TAG, "Channel found: " + rdata[i] + " Data rate: " + b);
                }
            }
        } else {
            Log.d(LOG_TAG, "connection is null");
            throw new IllegalStateException("Crazyradio not attached");
        }
        return result.toArray(new ConnectionData[result.size()]);
    }

    /**
     * Connect to the Crazyflie.
     * 
     * @throws IllegalStateException if the Crazyradio is not attached
     */
    @Override
    public void connect() throws IllegalStateException {
        Log.d(LOG_TAG, "connect()");
        notifyConnectionInitiated();

        if (mConnection != null) {
            if (mRadioLinkThread == null) {
                mRadioLinkThread = new Thread(radioControlRunnable);
                mRadioLinkThread.start();
            }
        } else {
            Log.d(LOG_TAG, "mConnection is null");
            notifyConnectionFailed();
            throw new IllegalStateException("Crazyradio not attached");
        }
    }

    @Override
    public void disconnect() {
        Log.d(LOG_TAG, "disconnect()");
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
    public void send(CrtpPacket p) {
        this.mSendQueue.addLast(p);
    }

    /**
     * Set the radio channel.
     * 
     * @param channel the new channel. Must be in range 0-125.
     */
    public void setRadioChannel(int channel) {
        sendControlTransfer(0x40, REQUEST_SET_RADIO_CHANNEL, channel, 0, null);
    }

    /**
     * Set the data rate.
     * 
     * @param rate new data rate. Possible values are in range 0-2.
     */
    public void setDataRate(int rate) {
        sendControlTransfer(0x40, REQUEST_SET_DATA_RATE, rate, 0, null);
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
        sendControlTransfer(0x40, REQUEST_SET_RADIO_ADDRESS, 0, 0, address);
    }

    /**
     * Set the continuous carrier mode. When enabled the radio chip provides a
     * test mode in which a continuous non-modulated sine wave is emitted. When
     * this mode is activated the radio dongle does not transmit any packets.
     * 
     * @param continuous <code>true</code> to enable the continuous carrier mode
     */
    public void setContinuousCarrier(boolean continuous) {
        sendControlTransfer(0x40, REQUEST_SET_CONT_CARRIER, (continuous ? 1 : 0), 0, null);
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
        sendControlTransfer(0x40, REQUEST_SET_RADIO_ARD, param, 0, null);
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
        sendControlTransfer(0x40, REQUEST_SET_RADIO_ARD, 0x80 | bytes, 0, null);
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
        sendControlTransfer(0x40, REQUEST_SET_RADIO_ARC, count, 0, null);
    }

    /**
     * Handles communication with the dongle to send and receive packets
     */
    private final Runnable radioControlRunnable = new Runnable() {
        @Override
        public void run() {
            int retryBeforeDisconnectRemaining = RETRYCOUNT_BEFORE_DISCONNECT;
            int nextLinkQualityUpdate = PACKETS_BETWEEN_LINK_QUALITY_UPDATE;

            notifyConnectionSetupFinished();

            while (mConnection != null) {
                try {
                    CrtpPacket p = mSendQueue.pollFirst(5, TimeUnit.MILLISECONDS);
                    if (p == null) { // if no packet was available in the send queue
                        p = CrtpPacket.NULL_PACKET;
                    }

                    byte[] receiveData = new byte[33];
                    final byte[] sendData = p.toByteArray();

                    final int receivedByteCount = sendBulkTransfer(sendData, receiveData);
                    
                    //TODO: extract link quality calculation
                    if (receivedByteCount >= 1) {
                        // update link quality status
                        if (nextLinkQualityUpdate <= 0) {
                            final int retransmission = receiveData[0] >> 4;
                            notifyLinkQuality(Math.max(0, (10 - retransmission) * 10));
                            nextLinkQualityUpdate = PACKETS_BETWEEN_LINK_QUALITY_UPDATE;
                        } else {
                            nextLinkQualityUpdate--;
                        }

                        if ((receiveData[0] & 1) != 0) { // check if ack received
                            retryBeforeDisconnectRemaining = RETRYCOUNT_BEFORE_DISCONNECT;
                            handleResponse(Arrays.copyOfRange(receiveData, 1, 1 + (receivedByteCount - 1)));
                        } else {
                            // count lost packets
                            retryBeforeDisconnectRemaining--;
                            if (retryBeforeDisconnectRemaining <= 0) {
                                notifyConnectionLost();
                                disconnect();
                                break;
                            }
                        }
                    } else {
                        Log.w(LOG_TAG, "CrazyradioLink comm error - didn't receive answer");
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    };

    //#Utility functions

    public int sendControlTransfer(int requestType, int request, int value, int index, byte[] data){
        if(mConnection != null){
            int dataLength = (data == null) ? 0 : data.length;
            return mConnection.controlTransfer(requestType, request, value, index, data, dataLength, TRANSFER_TIMEOUT);
        }
        return -1;
    }

    public int sendBulkTransfer(byte[] data, byte[] receiveData){
        int returnCode = -1;
        if(mConnection != null){
            mConnection.bulkTransfer(mEpOut, data, data.length, TRANSFER_TIMEOUT);
            returnCode = mConnection.bulkTransfer(mEpIn, receiveData, receiveData.length, TRANSFER_TIMEOUT);
        }
        return returnCode;
    }


    public static boolean isCrazyradio(UsbDevice device){
        return device.getVendorId() == CrazyradioLink.VENDOR_ID &&
               device.getProductId() == CrazyradioLink.PRODUCT_ID;
    }

}
