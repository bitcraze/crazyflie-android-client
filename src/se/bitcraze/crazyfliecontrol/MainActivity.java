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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.MobileAnarchy.Android.Widgets.Joystick.DualJoystickView;
import com.MobileAnarchy.Android.Widgets.Joystick.JoystickMovedListener;

public class MainActivity extends Activity{
	
    private static final String TAG = "CrazyflieControl";

	private DualJoystickView mJoysticks;

	private char thrust = 0;
	private float roll = 0;
	private float pitch = 0;
	private float yaw = 0;

	private RadioLink radioLink;
	
	public int resolution = 1000;
	
	SharedPreferences preferences;

	private int radioChannel;
	private int radioBandwidth;

	private String radioChannelDefaultValue;
	private String radioBandwidthDefaultValue;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);

        // Set default preference values
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        // Initialize preferences
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        
        radioChannelDefaultValue = getResources().getString(R.string.preferences_radio_channel_defaultvalue);
        radioBandwidthDefaultValue = getResources().getString(R.string.preferences_radio_bandwidth_defaultvalue);
        
        Log.v(TAG, "radiochannel: " + radioChannel);
        Log.v(TAG, "radiobandwidth: " + radioBandwidth);
        
        mJoysticks = (DualJoystickView) findViewById(R.id.joysticks);
        mJoysticks.setOnJostickMovedListener(_listenerLeft, _listenerRight);
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

	private void setRadioLink() {
		if(radioLink == null) {
			radioLink = new RadioLink((UsbManager) getSystemService(Context.USB_SERVICE), this);
		}
        radioChannel = Integer.parseInt(preferences.getString(PreferencesActivity.KEY_PREF_RADIO_CHANNEL, radioChannelDefaultValue));
        radioBandwidth = Integer.parseInt(preferences.getString(PreferencesActivity.KEY_PREF_RADIO_BANDWIDTH, radioBandwidthDefaultValue));

        radioLink.setChannel(radioChannel);
        radioLink.setBandwidth(radioBandwidth);
	}

    public char getThrust() {
		return thrust;
	}

	public void setThrust(char thrust) {
		this.thrust = thrust;
	}

	public float getRoll() {
		return roll;
	}

	public void setRoll(float roll) {
		this.roll = roll;
	}

	public float getPitch() {
		return pitch;
	}

	public void setPitch(float pitch) {
		this.pitch = pitch;
	}

	public float getYaw() {
		return yaw;
	}

	public void setYaw(float yaw) {
		this.yaw = yaw;
	}

	private JoystickMovedListener _listenerRight = new JoystickMovedListener() {

        @Override
        public void OnMoved(int pan, int tilt) {
        		float stilt = (float) tilt / resolution; 
        		float span = (float) pan / resolution;
        		int t = (int) ((int) (-1) * stilt * 40000);

        		if (stilt < 0.0)
               		setThrust((char) (20000 + t));
               	else
               		setThrust((char) 0);
                setYaw((float) 150.0 * span);

                //Log.i("Setpoint", "Thrust: " + Integer.toString((int) thrust)+", Yaw: "+ Float.toString(yaw));
        }

        @Override
        public void OnReleased() {
        		//Log.i("Joystick-Right", "Release");
        }
        
        public void OnReturnedToCenter() {
        		//Log.i("Joystick-Right", "Center");
        		setThrust((char) 0);
        };
    }; 

	private JoystickMovedListener _listenerLeft = new JoystickMovedListener() {

        @Override
        public void OnMoved(int pan, int tilt) {
        		float stilt = (float) tilt / resolution; 
	    		float span = (float) pan / resolution;

        		setPitch((float) (20.0 * stilt)); // Pitch is inversed in firmware
        		setRoll((float) (20.0 * span));

        		//Log.i("Setpoint", "Pitch" + Float.toString(pitch)+", Roll: "+ Float.toString(roll));
        }

        @Override
        public void OnReleased() {
        }
        
        public void OnReturnedToCenter() {
        };
}; 
	
}
