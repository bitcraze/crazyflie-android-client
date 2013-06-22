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

package se.bitcraze.crazyfliecontrol;

import java.nio.ByteOrder;

import struct.JavaStruct;
import struct.StructException;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.widget.Toast;

public class RadioLink {

	private static final String TAG = "Crazyflie_RadioLink";

	private UsbManager mUsbManager;
	private UsbDevice mDevice;
	private UsbInterface intf;
	private UsbEndpoint mEpIn;
	private UsbEndpoint mEpOut;
	private UsbDeviceConnection mConnection;

	private int channel;
	private int bandwidth;

	private Thread radioLinkThread;

	private MainActivity mJoystick;
	
	private boolean debug = false;


	public RadioLink(UsbManager usbManager, MainActivity joystick){
		this.mUsbManager = usbManager;
		this.mJoystick = joystick;
	}
	
	public void start() {
		Log.d(TAG, "start()");
		if(debug){
			Toast.makeText(this.mJoystick, "DEBUG MODE", Toast.LENGTH_SHORT).show();
            radioLinkThread = new Thread(radioControlRunnable);
            radioLinkThread.start();
		}
        if (mDevice != null && intf != null) {
            UsbDeviceConnection connection = mUsbManager.openDevice(mDevice);
            if (connection != null && connection.claimInterface(intf, true)) {
                Log.d(TAG, "open SUCCESS");
                mConnection = connection;
                radioLinkThread = new Thread(radioControlRunnable);
                radioLinkThread.start();
            } else {
                Log.d(TAG, "open FAIL");
                mConnection = null;
            }
         }
	}
	
	public void stop() {
		Log.d(TAG, "stop");
		if(radioLinkThread != null){
			radioLinkThread.interrupt();
		}
		mConnection = null;
	}
	
	public int getChannel() {
		return channel;
	}

	public void setChannel(int channel) {
		this.channel = channel;
	}
	
    public int getBandwidth() {
		return bandwidth;
	}

	public void setBandwidth(int bandwidth) {
		this.bandwidth = bandwidth;
	}
	
	public Object getDevice(){
		return mDevice;
	}

	public void setDevice(UsbDevice device) {
        Log.d(TAG, "setDevice " + device);
        if (device.getInterfaceCount() != 1) {
            Log.e(TAG, "could not find interface");
            return;
        }
        intf = device.getInterface(0);
        // device should have two endpoints
        if (intf.getEndpointCount() != 2) {
            Log.e(TAG, "could not find endpoints");
            return;
        }
        // endpoints should be of type bulk
        UsbEndpoint ep = intf.getEndpoint(0);
        if (ep.getType() != UsbConstants.USB_ENDPOINT_XFER_BULK) {
            Log.e(TAG, "endpoint is not bulk type");
            return;
        }
        mDevice = device;
        
        if (ep.getDirection()==UsbConstants.USB_DIR_IN) {
        	mEpIn = intf.getEndpoint(0);
        	mEpOut = intf.getEndpoint(1);
        } else {
        	mEpIn = intf.getEndpoint(0);
        	mEpOut = intf.getEndpoint(1);
        }
    }
	
	private Runnable radioControlRunnable = new Runnable() {
	
		//Run the Radio link loop to send attitude setpoint to the copter
		public void run() {
			//TODO: can channel and datarate be changed at any time?
			if(mConnection != null){
				//Set channel
				mConnection.controlTransfer(0x40, 0x01, channel, 0, null, 0, 100);
				//Set datarate
				mConnection.controlTransfer(0x40, 0x03, bandwidth, 0, null, 0, 100);
			}

			while (mDevice != null || debug) {
				//Log.v(TAG, "radioControlRunnable running");
				CommanderPacket cpk = new CommanderPacket(mJoystick.getRoll(), mJoystick.getPitch(),
														  mJoystick.getYaw(), (char) (mJoystick.getThrust() * 1000));

				byte [] data;
				byte [] rdata = new byte[33];

//				Log.i(TAG, "P: " + mJoystick.getPitch() + 
//						  " R: " + mJoystick.getRoll() + 
//						  " Y: " + mJoystick.getYaw() +
//						  " T: " + mJoystick.getThrust());

				try {
					data = JavaStruct.pack(cpk, ByteOrder.LITTLE_ENDIAN);
//					Log.v(TAG, "Sending a packet of " + data.length + " bytes");
//									String datastr = "[";
//									for (int i=0; i<data.length; i++)
//										datastr += "" + data[i] + ", ";
//									datastr += "]";
//					Log.v(TAG, "Sending data " + datastr);
					if(mConnection != null){
						mConnection.bulkTransfer(mEpOut, data, data.length, 100);
						mConnection.bulkTransfer(mEpIn, rdata, 33, 100);
					}
				} catch (StructException e1) {
					e1.printStackTrace();
				}

				try {
					Thread.sleep(20, 0);
				} catch (InterruptedException e) {
					//Log.v(TAG, "radioControlRunnable catch block");
					break;
				}
			}
		}
	};
}
