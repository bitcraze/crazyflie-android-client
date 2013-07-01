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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map.Entry;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

import com.MobileAnarchy.Android.Widgets.Joystick.DualJoystickView;
import com.MobileAnarchy.Android.Widgets.Joystick.JoystickMovedListener;

public class MainActivity extends Activity {

    private static final String TAG = "CrazyflieControl";

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

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
    private int maxRollPitchAngle;
    private int maxYawAngle;
    private int maxThrust;
    private int minThrust;
    private boolean xmode;

    private String radioChannelDefaultValue;
    private String radioBandwidthDefaultValue;
    private String modeDefaultValue;
    private String deadzoneDefaultValue;
    private String maxRollPitchAngleDefaultValue;
    private String maxYawAngleDefaultValue;
    private String maxThrustDefaultValue;
    private String minThrustDefaultValue;

    private UsbManager mUsbManager;
    private UsbDevice device;
    private PendingIntent mPermissionIntent;

    private boolean isOnscreenControllerDisabled;

    private boolean mPermissionAsked = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set default preference values
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        // Initialize preferences
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        radioChannelDefaultValue = getResources().getString(R.string.preferences_radio_channel_defaultValue);
        radioBandwidthDefaultValue = getResources().getString(R.string.preferences_radio_bandwidth_defaultValue);
        modeDefaultValue = getResources().getString(R.string.preferences_mode_defaultValue);
        deadzoneDefaultValue = getResources().getString(R.string.preferences_deadzone_defaultValue);
        maxRollPitchAngleDefaultValue = getResources().getString(R.string.preferences_maxRollPitchAngle_defaultValue);
        maxYawAngleDefaultValue = getResources().getString(R.string.preferences_maxYawAngle_defaultValue);
        maxThrustDefaultValue = getResources().getString(R.string.preferences_maxThrust_defaultValue);
        minThrustDefaultValue = getResources().getString(R.string.preferences_minThrust_defaultValue);

        Log.v(TAG, "radiochannel: " + radioChannel);
        Log.v(TAG, "radiobandwidth: " + radioBandwidth);

        textView_pitch = (TextView) findViewById(R.id.pitch);
        textView_roll = (TextView) findViewById(R.id.roll);
        textView_thrust = (TextView) findViewById(R.id.thrust);
        textView_yaw = (TextView) findViewById(R.id.yaw);

        mJoysticks = (DualJoystickView) findViewById(R.id.joysticks);
        mJoysticks.setMovementRange(resolution, resolution);

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
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

