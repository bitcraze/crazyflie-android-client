package se.bitcraze.crazyflielib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import se.bitcraze.crazyflielib.crtp.CRTPPacket;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

public class CrazyradioLink extends AbstractLink {

	// CrazyRadio USB device IDs
	public static final int VENDOR_ID = 6421;
	public static final int PRODUCT_ID = 30583;
	
	public static final int RETRYCOUNT_BEFORE_DISCONNECT = 10;
	
	public static final int PACKETS_BETWEEN_LINK_QUALITY_UPDATE = 5;
	
	private static final String LOG_TAG = "Crazyflie_RadioLink";
	
	private final UsbDevice mUsbDevice;
	private UsbInterface mIntf;
    private UsbEndpoint mEpIn;
    private UsbEndpoint mEpOut;
    private UsbDeviceConnection mConnection;
	
    private final ConnectionData mConnectionData;
    
    private Thread mRadioLinkThread;
    
    private final BlockingDeque<CRTPPacket> mSendQueue;
    
    /**
     * Holds information about a specific connection.
     */
    public static class ConnectionData {
    	private final int channel;
    	private final int bandwidth;
    	
		public ConnectionData(int channel, int bandwidth) {
			this.channel = channel;
			this.bandwidth = bandwidth;
		}

		public int getChannel() {
			return channel;
		}

		public int getBandwidth() {
			return bandwidth;
		}
    }
    
    /**
     * Create a new link using the Crazyradio.
     * @param usbManager
     * @param usbDevice
     * @param connectionData
     * @throws IllegalArgumentException if usbManager or usbDevice is <code>null</code>
     */
	public CrazyradioLink(UsbManager usbManager, UsbDevice usbDevice, ConnectionData connectionData) {
		if(usbManager == null || usbDevice == null) {
			throw new IllegalArgumentException("USB manager and device must not be null");
		}
		
		this.mUsbDevice = usbDevice;
		this.mConnectionData = connectionData;
		initDevice(usbManager);
		
		this.mSendQueue = new LinkedBlockingDeque<CRTPPacket>();
	}
	
	private void initDevice(UsbManager usbManager) {
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

        UsbDeviceConnection connection = usbManager.openDevice(mUsbDevice);
        if (connection != null && connection.claimInterface(mIntf, true)) {
            Log.d(LOG_TAG, "open SUCCESS");
            mConnection = connection;
        } else {
            Log.d(LOG_TAG, "open FAIL");
            throw new RuntimeException("could not open usb connection"); // TODO replace with more specific exception
        }
    }
	
	/**
	 * Scan for available channels.
	 * @return array containing the found channels and bandwidths.
	 * @throws IllegalStateException
	 */
    public static ConnectionData[] scanChannels(UsbManager usbManager, UsbDevice usbDevice) throws IllegalStateException {
        List<ConnectionData> result = new ArrayList<ConnectionData>();

        final UsbDeviceConnection connection = usbManager.openDevice(usbDevice);
        if (connection != null) {
            //null packet
            byte[] packet = new byte[1];
            packet[0] = (byte) 255;
            
            byte [] rdata = new byte[64];

            Log.d(LOG_TAG, "Scanning...");
            //scan for all 3 data rates
            for(int b = 0; b < 3; b++){
                //set bandwidth
                connection.controlTransfer(0x40, 0x03, b, 0, null, 0, 100);

                connection.controlTransfer(0x40, 0x21, 0, 125, packet, packet.length, 1000);
            	final int nfound = connection.controlTransfer(0xc0, 0x21, 0, 0, rdata, rdata.length, 1000);
            	for(int i=0; i<nfound; i++) {
            		result.add(new ConnectionData(rdata[i], b));
                    Log.d(LOG_TAG, "Channel found: " + rdata[i] + " Data rate: " + b);
            	}
            }
        } else {
            Log.d(LOG_TAG, "connection is null");
            throw new IllegalStateException("CrazyRadio not attached");
        }
        
        return result.toArray(new ConnectionData[result.size()]);
    }
	
	public int getChannel() {
        return this.mConnectionData.getChannel();
    }

    public int getBandwidth() {
        return this.mConnectionData.getBandwidth();
    }
	
	@Override
	public void connect() throws IllegalStateException {
		Log.d(LOG_TAG, "RadioLink start()");
		notifyConnectionInitiated();
		
        if (mConnection != null) {
            if (mRadioLinkThread == null) {
                mRadioLinkThread = new Thread(radioControlRunnable);
                mRadioLinkThread.start();
            }
        } else {
            Log.d(LOG_TAG, "mConnection is null");
            notifyConnectionFailed();
            throw new IllegalStateException("CrazyRadio not attached");
        }
	}

	@Override
	public void disconnect() {
		Log.d(LOG_TAG, "RadioLink stop()");
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
	public void send(CRTPPacket p) {
		this.mSendQueue.addLast(p);
	}
	
	private final Runnable radioControlRunnable = new Runnable() {
		// Run the Radio link loop to send and receive packets
		@Override
        public void run() {
            int retryBeforeDisconnectRemaining = RETRYCOUNT_BEFORE_DISCONNECT;
            int nextLinkQualityUpdate = PACKETS_BETWEEN_LINK_QUALITY_UPDATE;
			
            if (mConnection != null) {
                // Set channel
                mConnection.controlTransfer(0x40, 0x01, getChannel(), 0, null, 0, 100);
                // Set datarate
                mConnection.controlTransfer(0x40, 0x03, getBandwidth(), 0, null, 0, 100);
            }

            notifyConnectionSetupFinished();
            
            while (mConnection != null) {
            	try {
	                CRTPPacket p = mSendQueue.pollFirst(5, TimeUnit.MILLISECONDS);
	                if( p == null ) { // if no packet was available in the send queue
	                	p = CRTPPacket.NULL_PACKET;
	                }
	                
	                byte[] receiveData = new byte[33];
	                final byte[]sendData = p.toByteArray();
                    if (mConnection != null) {
                        mConnection.bulkTransfer(mEpOut, sendData, sendData.length, 100);
                        final int receivedByteCount = mConnection.bulkTransfer(mEpIn, receiveData, receiveData.length, 100);
                        
                        if(receivedByteCount >= 1) {
                        	// update link quality status
                        	if(nextLinkQualityUpdate <= 0) {
	                        	final int retransmission = receiveData[0] >> 4;
	                        	notifyLinkQuality(Math.max(0, (10 - retransmission) * 10));
	                        	nextLinkQualityUpdate = PACKETS_BETWEEN_LINK_QUALITY_UPDATE;
                        	} else {
                        		nextLinkQualityUpdate--;
                        	}
                        	
	                        if((receiveData[0] & 1) != 0) { // check if ack received
	                        	retryBeforeDisconnectRemaining = RETRYCOUNT_BEFORE_DISCONNECT;
	                        	handleResponse(Arrays.copyOfRange(receiveData, 1, 1 + (receivedByteCount - 1)));
	                        } else {
	                        	// count lost packets
	                        	retryBeforeDisconnectRemaining--;
	                        	if(retryBeforeDisconnectRemaining <= 0) {
	                        		notifyConnectionLost();
	                        		disconnect();
	                        		break;
	                        	}
	                        }
                        } else {
                        	Log.w(LOG_TAG, "CrazyradioLink comm error - didn't receive answer");
                        }
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
	};

}
