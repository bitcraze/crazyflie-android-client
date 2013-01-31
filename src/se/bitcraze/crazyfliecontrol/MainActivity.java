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
import struct.StructClass;
import struct.StructException;
import struct.StructField;

import com.MobileAnarchy.Android.Widgets.Joystick.DualJoystickView;
import com.MobileAnarchy.Android.Widgets.Joystick.JoystickMovedListener;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.view.Menu;

public class MainActivity extends Activity implements Runnable{
	
    private static final String TAG = "CrazyflieControl";

	private DualJoystickView mJoysticks;
	private UsbManager mUsbManager;

	private Object mDevice;

	private UsbEndpoint mEndpointIntr;

	private UsbEndpoint mEpIn;

	private UsbEndpoint mEpOut;

	private UsbDeviceConnection mConnection;
	
	char thrust = 0;
	float roll = 0;
	float pitch = 0;
	float yaw = 0;
	
	public int resolution = 1000;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
    
        mJoysticks = (DualJoystickView) findViewById(R.id.joysticks);
        mJoysticks.setOnJostickMovedListener(_listenerLeft, _listenerRight);
        mJoysticks.setMovementRange(resolution, resolution);
        mUsbManager = (UsbManager)getSystemService(Context.USB_SERVICE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	Intent intent = getIntent();
        Log.d(TAG, "intent: " + intent);
        String action = intent.getAction();
        
        UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
            setDevice(device);
        } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
            if (mDevice != null && mDevice.equals(device)) {
                setDevice(null);
            }
        }
    }
    
    private void setDevice(UsbDevice device) {
        Log.d(TAG, "setDevice " + device);
        if (device.getInterfaceCount() != 1) {
            Log.e(TAG, "could not find interface");
            return;
        }
        UsbInterface intf = device.getInterface(0);
        // device should have two endpoint
        if (intf.getEndpointCount() != 2) {
            Log.e(TAG, "could not find endpoint");
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
        
        if (device != null) {
            UsbDeviceConnection connection = mUsbManager.openDevice(device);
            if (connection != null && connection.claimInterface(intf, true)) {
                Log.d(TAG, "open SUCCESS");
                mConnection = connection;
                Thread thread = new Thread(this);
                thread.start();

            } else {
                Log.d(TAG, "open FAIL");
                mConnection = null;
            }
         }
    }
   
    //Runs the communication loop in a thread...
	public void run() {
		//Set channel 107
		mConnection.controlTransfer(0x40, 0x01, 107, 0, null, 0, 100);
		//Set datarate 250K
		mConnection.controlTransfer(0x40, 0x03,   0, 0, null, 0, 100);
		
		while (mDevice != null) {
			CommanderPacket cpk = new CommanderPacket();
			byte [] data;
			byte [] rdata = new byte[33];
			
			cpk.port = (byte) 0x30;
			cpk.pitch = pitch;
			cpk.roll  = roll;
			cpk.yaw   = yaw;
			cpk.thrust = thrust;
			
			try {
				data = JavaStruct.pack(cpk, ByteOrder.LITTLE_ENDIAN);
				Log.v(TAG, "Sending a packet of " + data.length + " bytes");
				String datastr = "[";
				for (int i=0; i<data.length; i++)
					datastr += "" + data[i] + ", ";
				datastr += "]";
				Log.v(TAG, "Sending data " + datastr);
				mConnection.bulkTransfer(mEpOut, data, data.length, 100);
	        	mConnection.bulkTransfer(mEpIn, rdata, 33, 100);
			} catch (StructException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			

			try {
				Thread.sleep(20, 0);
			} catch (InterruptedException e) {
				;
			}
		}
	}
	
    private JoystickMovedListener _listenerRight = new JoystickMovedListener() {

        @Override
        public void OnMoved(int pan, int tilt) {
        		float stilt = (float) tilt / resolution; 
        		float span = (float) pan / resolution;
        		int t = (int) ((int) (-1) * stilt * 40000);

        		if (stilt < 0.0)
               		thrust = (char) (20000 + t);
               	else
               		thrust = 0;
                yaw = (float) 150.0 * span;

                Log.i("Setpoint", "Thrust: " + Integer.toString((int) thrust)+", Yaw: "+ Float.toString(yaw));
        }

        @Override
        public void OnReleased() {
        		Log.i("Joystick-Right", "Release");
        }
        
        public void OnReturnedToCenter() {
        		Log.i("Joystick-Right", "Center");
        		thrust = 0;
        };
    }; 

	private JoystickMovedListener _listenerLeft = new JoystickMovedListener() {

        @Override
        public void OnMoved(int pan, int tilt) {
        		float stilt = (float) tilt / resolution; 
	    		float span = (float) pan / resolution;

        		pitch = (float) (20.0 * stilt); // Pitch is inversed in firmware
        		roll = (float) (20.0 * span);

        		Log.i("Setpoint", "Pitch" + Float.toString(pitch)+", Roll: "+ Float.toString(roll));
        }

        @Override
        public void OnReleased() {
        }
        
        public void OnReturnedToCenter() {
        };
}; 
	
}
