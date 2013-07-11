package se.bitcraze.crazyflielib;

import java.nio.ByteOrder;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import struct.JavaStruct;
import struct.StructException;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

public class CrazyradioLink extends AbstractLink {

	private static final String LOG_TAG = "Crazyflie_RadioLink";
	
	private final UsbDevice mUsbDevice;
	private UsbInterface mIntf;
    private UsbEndpoint mEpIn;
    private UsbEndpoint mEpOut;
    private UsbDeviceConnection mConnection;
	
    private int mChannel;
    private int mBandwidth;
    
    private Thread mRadioLinkThread;
    
    private final BlockingDeque<Packet> mSendQueue;
    
    /**
     * Create a new link using the Crazyradio.
     * @param usbManager
     * @param usbDevice
     * @throws IllegalArgumentException if usbManager or usbDevice is <code>null</code>
     */
	public CrazyradioLink(UsbManager usbManager, UsbDevice usbDevice) {
		if(usbManager == null || usbDevice == null) {
			throw new IllegalArgumentException("USB manager and device must not be null");
		}
		
		this.mUsbDevice = usbDevice;
		initDevice(usbManager);
		
		this.mSendQueue = new LinkedBlockingDeque<Packet>();
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
	 * Scan for available channels. Currently only handles the first found channel.
	 * @return array containing the found channel and bandwidth.
	 * @throws IllegalStateException
	 */
    public int[] scanChannels() throws IllegalStateException {
        int[] result = new int[]{-1,-1};

        if (mConnection != null) {
            //null packet
            byte[] packet = new byte[1];
            packet[0] = (byte) 255;
            
            byte [] rdata = new byte[64];

            Log.d(LOG_TAG, "Scanning...");
            //scan for all 3 data rates
            for(int b = 0; b < 3; b++){
                //set bandwidth
                mConnection.controlTransfer(0x40, 0x03, b, 0, null, 0, 100);

            	mConnection.controlTransfer(0x40, 0x21, 0, 125, packet, packet.length, 1000);
            	int nfound = mConnection.controlTransfer(0xc0, 0x21, 0, 0, rdata, rdata.length, 1000);
            	if (nfound > 0) {
            		result[0] = rdata[0]; //channel
                    result[1] = b; //bandwidth
                    Log.d(LOG_TAG, "Channel found: " + rdata[0] + " Data rate: " + b);
                    //TODO: handle more than one found channel
                    break;
            	}

            	if(rdata[0] != 0){
                    break;
                }
            }
        } else {
            Log.d(LOG_TAG, "mConnection is null");
            throw new IllegalStateException("CrazyRadio not attached");
        }
        return result;
    }
	
	public int getChannel() {
        return mChannel;
    }

    public void setChannel(int channel) {
        this.mChannel = channel;
    }

    public int getBandwidth() {
        return mBandwidth;
    }

    public void setBandwidth(int bandwidth) {
        this.mBandwidth = bandwidth;
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
	public void send(Packet p) {
		this.mSendQueue.addLast(p);
	}
	
	private final Runnable radioControlRunnable = new Runnable() {
		// Run the Radio link loop to send attitude setpoint to the copter
		@Override
        public void run() {
            // TODO: can channel and datarate be changed at any time?
            if (mConnection != null) {
                // Set channel
                mConnection.controlTransfer(0x40, 0x01, getChannel(), 0, null, 0, 100);
                // Set datarate
                mConnection.controlTransfer(0x40, 0x03, getBandwidth(), 0, null, 0, 100);
            }

            notifyConnectionSetupFinished();
            
            while (mConnection != null) {
                // Log.v(TAG, "radioControlRunnable running");
            	try {
	                final Packet p = mSendQueue.takeFirst();
	
	                byte[] data;
	                byte[] rdata = new byte[33];
	
	                // Log.i(TAG, "P: " + mJoystick.getPitch() +
	                // " R: " + mJoystick.getRoll() +
	                // " Y: " + mJoystick.getYaw() +
	                // " T: " + mJoystick.getThrust());
	
	                try {
	                    data = JavaStruct.pack(p, ByteOrder.LITTLE_ENDIAN);
	                    // Log.v(TAG, "Sending a packet of " + data.length + " bytes");
	                    // String datastr = "[";
	                    // for (int i=0; i<data.length; i++)
	                    // datastr += "" + data[i] + ", ";
	                    // datastr += "]";
	                    // Log.v(TAG, "Sending data " + datastr);
	                    if (mConnection != null) {
	                        mConnection.bulkTransfer(mEpOut, data, data.length, 100);
	                        mConnection.bulkTransfer(mEpIn, rdata, 33, 100);
	                    }
	                } catch (StructException e) {
	                    e.printStackTrace();
	                }
                } catch (InterruptedException e) {
                    // Log.v(TAG, "radioControlRunnable catch block");
                    break;
                }
            }
        }
	};

}
