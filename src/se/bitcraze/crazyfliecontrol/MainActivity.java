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

import java.util.HashMap;
import java.util.Map.Entry;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.InputDevice;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

import com.MobileAnarchy.Android.Widgets.Joystick.DualJoystickView;
import com.MobileAnarchy.Android.Widgets.Joystick.JoystickMovedListener;

public class MainActivity extends Activity{
	
    private static final String TAG = "CrazyflieControl";

	private DualJoystickView mJoysticks;
	private TextView textView_pitch;
	private TextView textView_roll;
	private TextView textView_thrust;
	private TextView textView_yaw;

	private float right_analog_x;
	private float right_analog_y;
	private float left_analog_x;
	private float left_analog_y;

	private RadioLink radioLink;
	public int resolution = 1000;
	
	SharedPreferences preferences;

	private int radioChannel;
	private int radioBandwidth;
	private int mode;
	public float deadzone;

	private String radioChannelDefaultValue;
	private String radioBandwidthDefaultValue;
	private String modeDefaultValue;
	private String deadzoneDefaultValue;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        // Set default preference values
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        // Initialize preferences
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        
        radioChannelDefaultValue = getResources().getString(R.string.preferences_radio_channel_defaultvalue);
        radioBandwidthDefaultValue = getResources().getString(R.string.preferences_radio_bandwidth_defaultvalue);
        modeDefaultValue = getResources().getString(R.string.preferences_mode_defaultvalue);
        deadzoneDefaultValue = getResources().getString(R.string.preferences_deadzone_defaultvalue);
        
        Log.v(TAG, "radiochannel: " + radioChannel);
        Log.v(TAG, "radiobandwidth: " + radioBandwidth);
        
