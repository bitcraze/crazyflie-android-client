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

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;

import se.bitcraze.crazyfliecontrol.SelectConnectionDialogFragment.SelectCrazyflieDialogListener;
import se.bitcraze.crazyflielib.ConnectionAdapter;
import se.bitcraze.crazyflielib.CrazyradioLink;
import se.bitcraze.crazyflielib.CrazyradioLink.ConnectionData;
import se.bitcraze.crazyflielib.Link;
import se.bitcraze.crazyflielib.crtp.CommanderPacket;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Toast;

import com.MobileAnarchy.Android.Widgets.Joystick.DualJoystickView;
import com.MobileAnarchy.Android.Widgets.Joystick.JoystickMovedListener;

public class MainActivity extends Activity {

    private static final String TAG = "CrazyflieControl";

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    
    private static final int MAX_THRUST = 65535;

    private DualJoystickView mJoysticks;
    private FlightDataView mFlightDataView;

    private Link crazyflieLink;
    private int resolution = 1000;

    private SharedPreferences mPreferences;

    private int maxRollPitchAngle;
    private int maxYawAngle;
    private int maxThrust;
    private int minThrust;
    private boolean xmode;

    private String mRadioChannelDefaultValue;
    private String mRadioBandwidthDefaultValue;

    private String mMaxRollPitchAngleDefaultValue;
    private String mMaxYawAngleDefaultValue;
    private String mMaxThrustDefaultValue;
    private String mMinThrustDefaultValue;

    private UsbManager mUsbManager;
    private UsbDevice mDevice;
    private PendingIntent mPermissionIntent;

    private boolean isOnscreenControllerDisabled;
    private boolean mPermissionAsked = false;
    private boolean mDoubleBackToExitPressedOnce = false;

    private Thread mSendJoystickDataThread;

    private String[] mDatarateStrings;

    private Controls mControls;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setDefaultPreferenceValues();

        mControls = new Controls(this, mPreferences);
        mControls.setDefaultPreferenceValues(getResources());

        mJoysticks = (DualJoystickView) findViewById(R.id.joysticks);
        mJoysticks.setMovementRange(resolution, resolution);