        resetInputMethod();
        searchForCrazyRadio();
        setControlConfig();
        setRadioLink();
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
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mUsbReceiver);
        super.onDestroy();
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        // Check that the event came from a joystick since a generic motion event
        // could be almost anything.
        if ((event.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) != 0 && event.getAction() == MotionEvent.ACTION_MOVE) {

            Log.i(TAG, "Input device: " + event.getDevice().getName());

            if (!isOnscreenControllerDisabled) {
                disableOnscreenController();
            }

            // hardcoded to work with PS3 controller
            right_analog_x = (float) (event.getAxisValue(MotionEvent.AXIS_Z));
            right_analog_y = (float) (event.getAxisValue(MotionEvent.AXIS_RZ));
            left_analog_x = (float) (event.getAxisValue(MotionEvent.AXIS_X));
            left_analog_y = (float) (event.getAxisValue(MotionEvent.AXIS_Y));

            updateFlightData();
            return true;
        } else {
            return super.dispatchGenericMotionEvent(event);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // TODO: works for PS3 controller, but does it also work for other controllers?
        if (event.getSource() == 1281) {
            switch (event.getAction()) {
            case KeyEvent.ACTION_DOWN:
                // TODO: use keys
                // Toast.makeText(this, "Event.getSource(): " + event.getSource(), Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
            }
            // do not call super if key event comes from a gamepad, otherwise the buttons can quit the app
            return true;
        }
        return super.dispatchKeyEvent(event);
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
        this.mode = Integer.parseInt(preferences.getString(PreferencesActivity.KEY_PREF_MODE, modeDefaultValue));
        this.deadzone = Float.parseFloat(preferences.getString(PreferencesActivity.KEY_PREF_DEADZONE, deadzoneDefaultValue));
        if (preferences.getBoolean(PreferencesActivity.KEY_PREF_AFC_BOOL, false)) {
            this.maxRollPitchAngle = Integer.parseInt(preferences.getString(PreferencesActivity.KEY_PREF_MAX_ROLLPITCH_ANGLE, maxRollPitchAngleDefaultValue));
            this.maxYawAngle = Integer.parseInt(preferences.getString(PreferencesActivity.KEY_PREF_MAX_YAW_ANGLE, maxYawAngleDefaultValue));
            this.maxThrust = Integer.parseInt(preferences.getString(PreferencesActivity.KEY_PREF_MAX_THRUST, maxThrustDefaultValue));
            this.minThrust = Integer.parseInt(preferences.getString(PreferencesActivity.KEY_PREF_MIN_THRUST, minThrustDefaultValue));
            this.xmode = preferences.getBoolean(PreferencesActivity.KEY_PREF_XMODE, false);
        } else {
            this.maxRollPitchAngle = Integer.parseInt(maxRollPitchAngleDefaultValue);
            this.maxYawAngle = Integer.parseInt(maxYawAngleDefaultValue);
            this.maxThrust = Integer.parseInt(maxThrustDefaultValue);
            this.minThrust = Integer.parseInt(minThrustDefaultValue);
            this.xmode = false;
        }
    }

    /**
     * Iterate over all attached USB devices and look for CrazyRadio. If CrazyRadio is found, request permission.
     */
    private void searchForCrazyRadio() {
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        // Iterate over USB devices
        for (Entry<String, UsbDevice> e : deviceList.entrySet()) {
            Log.i(TAG, "String: " + e.getKey() + " " + e.getValue().getVendorId() + " " + e.getValue().getProductId());
            // CrazyRadio - Vendor ID: 6421, Product ID: 30583
            if (e.getValue().getVendorId() == 6421 && e.getValue().getProductId() == 30583) {
                device = deviceList.get(e.getKey());
            }
        }

        if (device != null && !this.mPermissionAsked) {
            mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
            mUsbManager.requestPermission(device, mPermissionIntent);
            mPermissionAsked = true;
        } else {
            Log.d(TAG, "device == null");
        }
    }

    private void setRadioLink() {
        if (radioLink == null) {
            radioLink = new RadioLink(this);
        }
        radioChannel = Integer.parseInt(preferences.getString(PreferencesActivity.KEY_PREF_RADIO_CHANNEL, radioChannelDefaultValue));
        radioBandwidth = Integer.parseInt(preferences.getString(PreferencesActivity.KEY_PREF_RADIO_BANDWIDTH, radioBandwidthDefaultValue));

        radioLink.setChannel(radioChannel);
        radioLink.setBandwidth(radioBandwidth);
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
                            Log.d(TAG, "setDevice");
                            radioLink.setDevice(mUsbManager, device);
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
                    if (radioLink.getDevice() != null && radioLink.getDevice().equals(device)) {
                        Log.d(TAG, "setDevice(null,null)");
                        radioLink.setDevice(null, null);
                        mPermissionAsked = false;
                    }
                }
            }
        }
    };

    public void updateFlightData() {
        textView_pitch.setText("Pitch: " + round(getPitch() * -1)); // inverse
        textView_roll.setText("Roll: " + round(getRoll()));
        textView_thrust.setText("Thrust (%): " + round(getThrust()));
        textView_yaw.setText("Yaw: " + round(getYaw()));
    }

    public static double round(double unrounded) {
        BigDecimal bd = new BigDecimal(unrounded);
        BigDecimal rounded = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
        return rounded.doubleValue();
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
        return roll * getRollPitchFactor() * getDeadzone(roll);
    }

    public float getPitch() {
        float pitch = (mode == 1 || mode == 3) ? getLeftAnalog_Y() : getRightAnalog_Y();
        return pitch * getRollPitchFactor() * getDeadzone(pitch);
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

            updateFlightData();
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

            updateFlightData();
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
