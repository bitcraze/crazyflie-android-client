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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
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

    private float right_analog_x;
    private float right_analog_y;
    private float left_analog_x;
    private float left_analog_y;

    private Link crazyflieLink;
    public int resolution = 1000;
    private float mMaxTrim = 0.5f;
    private float mTrimIncrements = 0.1f; 

    SharedPreferences mPreferences;

    private int mode;
    public float deadzone;
    private int maxRollPitchAngle;
    private int maxYawAngle;
    private int maxThrust;
    private int minThrust;
    private boolean xmode;
    private float mRollTrim;
    private float mPitchTrim;

    private int emergencyBtn;
    private int rollTrimPlusBtn;
    private int rollTrimMinusBtn;
    private int pitchTrimPlusBtn;
    private int pitchTrimMinusBtn;
    
    private String radioChannelDefaultValue;
    private String radioBandwidthDefaultValue;
    private String modeDefaultValue;
    private String deadzoneDefaultValue;
    private String trimDefaultValue;
    private String emergencyBtnDefaultValue;
    private String rollTrimPlusBtnDefaultValue;
    private String rollTrimMinusBtnDefaultValue;
    private String pitchTrimPlusBtnDefaultValue;
    private String pitchTrimMinusBtnDefaultValue;
    private String maxRollPitchAngleDefaultValue;
    private String maxYawAngleDefaultValue;
    private String maxThrustDefaultValue;
    private String minThrustDefaultValue;

    private UsbManager mUsbManager;
    private UsbDevice mDevice;
    private PendingIntent mPermissionIntent;

    private boolean isOnscreenControllerDisabled;
    private boolean mPermissionAsked = false;
    private boolean mDoubleBackToExitPressedOnce = false;

    private Thread mSendJoystickDataThread;

    private String[] mDatarateStrings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setDefaultPreferenceValues();

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

        radioChannelDefaultValue = getResources().getString(R.string.preferences_radio_channel_defaultValue);
        radioBandwidthDefaultValue = getResources().getString(R.string.preferences_radio_bandwidth_defaultValue);
        modeDefaultValue = getResources().getString(R.string.preferences_mode_defaultValue);
        deadzoneDefaultValue = getResources().getString(R.string.preferences_deadzone_defaultValue);
        trimDefaultValue = getResources().getString(R.string.preferences_trim_defaultValue);
        emergencyBtnDefaultValue = getResources().getString(R.string.preferences_emergency_btn_defaultValue);
        rollTrimPlusBtnDefaultValue = getResources().getString(R.string.preferences_rolltrim_plus_btn_defaultValue);
        rollTrimMinusBtnDefaultValue = getResources().getString(R.string.preferences_rolltrim_minus_btn_defaultValue);
        pitchTrimPlusBtnDefaultValue = getResources().getString(R.string.preferences_pitchtrim_plus_btn_defaultValue);
        pitchTrimMinusBtnDefaultValue = getResources().getString(R.string.preferences_pitchtrim_minus_btn_defaultValue);
        maxRollPitchAngleDefaultValue = getResources().getString(R.string.preferences_maxRollPitchAngle_defaultValue);
        maxYawAngleDefaultValue = getResources().getString(R.string.preferences_maxYawAngle_defaultValue);
        maxThrustDefaultValue = getResources().getString(R.string.preferences_maxThrust_defaultValue);
        minThrustDefaultValue = getResources().getString(R.string.preferences_minThrust_defaultValue);

        mDatarateStrings = this.getResources().getStringArray(R.array.radioBandwidthEntries);
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
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        resetAxisValues();
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

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        // Check that the event came from a joystick since a generic motion
        // event
        // could be almost anything.
        if ((event.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) != 0
                && event.getAction() == MotionEvent.ACTION_MOVE) {

            Log.i(TAG, "Input device: " + event.getDevice().getName());

            if (!isOnscreenControllerDisabled) {
                disableOnscreenController();
            }

            // hardcoded to work with PS3 controller
            right_analog_x = (float) (event.getAxisValue(MotionEvent.AXIS_Z));
            right_analog_y = (float) (event.getAxisValue(MotionEvent.AXIS_RZ));
            left_analog_x = (float) (event.getAxisValue(MotionEvent.AXIS_X));
            left_analog_y = (float) (event.getAxisValue(MotionEvent.AXIS_Y));

            mFlightDataView.updateFlightData();
            return true;
        } else {
            return super.dispatchGenericMotionEvent(event);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // TODO: works for PS3 controller, but does it also work for other
        // controllers?
        // do not call super if key event comes from a gamepad, otherwise the
        // buttons can quit the app
        if (event.getSource() == 1281) {
            switch (event.getAction()) {
                case KeyEvent.ACTION_DOWN:
                    if(event.getKeyCode() == emergencyBtn){
                        //quick solution
                        resetAxisValues();
                        if (crazyflieLink != null) {
                            linkDisconnect();
                        }
                        Toast.makeText(this, "Emergency Stop", Toast.LENGTH_SHORT).show();
                    }else if (event.getKeyCode() == rollTrimPlusBtn) {
                        increaseTrim(PreferencesActivity.KEY_PREF_ROLLTRIM);
                    }else if (event.getKeyCode() == rollTrimMinusBtn) {
                        decreaseTrim(PreferencesActivity.KEY_PREF_ROLLTRIM);
                    }else if (event.getKeyCode() == pitchTrimPlusBtn) {
                        increaseTrim(PreferencesActivity.KEY_PREF_PITCHTRIM);
                    }else if (event.getKeyCode() == pitchTrimMinusBtn) {
                        decreaseTrim(PreferencesActivity.KEY_PREF_PITCHTRIM);
                    }
                    break;
                default:
                    break;
            }
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

    private void disableOnscreenController() {
        Toast.makeText(this, "Using external controller", Toast.LENGTH_SHORT).show();
        mJoysticks.setOnJostickMovedListener(null, null);
        this.isOnscreenControllerDisabled = true;
    }

    private void resetInputMethod() {
        Toast.makeText(this, "Using on-screen controller", Toast.LENGTH_SHORT).show();
        this.isOnscreenControllerDisabled = false;
        mJoysticks.setOnJostickMovedListener(_listenerLeft, _listenerRight);
    }

    private void setControlConfig() {
        this.mode = Integer.parseInt(mPreferences.getString(PreferencesActivity.KEY_PREF_MODE, modeDefaultValue));
        this.deadzone = Float.parseFloat(mPreferences.getString(PreferencesActivity.KEY_PREF_DEADZONE, deadzoneDefaultValue));
        this.mRollTrim = Float.parseFloat(mPreferences.getString(PreferencesActivity.KEY_PREF_ROLLTRIM, trimDefaultValue));
        this.mPitchTrim = Float.parseFloat(mPreferences.getString(PreferencesActivity.KEY_PREF_PITCHTRIM, trimDefaultValue));
        this.emergencyBtn = KeyEvent.keyCodeFromString(mPreferences.getString(PreferencesActivity.KEY_PREF_EMERGENCY_BTN, emergencyBtnDefaultValue));
        this.rollTrimPlusBtn = KeyEvent.keyCodeFromString(mPreferences.getString(PreferencesActivity.KEY_PREF_ROLLTRIM_PLUS_BTN, rollTrimPlusBtnDefaultValue));
        this.rollTrimMinusBtn = KeyEvent.keyCodeFromString(mPreferences.getString(PreferencesActivity.KEY_PREF_ROLLTRIM_MINUS_BTN, rollTrimMinusBtnDefaultValue));
        this.pitchTrimPlusBtn = KeyEvent.keyCodeFromString(mPreferences.getString(PreferencesActivity.KEY_PREF_PITCHTRIM_PLUS_BTN, pitchTrimPlusBtnDefaultValue));
        this.pitchTrimMinusBtn = KeyEvent.keyCodeFromString(mPreferences.getString(PreferencesActivity.KEY_PREF_PITCHTRIM_MINUS_BTN, pitchTrimMinusBtnDefaultValue));
        if (mPreferences.getBoolean(PreferencesActivity.KEY_PREF_AFC_BOOL, false)) {
            this.maxRollPitchAngle = Integer.parseInt(mPreferences.getString(PreferencesActivity.KEY_PREF_MAX_ROLLPITCH_ANGLE, maxRollPitchAngleDefaultValue));
            this.maxYawAngle = Integer.parseInt(mPreferences.getString(PreferencesActivity.KEY_PREF_MAX_YAW_ANGLE, maxYawAngleDefaultValue));
            this.maxThrust = Integer.parseInt(mPreferences.getString(PreferencesActivity.KEY_PREF_MAX_THRUST, maxThrustDefaultValue));
            this.minThrust = Integer.parseInt(mPreferences.getString(PreferencesActivity.KEY_PREF_MIN_THRUST, minThrustDefaultValue));
            this.xmode = mPreferences.getBoolean(PreferencesActivity.KEY_PREF_XMODE, false);
        } else {
            this.maxRollPitchAngle = Integer.parseInt(maxRollPitchAngleDefaultValue);
            this.maxYawAngle = Integer.parseInt(maxYawAngleDefaultValue);
            this.maxThrust = Integer.parseInt(maxThrustDefaultValue);
            this.minThrust = Integer.parseInt(minThrustDefaultValue);
            this.xmode = false;
        }
    }

    private void setPreference(String pKey, String pDefaultValue) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(pKey, pDefaultValue);
        editor.commit();
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
            if (e.getValue().getVendorId() == CrazyradioLink.VENDOR_ID &&
                e.getValue().getProductId() == CrazyradioLink.PRODUCT_ID) {
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
                if (device != null) {
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
                PreferencesActivity.KEY_PREF_RADIO_CHANNEL, radioChannelDefaultValue));
        int radioBandwidth = Integer.parseInt(mPreferences.getString(
                PreferencesActivity.KEY_PREF_RADIO_BANDWIDTH, radioBandwidthDefaultValue));

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

    private void linkDisconnect() {
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
        try {
            final ConnectionData[] result = CrazyradioLink.scanChannels(mUsbManager, mDevice);

            //TEST DATA for debugging SelectionConnectionDialogFragment (replace with test!)
//            final CrazyradioLink.ConnectionData[] result = new ConnectionData[3];
//            result[0] = new ConnectionData(13, 2);
//            result[1] = new ConnectionData(15, 1);
//            result[2] = new ConnectionData(125, 2);

            if (result != null && result.length > 0) {
                if(result.length > 1){
                    // let user choose connection, if there is more than one Crazyflie 
                    showSelectConnectionDialog(result);
                }else{
                    // use first channel
                    setRadioChannelAndDatarate(result[0].getChannel(), result[0].getDataRate());
                }
            } else {
                Toast.makeText(this, "No connection found", Toast.LENGTH_SHORT).show();
            }
        } catch (IllegalStateException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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
        float thrust = ((mode == 1 || mode == 3) ? getRightAnalog_Y() : getLeftAnalog_Y());
        thrust = thrust * -1; // invert
        if (thrust > deadzone) {
            return minThrust + (thrust * getThrustFactor());
        }
        return 0;
    }

    public float getRoll() {
        float roll = (mode == 1 || mode == 2) ? getRightAnalog_X() : getLeftAnalog_X();
        return (roll + getRollTrim()) * getRollPitchFactor() * getDeadzone(roll);
    }

    private float getRollTrim() {
        return mRollTrim;
    }
    
    private void increaseTrim(String prefKey){
        changeTrim(prefKey, true);
    }

    private void decreaseTrim(String prefKey){
        changeTrim(prefKey, false);
    }
    
    private void changeTrim(String prefKey, boolean increase){
        float axis;
        String axisName;
        if(PreferencesActivity.KEY_PREF_ROLLTRIM.equals(prefKey)){
            axisName = "Roll";
            axis = mRollTrim;
        }else{
            axisName = "Pitch";
            axis = mPitchTrim;
        }
        
        if(increase && axis < mMaxTrim){
            axis += mTrimIncrements;
        }else if(!increase && axis > (mMaxTrim * -1)){
           axis -= mTrimIncrements;
        }

        setPreference(prefKey, String.valueOf(axis));
        Toast.makeText(this, axisName + " Trim: " + FlightDataView.round(axis), Toast.LENGTH_SHORT).show();
        
        if(PreferencesActivity.KEY_PREF_ROLLTRIM.equals(prefKey)){
            mRollTrim = axis;
        }else{
            mPitchTrim = axis;
        }
    }

    public float getPitch() {
        float pitch = (mode == 1 || mode == 3) ? getLeftAnalog_Y() : getRightAnalog_Y();
        return (pitch + getPitchTrim()) * getRollPitchFactor() * getDeadzone(pitch);
    }

    private float getPitchTrim() {
        return mPitchTrim;
    }

    public float getYaw() {
        float yaw = (mode == 1 || mode == 2) ? getLeftAnalog_X() : getRightAnalog_X();
        return yaw * getYawFactor() * getDeadzone(yaw);
    }

    private float getDeadzone(float axis) {
        if (axis < deadzone && axis > deadzone * -1) {
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

    private void resetAxisValues() {
        right_analog_y = 0;
        right_analog_x = 0;
        left_analog_y = 0;
        left_analog_x = 0;
    }

    private JoystickMovedListener _listenerRight = new JoystickMovedListener() {

        @Override
        public void OnMoved(int pan, int tilt) {
            right_analog_y = (float) tilt / resolution;
            right_analog_x = (float) pan / resolution;

            mFlightDataView.updateFlightData();
        }

        @Override
        public void OnReleased() {
            // Log.i("Joystick-Right", "Release");
            right_analog_y = 0;
            right_analog_x = 0;
        }

        public void OnReturnedToCenter() {
            // Log.i("Joystick-Right", "Center");
            right_analog_y = 0;
            right_analog_x = 0;
        }
    };

    private JoystickMovedListener _listenerLeft = new JoystickMovedListener() {

        @Override
        public void OnMoved(int pan, int tilt) {
            left_analog_y = (float) tilt / resolution;
            left_analog_x = (float) pan / resolution;

            mFlightDataView.updateFlightData();
        }

        @Override
        public void OnReleased() {
            left_analog_y = 0;
            left_analog_x = 0;
        }

        public void OnReturnedToCenter() {
            left_analog_y = 0;
            left_analog_x = 0;
        }
    };

}