        mFlightDataView = (FlightDataView) findViewById(R.id.flightdataview);

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filter);
    }

    private void setDefaultPreferenceValues(){
        // Set default preference values
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        // Initialize preferences
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        mRadioChannelDefaultValue = getString(R.string.preferences_radio_channel_defaultValue);
        mRadioBandwidthDefaultValue = getString(R.string.preferences_radio_bandwidth_defaultValue);

        mMaxRollPitchAngleDefaultValue = getString(R.string.preferences_maxRollPitchAngle_defaultValue);
        mMaxYawAngleDefaultValue = getString(R.string.preferences_maxYawAngle_defaultValue);
        mMaxThrustDefaultValue = getString(R.string.preferences_maxThrust_defaultValue);
        mMinThrustDefaultValue = getString(R.string.preferences_minThrust_defaultValue);

        mDatarateStrings = getResources().getStringArray(R.array.radioBandwidthEntries);
    }

    private void checkScreenLock() {
        boolean isScreenLock = mPreferences.getBoolean(PreferencesActivity.KEY_PREF_SCREEN_ROTATION_LOCK_BOOL, false);
        if(isScreenLock){
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }else{
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                searchForCrazyRadio();
                try {
                    linkConnect();
                } catch (IllegalStateException e) {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.menu_disconnect:
                linkDisconnect();
                break;
            case R.id.menu_radio_scan:
                radioScan();
                break;
            case R.id.preferences:
                Intent intent = new Intent(this, PreferencesActivity.class);
                startActivity(intent);
                break;
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        resetInputMethod();
        setControlConfig();
        checkScreenLock();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mControls.resetAxisValues();
        if (crazyflieLink != null) {
            linkDisconnect();
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mUsbReceiver);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (mDoubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }
        this.mDoubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                mDoubleBackToExitPressedOnce = false;

            }
        }, 2000);
    }

    private void updateFlightData(){
        mFlightDataView.updateFlightData(getPitch(), getRoll(), getThrust(), getYaw());
    }
    
    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        // Check that the event came from a joystick since a generic motion event could be almost anything.
        if ((event.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) != 0 && event.getAction() == MotionEvent.ACTION_MOVE) {
            mControls.dealWithMotionEvent(event);
            updateFlightData();
            return true;
        } else {
            return super.dispatchGenericMotionEvent(event);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // TODO: works for PS3 controller, but does it also work for other controllers?
        // do not call super if key event comes from a gamepad, otherwise the buttons can quit the app
        if (event.getSource() == 1281) {
            mControls.dealWithKeyEvent(event);
            // exception for OUYA controllers
            if (!Build.MODEL.toUpperCase(Locale.getDefault()).contains("OUYA")) {
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void setRadioChannelAndDatarate(int channel, int datarate) {
        if (channel != -1 && datarate != -1) {
            SharedPreferences.Editor editor = mPreferences.edit();
            editor.putString(PreferencesActivity.KEY_PREF_RADIO_CHANNEL, String.valueOf(channel));
            editor.putString(PreferencesActivity.KEY_PREF_RADIO_BANDWIDTH, String.valueOf(datarate));
            editor.commit();

            Toast.makeText(this,"Channel: " + channel + " Data rate: " + mDatarateStrings[datarate] +
                           "\nSetting preferences...", Toast.LENGTH_SHORT).show();
        }
    }

    public void disableOnscreenController() {
        Toast.makeText(this, "Using external controller", Toast.LENGTH_SHORT).show();
        mJoysticks.setOnJostickMovedListener(null, null);
        this.isOnscreenControllerDisabled = true;
    }

    public boolean isOnscreenControllerDisabled() {
        return isOnscreenControllerDisabled;
    }

    private void resetInputMethod() {
        Toast.makeText(this, "Using on-screen controller", Toast.LENGTH_SHORT).show();
        this.isOnscreenControllerDisabled = false;
        mJoysticks.setOnJostickMovedListener(_listenerLeft, _listenerRight);
    }

    private void setControlConfig() {
        mControls.setControlConfig();
        if (mPreferences.getBoolean(PreferencesActivity.KEY_PREF_AFC_BOOL, false)) {
            this.maxRollPitchAngle = Integer.parseInt(mPreferences.getString(PreferencesActivity.KEY_PREF_MAX_ROLLPITCH_ANGLE, mMaxRollPitchAngleDefaultValue));
            this.maxYawAngle = Integer.parseInt(mPreferences.getString(PreferencesActivity.KEY_PREF_MAX_YAW_ANGLE, mMaxYawAngleDefaultValue));
            this.maxThrust = Integer.parseInt(mPreferences.getString(PreferencesActivity.KEY_PREF_MAX_THRUST, mMaxThrustDefaultValue));
            this.minThrust = Integer.parseInt(mPreferences.getString(PreferencesActivity.KEY_PREF_MIN_THRUST, mMinThrustDefaultValue));
            this.xmode = mPreferences.getBoolean(PreferencesActivity.KEY_PREF_XMODE, false);
        } else {
            this.maxRollPitchAngle = Integer.parseInt(mMaxRollPitchAngleDefaultValue);
            this.maxYawAngle = Integer.parseInt(mMaxYawAngleDefaultValue);
            this.maxThrust = Integer.parseInt(mMaxThrustDefaultValue);
            this.minThrust = Integer.parseInt(mMinThrustDefaultValue);
            this.xmode = false;
        }
    }

    /**
     * Iterate over all attached USB devices and look for CrazyRadio. If
     * CrazyRadio is found, request permission.
     */
    private void searchForCrazyRadio() {
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        // Iterate over USB devices
        for (Entry<String, UsbDevice> e : deviceList.entrySet()) {
            Log.i(TAG, "String: " + e.getKey() + " " + e.getValue().getVendorId() + " " + e.getValue().getProductId());
            if (CrazyradioLink.isCrazyRadio(e.getValue())) {
                mDevice = e.getValue();
                break; // stop after first matching device is found
            }
        }

        if (mDevice != null && !this.mPermissionAsked) {
            Log.d(TAG, "Request permission");
            mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
            mUsbManager.requestPermission(mDevice, mPermissionIntent);
            mPermissionAsked = true;
        } else {
            Log.d(TAG, "device == null");
        }
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive");
            if (ACTION_USB_PERMISSION.equals(action)) {
                Log.d(TAG, "USB_PERMISSON");
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            Toast.makeText(MainActivity.this, "CrazyRadio attached", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.d(TAG, "permission denied for device " + device);
                    }
                }
            }
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                Log.d(TAG, "USB device detached ");
                if (device != null && CrazyradioLink.isCrazyRadio(device)) {
                    Toast.makeText(MainActivity.this, "CrazyRadio detached", Toast.LENGTH_SHORT).show();
                    if (crazyflieLink != null) {
                        Log.d(TAG, "linkDisconnect()");
                        linkDisconnect();
                        mPermissionAsked = false;
                    }
                }
            }
        }
    };

    private void linkConnect() {
        // ensure previous link is disconnected
        linkDisconnect();

        int radioChannel = Integer.parseInt(mPreferences.getString(
                PreferencesActivity.KEY_PREF_RADIO_CHANNEL, mRadioChannelDefaultValue));
        int radioBandwidth = Integer.parseInt(mPreferences.getString(
                PreferencesActivity.KEY_PREF_RADIO_BANDWIDTH, mRadioBandwidthDefaultValue));

        try {
            // create link
            crazyflieLink = new CrazyradioLink(mUsbManager, mDevice,
                    new CrazyradioLink.ConnectionData(radioChannel, radioBandwidth));

            // add listener for connection status
            crazyflieLink.addConnectionListener(new ConnectionAdapter() {
                @Override
                public void connectionSetupFinished(Link l) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void connectionLost(Link l) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Connection lost", Toast.LENGTH_SHORT).show();
                        }
                    });
                    linkDisconnect();
                }

                @Override
                public void connectionFailed(Link l) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Connection failed", Toast.LENGTH_SHORT).show();
                        }
                    });
                    linkDisconnect();
                }

                @Override
                public void linkQualityUpdate(Link l, final int quality) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mFlightDataView.setLinkQualityText(quality + "%");
                        }
                    });
                }
            });

            // connect and start thread to periodically send commands containing
            // the user input
            crazyflieLink.connect();
            mSendJoystickDataThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (crazyflieLink != null) {
                        crazyflieLink.send(new CommanderPacket(getRoll(), getPitch(), getYaw(),
                                (char) (getThrust()/100 * MAX_THRUST), isXmode()));

                        try {
                            Thread.sleep(20, 0);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
            });
            mSendJoystickDataThread.start();
        } catch (IllegalArgumentException e) {
            Log.d(TAG, e.getMessage());
            Toast.makeText(this, "Crazyradio not attached", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public Link getCrazyflieLink(){
        return crazyflieLink;
    }
    
    public void linkDisconnect() {
        if (crazyflieLink != null) {
            crazyflieLink.disconnect();
            crazyflieLink = null;
        }
        if (mSendJoystickDataThread != null) {
            mSendJoystickDataThread.interrupt();
            mSendJoystickDataThread = null;
        }
        
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // link quality is not available when there is no active connection
                mFlightDataView.setLinkQualityText("n/a");
            }
        });
    }

    private void radioScan() {
        searchForCrazyRadio();
    	new AsyncTask<UsbManager, Void, ConnectionData[]>() {

    		private Exception mException = null;
    		private ProgressDialog mProgress;
    		
			@Override
			protected void onPreExecute() {
				mProgress = ProgressDialog.show(MainActivity.this,
						"Radio Scan", "Searching for the Crazyflie...", true, false);
			}

			@Override
			protected ConnectionData[] doInBackground(UsbManager... params) {
				try {
					return CrazyradioLink.scanChannels(mUsbManager, mDevice);
				} catch(IllegalStateException e) {
					mException = e;
					return null;
				}
			}

			@Override
			protected void onPostExecute(ConnectionData[] result) {
				mProgress.dismiss();
				
				if(mException != null) {
					Toast.makeText(MainActivity.this, mException.getMessage(), Toast.LENGTH_SHORT).show();
				} else {
					//TEST DATA for debugging SelectionConnectionDialogFragment (replace with test!)
//			        result = new ConnectionData[3];
//			        result[0] = new ConnectionData(13, 2);
//			        result[1] = new ConnectionData(15, 1);
//			        result[2] = new ConnectionData(125, 2);
					
					if (result != null && result.length > 0) {
		                if(result.length > 1){
		                    // let user choose connection, if there is more than one Crazyflie 
		                    showSelectConnectionDialog(result);
		                }else{
		                    // use first channel
		                    setRadioChannelAndDatarate(result[0].getChannel(), result[0].getDataRate());
		                }
		            } else {
		                Toast.makeText(MainActivity.this, "No connection found", Toast.LENGTH_SHORT).show();
		            }
				}
			}
		}.execute();
    }

    private void showSelectConnectionDialog(final ConnectionData[] result) {
        SelectConnectionDialogFragment selectConnectionDialogFragment = new SelectConnectionDialogFragment();
        //supply list of Crazyflie connections as arguments
        Bundle args = new Bundle();
        String[] crazyflieArray = new String[result.length];
        for(int i = 0; i < result.length; i++){
            crazyflieArray[i] = i + ": Channel " + result[i].getChannel() + ", Data rate " + mDatarateStrings[result[i].getDataRate()];
        }
        args.putStringArray("connection_array", crazyflieArray);
        selectConnectionDialogFragment.setArguments(args);
        selectConnectionDialogFragment.setListener(new SelectCrazyflieDialogListener(){
            @Override
            public void onClick(int which) {
                setRadioChannelAndDatarate(result[which].getChannel(), result[which].getDataRate());
            }
        });
        selectConnectionDialogFragment.show(getFragmentManager(), "select_crazyflie");
    }

    public float getThrust() {
        float thrust = ((mControls.getMode() == 1 || mControls.getMode() == 3) ? mControls.getRightAnalog_Y() : mControls.getLeftAnalog_Y());
        thrust = thrust * -1; // invert
        if (thrust > mControls.getDeadzone()) {
            return minThrust + (thrust * getThrustFactor());
        }
        return 0;
    }

    public float getRoll() {
        float roll = (mControls.getMode() == 1 || mControls.getMode() == 2) ? mControls.getRightAnalog_X() : mControls.getLeftAnalog_X();
        return (roll + mControls.getRollTrim()) * getRollPitchFactor() * mControls.getDeadzone(roll);
    }

    public float getPitch() {
        float pitch = (mControls.getMode() == 1 || mControls.getMode() == 3) ? mControls.getLeftAnalog_Y() : mControls.getRightAnalog_Y();
        return (pitch + mControls.getPitchTrim()) * getRollPitchFactor() * mControls.getDeadzone(pitch);
    }

    public float getYaw() {
        float yaw = 0;
        if(mControls.useSplitAxisYaw()){
            yaw = mControls.getSplitAxisYawRight() - mControls.getSplitAxisYawLeft();
        }else{
            yaw = (mControls.getMode() == 1 || mControls.getMode() == 2) ? mControls.getLeftAnalog_X() : mControls.getRightAnalog_X();
        }
        return yaw * getYawFactor() * mControls.getDeadzone(yaw);
    }

    public float getRollPitchFactor() {
        return maxRollPitchAngle;
    }

    public float getYawFactor() {
        return maxYawAngle;
    }

    public float getThrustFactor() {
        int addThrust = 0;
        if ((maxThrust - minThrust) < 0) {
            addThrust = 0; // do not allow negative values
        } else {
            addThrust = (maxThrust - minThrust);
        }
        return addThrust;
    }

    public boolean isXmode() {
        return this.xmode;
    }

    private JoystickMovedListener _listenerRight = new JoystickMovedListener() {

        @Override
        public void OnMoved(int pan, int tilt) {
            mControls.setRightAnalogY((float) tilt / resolution);
            mControls.setRightAnalogX((float) pan / resolution);

            updateFlightData();
        }

        @Override
        public void OnReleased() {
            // Log.i("Joystick-Right", "Release");
            mControls.setRightAnalogY(0);
            mControls.setRightAnalogX(0);
        }

        public void OnReturnedToCenter() {
            // Log.i("Joystick-Right", "Center");
            mControls.setRightAnalogY(0);
            mControls.setRightAnalogX(0);
        }
    };

    private JoystickMovedListener _listenerLeft = new JoystickMovedListener() {

        @Override
        public void OnMoved(int pan, int tilt) {
            mControls.setLeftAnalogY((float) tilt / resolution);
            mControls.setLeftAnalogX((float) pan / resolution);

            updateFlightData();
        }

        @Override
        public void OnReleased() {
            mControls.setLeftAnalogY(0);
            mControls.setLeftAnalogX(0);
        }

        public void OnReturnedToCenter() {
            mControls.setLeftAnalogY(0);
            mControls.setLeftAnalogX(0);
        }
    };

}