        mJoysticks = (DualJoystickView) findViewById(R.id.joysticks);
        mJoysticks.setMovementRange(resolution, resolution);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()){
    	   	case R.id.preferences:
	    		Intent intent = new Intent(this, PreferencesActivity.class);
	    		startActivity(intent);
	    		break;
	    	case R.id.menu_connect:
	    		radioLink.start();
	    		break;
	    	case R.id.menu_disconnect:
	    		radioLink.stop();
	    		break;
    	}
    	return true;
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	Intent intent = getIntent();
        Log.d(TAG, "intent: " + intent);
        String action = intent.getAction();
        
        if(isExternalControllerConnected()){
        	Toast.makeText(this, "Using external controller", Toast.LENGTH_SHORT).show();
        	mJoysticks.setOnJostickMovedListener(null, null);     	
        }else{        
        	Toast.makeText(this, "Using on-screen controller", Toast.LENGTH_SHORT).show();
        	mJoysticks.setOnJostickMovedListener(_listenerLeft, _listenerRight);
        }
        
        setControlConfig();
        setRadioLink();

        UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
            radioLink.setDevice(device);
        } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
            if (radioLink.getDevice() != null && radioLink.getDevice().equals(device)) {
            	radioLink.setDevice(null);
            }
        }
    }

    @Override
    protected void onRestart() {
    	super.onRestart();
    	onResume();
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        // Check that the event came from a joystick since a generic motion event
        // could be almost anything.
        if ((event.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) != 0 &&
        		event.getAction() == MotionEvent.ACTION_MOVE) {
        	
        	//hardcoded to work with PS3 controller
        	right_analog_x = (float) (event.getAxisValue(MotionEvent.AXIS_Z));
        	right_analog_y = (float) (event.getAxisValue(MotionEvent.AXIS_RZ)); 
        	left_analog_x = (float) (event.getAxisValue(MotionEvent.AXIS_X));
	        left_analog_y = (float) (event.getAxisValue(MotionEvent.AXIS_Y));
	        
            updateFlightData();
            return true;
        }else{
        	return super.dispatchGenericMotionEvent(event);
        }
    }

	private void setControlConfig(){
		this.mode = Integer.parseInt(preferences.getString(PreferencesActivity.KEY_PREF_MODE, modeDefaultValue));
		this.deadzone = Float.parseFloat(preferences.getString(PreferencesActivity.KEY_PREF_DEADZONE, deadzoneDefaultValue));
	}

	private void setRadioLink() {
		if(radioLink == null) {
			radioLink = new RadioLink((UsbManager) getSystemService(Context.USB_SERVICE), this);
		}
        radioChannel = Integer.parseInt(preferences.getString(PreferencesActivity.KEY_PREF_RADIO_CHANNEL, radioChannelDefaultValue));
        radioBandwidth = Integer.parseInt(preferences.getString(PreferencesActivity.KEY_PREF_RADIO_BANDWIDTH, radioBandwidthDefaultValue));

        radioLink.setChannel(radioChannel);
        radioLink.setBandwidth(radioBandwidth);
	}

	public void updateFlightData(){
        textView_pitch = (TextView) findViewById(R.id.pitch);
        textView_roll = (TextView) findViewById(R.id.roll);
        textView_thrust = (TextView) findViewById(R.id.thrust);
        textView_yaw = (TextView) findViewById(R.id.yaw);
        
        textView_pitch.setText("Pitch: " + getPitch() * -1); //inverse
        textView_roll.setText("Roll: " + getRoll());
        textView_thrust.setText("Thrust: " + (float) getThrust());
        textView_yaw.setText("Yaw: " + getYaw());
	}
	
	//does not yet recognize controller connected via sixaxis app (it still works, but the on-screen controls are not disabled)
	private boolean isExternalControllerConnected(){
	    UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
	    HashMap<String,UsbDevice> deviceList = usbManager.getDeviceList();        
	    for(Entry<String, UsbDevice> e : deviceList.entrySet()){
	    	UsbDevice usbDevice = (UsbDevice) e.getValue();
	    	//do we need to be more specific?
	    	for(int i = 0; i < usbDevice.getInterfaceCount();i++){
	    		UsbInterface usbInterface = usbDevice.getInterface(i);
	    		if(usbInterface.getInterfaceClass() == UsbConstants.USB_CLASS_HID){
	    			Toast.makeText(this, "HID device found", Toast.LENGTH_SHORT).show();
	    			return true;
	    		}
	    	}
	    	
	    }
	    return false;
	}

	public char getThrust() {
		float thrust = ((mode == 1 || mode == 3) ? getRightAnalog_Y() : getLeftAnalog_Y());
		
		if(thrust < deadzone*-1){
			return (char) (20000 + (thrust * getThrustFactor()));
		}
		return 0;
	}

	public float getRoll() {
		float roll = (mode == 1 || mode == 2) ? getRightAnalog_X() : getLeftAnalog_X();
		return roll * getRollFactor() * getDeadzone(roll);
	}

	public float getPitch() {
		float pitch = (mode == 1 || mode == 3) ? getLeftAnalog_Y() : getRightAnalog_Y();
		return pitch * getPitchFactor() * getDeadzone(pitch);
	}

	public float getYaw() {
		float yaw = (mode == 1 || mode == 2) ? getLeftAnalog_X() : getRightAnalog_X();
		return yaw * getYawFactor() * getDeadzone(yaw);
	}

	private float getDeadzone(float axis) {
		if(axis < deadzone && axis > deadzone*-1) {
			return 0;
		}
		return 1;
	}

	public float getRightAnalog_X() {
		return right_analog_x;
	}

	public float getRightAnalog_Y() {
		return right_analog_y;
	}

	public float getLeftAnalog_X() {
		return left_analog_x;
	}

	public float getLeftAnalog_Y() {
		return left_analog_y;
	}

	public float getPitchFactor() {
		return 20;
	}

	public float getRollFactor() {
		return 20; 
	}

	public float getYawFactor() {
		return 150;
	}

	public float getThrustFactor() {
		return 40000 * -1;
	}

	private JoystickMovedListener _listenerRight = new JoystickMovedListener() {

        @Override
        public void OnMoved(int pan, int tilt) {
    		right_analog_y = (float) tilt / resolution; 
    		right_analog_x = (float) pan / resolution;

    		updateFlightData();
        }

        @Override
        public void OnReleased() {
        	//Log.i("Joystick-Right", "Release");
        }
        
        public void OnReturnedToCenter() {
        	//Log.i("Joystick-Right", "Center");
        	right_analog_y = 0;
        	right_analog_x = 0;
        };
    }; 

	private JoystickMovedListener _listenerLeft = new JoystickMovedListener() {

        @Override
        public void OnMoved(int pan, int tilt) {
    		left_analog_y = (float) tilt / resolution; 
    		left_analog_x= (float) pan / resolution;

        	updateFlightData();
        }

        @Override
        public void OnReleased() {
        }
        
        public void OnReturnedToCenter() {
    		left_analog_y = 0;
    		left_analog_x = 0;
        };
}; 
	
}
