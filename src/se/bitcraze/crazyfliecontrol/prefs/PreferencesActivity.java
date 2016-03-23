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

package se.bitcraze.crazyfliecontrol.prefs;

import java.io.IOException;

import se.bitcraze.crazyflie.lib.crazyradio.ConnectionData;
import se.bitcraze.crazyflie.lib.crazyradio.Crazyradio;
import se.bitcraze.crazyfliecontrol.prefs.SelectConnectionDialogFragment.SelectCrazyflieDialogListener;
import se.bitcraze.crazyfliecontrol2.R;
import se.bitcraze.crazyfliecontrol2.UsbLinkAndroid;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

public class PreferencesActivity extends PreferenceActivity {

    private static final String LOG_TAG = "PreferencesActivity";

    public static final String KEY_PREF_RADIO_CHANNEL = "pref_radiochannel";
    public static final String KEY_PREF_RADIO_DATARATE = "pref_radiodatarate";
    public static final String KEY_PREF_RADIO_SCAN = "pref_radio_scan";
    public static final String KEY_PREF_RADIO_STATS = "pref_radio_stats";
    public static final String KEY_PREF_BLATENCY_BOOL = "pref_blatency_bool";

    public static final String KEY_PREF_MODE = "pref_mode";
    public static final String KEY_PREF_DEADZONE = "pref_deadzone";
    public static final String KEY_PREF_ROLLTRIM = "pref_rolltrim";
    public static final String KEY_PREF_PITCHTRIM = "pref_pitchtrim";
    public static final String KEY_PREF_AFC_BOOL = "pref_afc_bool";
    public static final String KEY_PREF_AFC_SCREEN = "pref_afc_screen";
    public static final String KEY_PREF_MAX_ROLLPITCH_ANGLE = "pref_maxrollpitchangle";
    public static final String KEY_PREF_MAX_YAW_ANGLE = "pref_maxyawangle";
    public static final String KEY_PREF_MAX_THRUST = "pref_maxthrust";
    public static final String KEY_PREF_MIN_THRUST = "pref_minthrust";
    public static final String KEY_PREF_XMODE = "pref_xmode";
    public static final String KEY_PREF_RESET_AFC = "pref_reset_afc";

    public static final String KEY_PREF_CONTROLLER = "pref_controller";
    public static final String KEY_PREF_USE_GYRO_BOOL = "pref_use_gyro_bool";
    public static final String KEY_PREF_GYRO_AMP = "pref_gyro_amp";
    public static final String KEY_PREF_BTN_SCREEN = "pref_btn_screen";
    public static final String KEY_PREF_TOUCH_THRUST_FULL_TRAVEL = "pref_touch_thrust_full_travel";
    public static final String KEY_PREF_RIGHT_ANALOG_X_AXIS = "pref_right_analog_x_axis";
    public static final String KEY_PREF_RIGHT_ANALOG_Y_AXIS = "pref_right_analog_y_axis";
    public static final String KEY_PREF_LEFT_ANALOG_X_AXIS = "pref_left_analog_x_axis";
    public static final String KEY_PREF_LEFT_ANALOG_Y_AXIS = "pref_left_analog_y_axis";
    public static final String KEY_PREF_SPLITAXIS_YAW_BOOL = "pref_splitaxis_yaw_bool";
    public static final String KEY_PREF_SPLITAXIS_YAW_LEFT_AXIS = "pref_splitaxis_yaw_left_axis";
    public static final String KEY_PREF_SPLITAXIS_YAW_RIGHT_AXIS = "pref_splitaxis_yaw_right_axis";
    public static final String KEY_PREF_EMERGENCY_BTN = "pref_emergency_btn";
    public static final String KEY_PREF_ROLLTRIM_PLUS_BTN = "pref_rolltrim_plus_btn";
    public static final String KEY_PREF_ROLLTRIM_MINUS_BTN = "pref_rolltrim_minus_btn";
    public static final String KEY_PREF_PITCHTRIM_PLUS_BTN = "pref_pitchtrim_plus_btn";
    public static final String KEY_PREF_PITCHTRIM_MINUS_BTN = "pref_pitchtrim_minus_btn";
    public static final String KEY_PREF_RESET_BTN = "pref_reset_btn";
    public static final String KEY_PREF_ALT1_BTN = "pref_alt1_btn";
    public static final String KEY_PREF_ALT1_ACTION = "pref_alt1_action";
    public static final String KEY_PREF_ALT2_BTN = "pref_alt2_btn";
    public static final String KEY_PREF_ALT2_ACTION = "pref_alt2_action";
    public static final String KEY_PREF_HOVER_BTN = "pref_hover_btn";

    public static final String KEY_PREF_JOYSTICK_SIZE = "pref_touch_slider_size";

    public static final String KEY_PREF_SCREEN_ROTATION_LOCK_BOOL = "pref_screen_rotation_lock_bool";
    public static final String KEY_PREF_IMMERSIVE_MODE_BOOL = "pref_immersive_mode_bool";

    private static final int RADIOCHANNEL_UPPER_LIMIT = 125;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction().replace(android.R.id.content, new PreferencesFragment()).commit();

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        setupActionBar();
    }

    public static class PreferencesFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
        private SharedPreferences mSharedPreferences;

        private String mRadioChannelDefaultValue;
        private String mDeadzoneDefaultValue;
        private String mMaxRollPitchAngleDefaultValue;
        private String mMaxYawAngleDefaultValue;
        private String mMaxThrustDefaultValue;
        private String mMinThrustDefaultValue;

        private String mGyroAmpDefaultValue;

        private String mJoystickSizeDefaultValue;

        private String mRightAnalogXAxisDefaultValue;
        private String mRightAnalogYAxisDefaultValue;
        private String mLeftAnalogXAxisDefaultValue;
        private String mLeftAnalogYAxisDefaultValue;
        private String mSplitAxisLeftAxisDefaultValue;
        private String mSplitAxisRightAxisDefaultValue;
        private String mEmergencyBtnDefaultValue;
        private String mRollTrimPlusBtnDefaultValue;
        private String mRollTrimMinusBtnDefaultValue;
        private String mPitchTrimPlusBtnDefaultValue;
        private String mPitchTrimMinusBtnDefaultValue;
        private String mAlt1BtnDefaultValue;
        private String mAlt2BtnDefaultValue;
        private String mHoverBtnDefaultValue;

        private String[] mDatarateStrings;

        private boolean mNoGyroSensor = false;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Make sure default values are applied.  In a real app, you would
            // want this in a shared function that is used to retrieve the
            // SharedPreferences wherever they are needed.
//            PreferenceManager.setDefaultValues(getActivity(), R.xml.advanced_preferences, false);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);

            mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            setInitialSummaries();

            mDatarateStrings = getResources().getStringArray(R.array.radioDatarateEntries);
        }

        /**
         * Set initial summaries and get default values
         */
        private void setInitialSummaries() {
            // Connection settings
            mRadioChannelDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_RADIO_CHANNEL, R.string.preferences_radio_channel_defaultValue);
            setSummaryArray(KEY_PREF_RADIO_DATARATE, R.string.preferences_radio_datarate_defaultValue, R.array.radioDatarateEntries, 0);
            findPreference(KEY_PREF_RADIO_SCAN).setOnPreferenceClickListener(new OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference preference) {
                    radioScan();
                    return true;
                }
            });

            //Radio stats
            setRadioStats();
            findPreference(KEY_PREF_RADIO_STATS).setOnPreferenceClickListener(new OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference preference) {
                    setRadioStats();
                    return true;
                }
            });

            // Flight control settings
            setSummaryArray(KEY_PREF_MODE, R.string.preferences_mode_defaultValue, R.array.modeEntries, -1);

            mDeadzoneDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_DEADZONE, R.string.preferences_deadzone_defaultValue);
            setInitialSummaryAndReturnDefaultValue(KEY_PREF_PITCHTRIM, R.string.preferences_trim_defaultValue);

            // Controller settings
            setSummaryArray(KEY_PREF_CONTROLLER, R.string.preferences_controller_defaultValue, R.array.controllerEntries, 0);
            setControllerSpecificPreferences();
            mGyroAmpDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_GYRO_AMP, R.string.preferences_gyro_amp_defaultValue);
            mJoystickSizeDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_JOYSTICK_SIZE, R.string.preferences_joystick_size_defaultValue);

            // Gamepad and button mapping
            mRightAnalogXAxisDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_RIGHT_ANALOG_X_AXIS, R.string.preferences_right_analog_x_axis_defaultValue);
            mRightAnalogYAxisDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_RIGHT_ANALOG_Y_AXIS, R.string.preferences_right_analog_y_axis_defaultValue);
            mLeftAnalogXAxisDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_LEFT_ANALOG_X_AXIS, R.string.preferences_left_analog_x_axis_defaultValue);
            mLeftAnalogYAxisDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_LEFT_ANALOG_Y_AXIS, R.string.preferences_left_analog_y_axis_defaultValue);

            findPreference(KEY_PREF_SPLITAXIS_YAW_LEFT_AXIS).setEnabled(mSharedPreferences.getBoolean(KEY_PREF_SPLITAXIS_YAW_BOOL, false));
            findPreference(KEY_PREF_SPLITAXIS_YAW_RIGHT_AXIS).setEnabled(mSharedPreferences.getBoolean(KEY_PREF_SPLITAXIS_YAW_BOOL, false));
            mSplitAxisLeftAxisDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_SPLITAXIS_YAW_LEFT_AXIS, R.string.preferences_splitaxis_yaw_left_axis_defaultValue);
            mSplitAxisRightAxisDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_SPLITAXIS_YAW_RIGHT_AXIS, R.string.preferences_splitaxis_yaw_right_axis_defaultValue);

            mEmergencyBtnDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_EMERGENCY_BTN, R.string.preferences_emergency_btn_defaultValue);
            mRollTrimPlusBtnDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_ROLLTRIM_PLUS_BTN , R.string.preferences_rolltrim_plus_btn_defaultValue);
            mRollTrimMinusBtnDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_ROLLTRIM_MINUS_BTN, R.string.preferences_rolltrim_minus_btn_defaultValue);
            mPitchTrimPlusBtnDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_PITCHTRIM_PLUS_BTN, R.string.preferences_pitchtrim_plus_btn_defaultValue);
            mPitchTrimMinusBtnDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_PITCHTRIM_MINUS_BTN, R.string.preferences_pitchtrim_minus_btn_defaultValue);
            mAlt1BtnDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_ALT1_BTN, R.string.preferences_alt1_btn_defaultValue);
            setSummaryArrayString(KEY_PREF_ALT1_ACTION, R.string.preferences_alt1_action_defaultValue, R.array.actionEntries, R.array.actionValues);
            mAlt2BtnDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_ALT2_BTN, R.string.preferences_alt2_btn_defaultValue);
            setSummaryArrayString(KEY_PREF_ALT2_ACTION, R.string.preferences_alt2_action_defaultValue, R.array.actionEntries, R.array.actionValues);
            mHoverBtnDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_HOVER_BTN, R.string.preferences_hover_btn_defaultValue);

            findPreference(KEY_PREF_RESET_BTN).setOnPreferenceClickListener(new OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference preference) {
                    resetPreference(KEY_PREF_RIGHT_ANALOG_X_AXIS, mRightAnalogXAxisDefaultValue, null);
                    resetPreference(KEY_PREF_RIGHT_ANALOG_Y_AXIS, mRightAnalogYAxisDefaultValue, null);
                    resetPreference(KEY_PREF_LEFT_ANALOG_X_AXIS, mLeftAnalogXAxisDefaultValue, null);
                    resetPreference(KEY_PREF_LEFT_ANALOG_Y_AXIS, mLeftAnalogYAxisDefaultValue, null);
                    resetPreference(KEY_PREF_SPLITAXIS_YAW_BOOL, false);
                    resetPreference(KEY_PREF_SPLITAXIS_YAW_LEFT_AXIS, mSplitAxisLeftAxisDefaultValue, null);
                    resetPreference(KEY_PREF_SPLITAXIS_YAW_RIGHT_AXIS, mSplitAxisRightAxisDefaultValue, null);

                    resetPreference(KEY_PREF_EMERGENCY_BTN, mEmergencyBtnDefaultValue, null);
                    resetPreference(KEY_PREF_ROLLTRIM_PLUS_BTN, mRollTrimPlusBtnDefaultValue, null);
                    resetPreference(KEY_PREF_ROLLTRIM_MINUS_BTN, mRollTrimMinusBtnDefaultValue, null);
                    resetPreference(KEY_PREF_PITCHTRIM_PLUS_BTN, mPitchTrimPlusBtnDefaultValue, null);
                    resetPreference(KEY_PREF_PITCHTRIM_MINUS_BTN, mPitchTrimMinusBtnDefaultValue, null);
                    resetPreference(KEY_PREF_ALT1_BTN, mAlt1BtnDefaultValue, null);
                    resetPreference(KEY_PREF_ALT2_BTN, mAlt2BtnDefaultValue, null);
                    resetPreference(KEY_PREF_HOVER_BTN, mHoverBtnDefaultValue, null);
                    Toast.makeText(getActivity(), "Resetting to default values...", Toast.LENGTH_SHORT).show();
                    return true;
                }
            });

            // Advanced flight control settings
            findPreference(KEY_PREF_AFC_SCREEN).setEnabled(mSharedPreferences.getBoolean(KEY_PREF_AFC_BOOL, false));

            mMaxRollPitchAngleDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_MAX_ROLLPITCH_ANGLE, R.string.preferences_maxRollPitchAngle_defaultValue);
            mMaxYawAngleDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_MAX_YAW_ANGLE, R.string.preferences_maxYawAngle_defaultValue);
            mMaxThrustDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_MAX_THRUST, R.string.preferences_maxThrust_defaultValue);
            mMinThrustDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_MIN_THRUST, R.string.preferences_minThrust_defaultValue);

            findPreference(KEY_PREF_RESET_AFC).setOnPreferenceClickListener(new OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference preference) {
                    resetPreference(KEY_PREF_MAX_ROLLPITCH_ANGLE, mMaxRollPitchAngleDefaultValue, null);
                    resetPreference(KEY_PREF_MAX_YAW_ANGLE, mMaxYawAngleDefaultValue, null);
                    resetPreference(KEY_PREF_MAX_THRUST, mMaxThrustDefaultValue, null);
                    resetPreference(KEY_PREF_MIN_THRUST, mMinThrustDefaultValue, null);
                    resetPreference(KEY_PREF_XMODE, false);
                    return true;
                }
            });
        }

        private void checkGyroSensors() {
            //Test the available sensors
            SensorManager sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
            CheckBoxPreference pref = (CheckBoxPreference) findPreference(KEY_PREF_USE_GYRO_BOOL);

            if (sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) == null && sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) == null) {
                pref.setEnabled(false);
                pref.setChecked(false);
                resetPreference(KEY_PREF_USE_GYRO_BOOL, false);
                mNoGyroSensor  = true;
                pref.setSummaryOff("No gyro or accelerometer sensors found");
                Log.i(LOG_TAG, "No gyro or accelerometer sensors found");
            } else {
                pref.setEnabled(true);
                mNoGyroSensor = false;
            }
        }

        private void setRadioStats() {
            Preference prefRadioStats = findPreference(KEY_PREF_RADIO_STATS);
            String defaultValue = getResources().getString(R.string.preferences_radio_stats_summary);

            UsbLinkAndroid usbLinkAndroid = null;

            try {
                usbLinkAndroid = new UsbLinkAndroid(getActivity());
                if (usbLinkAndroid != null) {
                    usbLinkAndroid.initDevice(Crazyradio.CRADIO_VID, Crazyradio.CRADIO_PID);
                    if(usbLinkAndroid.isUsbConnected()) {
                        prefRadioStats.setSummary("Firmware version: " + usbLinkAndroid.getFirmwareVersion() + "\n" +
                               					  "Serial number: " + usbLinkAndroid.getSerialNumber());
                        enableDisableRadioSettings(true);
                    } else{
                        prefRadioStats.setSummary(defaultValue);
                        enableDisableRadioSettings(false);
                    }
                } else {
                    prefRadioStats.setSummary(defaultValue);
                    enableDisableRadioSettings(false);
                }
            } catch (IllegalArgumentException e) {
                Log.d(LOG_TAG, e.getMessage());
                prefRadioStats.setSummary(defaultValue);
                enableDisableRadioSettings(false);
            } catch (IOException iae) {
                Log.e(LOG_TAG, iae.getMessage());
                prefRadioStats.setSummary(defaultValue);
                enableDisableRadioSettings(false);
            } finally {
                if (usbLinkAndroid != null) {
                    usbLinkAndroid.releaseInterface();
                }
            }
        }

        private void enableDisableRadioSettings(boolean enable) {
            findPreference(KEY_PREF_RADIO_CHANNEL).setEnabled(enable);
            findPreference(KEY_PREF_RADIO_DATARATE).setEnabled(enable);
            findPreference(KEY_PREF_RADIO_SCAN).setEnabled(enable);
            findPreference(KEY_PREF_RADIO_STATS).setEnabled(enable);
        }

        // Set summary to be the user-description for the selected value
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            // Connection settings
            if (key.equals(KEY_PREF_RADIO_CHANNEL)) {
                setSummaryInt(key, mRadioChannelDefaultValue, RADIOCHANNEL_UPPER_LIMIT, "Radio channel");
            }
            if (key.equals(KEY_PREF_RADIO_DATARATE)) {
                setSummaryArray(key, R.string.preferences_radio_datarate_defaultValue, R.array.radioDatarateEntries, 0);
            }

            // Flight control settings
            if (key.equals(KEY_PREF_MODE)) {
                setSummaryArray(key, R.string.preferences_mode_defaultValue, R.array.modeEntries, -1);
            }
            if (key.equals(KEY_PREF_DEADZONE)) {
                findPreference(key).setSummary(sharedPreferences.getString(key, mDeadzoneDefaultValue));
            }

            // Controller settings
            if (key.equals(KEY_PREF_CONTROLLER)) {
                setSummaryArray(key, R.string.preferences_controller_defaultValue, R.array.controllerEntries, 0);
                setControllerSpecificPreferences();
            }

            if (key.equals(KEY_PREF_USE_GYRO_BOOL)) {
                CheckBoxPreference pref = (CheckBoxPreference) findPreference(key);
                boolean useGyro = sharedPreferences.getBoolean(key, false);
                pref.setChecked(useGyro);
                // automatically activate the screen rotation lock preference
                CheckBoxPreference screenRotationLock = (CheckBoxPreference) findPreference(KEY_PREF_SCREEN_ROTATION_LOCK_BOOL);
                if (useGyro) {
                    Toast.makeText(getActivity(), "Activating screen rotation lock...", Toast.LENGTH_LONG).show();
                    screenRotationLock.setSummary("Locked because gyroscope is used as controller.");
                } else {
                    Toast.makeText(getActivity(), "Deactivating screen rotation lock...", Toast.LENGTH_LONG).show();
                    screenRotationLock.setSummary("");
                }
                screenRotationLock.setChecked(useGyro);
                screenRotationLock.setEnabled(!useGyro);

                SliderPreference gyroAmp = (SliderPreference) findPreference(KEY_PREF_GYRO_AMP);
                gyroAmp.setEnabled(useGyro);
            }

            if (key.equals(KEY_PREF_GYRO_AMP)) {
                findPreference(key).setSummary(sharedPreferences.getString(key, mGyroAmpDefaultValue));
            }

            if(key.equals(KEY_PREF_JOYSTICK_SIZE)){
                findPreference(key).setSummary(sharedPreferences.getString(key,mJoystickSizeDefaultValue));
            }

            // Gamepad and button mapping
            if (key.equals(KEY_PREF_RIGHT_ANALOG_X_AXIS)){
                findPreference(key).setSummary(sharedPreferences.getString(key, mRightAnalogXAxisDefaultValue));
            }
            if (key.equals(KEY_PREF_RIGHT_ANALOG_Y_AXIS)){
                findPreference(key).setSummary(sharedPreferences.getString(key, mRightAnalogYAxisDefaultValue));
            }
            if (key.equals(KEY_PREF_LEFT_ANALOG_X_AXIS)){
                findPreference(key).setSummary(sharedPreferences.getString(key, mLeftAnalogXAxisDefaultValue));
            }
            if (key.equals(KEY_PREF_LEFT_ANALOG_Y_AXIS)){
                findPreference(key).setSummary(sharedPreferences.getString(key, mLeftAnalogYAxisDefaultValue));
            }

            if (key.equals(KEY_PREF_SPLITAXIS_YAW_BOOL)) {
                CheckBoxPreference pref = (CheckBoxPreference) findPreference(key);
                pref.setChecked(sharedPreferences.getBoolean(key, false));
                findPreference(KEY_PREF_SPLITAXIS_YAW_LEFT_AXIS).setEnabled(sharedPreferences.getBoolean(key, false));
                findPreference(KEY_PREF_SPLITAXIS_YAW_RIGHT_AXIS).setEnabled(sharedPreferences.getBoolean(key, false));
            }

            if (key.equals(KEY_PREF_SPLITAXIS_YAW_LEFT_AXIS)){
                findPreference(key).setSummary(sharedPreferences.getString(key, mSplitAxisLeftAxisDefaultValue));
            }
            if (key.equals(KEY_PREF_SPLITAXIS_YAW_RIGHT_AXIS)){
                findPreference(key).setSummary(sharedPreferences.getString(key, mSplitAxisRightAxisDefaultValue));
            }
            if (key.equals(KEY_PREF_EMERGENCY_BTN)) {
                findPreference(key).setSummary(sharedPreferences.getString(key, mEmergencyBtnDefaultValue));
            }
            if (key.equals(KEY_PREF_ROLLTRIM_PLUS_BTN)) {
                findPreference(key).setSummary(sharedPreferences.getString(key, mRollTrimPlusBtnDefaultValue));
            }
            if (key.equals(KEY_PREF_ROLLTRIM_MINUS_BTN)) {
                findPreference(key).setSummary(sharedPreferences.getString(key, mRollTrimMinusBtnDefaultValue));
            }
            if (key.equals(KEY_PREF_PITCHTRIM_PLUS_BTN)) {
                findPreference(key).setSummary(sharedPreferences.getString(key, mPitchTrimPlusBtnDefaultValue));
            }
            if (key.equals(KEY_PREF_PITCHTRIM_MINUS_BTN)) {
                findPreference(key).setSummary(sharedPreferences.getString(key, mPitchTrimMinusBtnDefaultValue));
            }
            if (key.equals(KEY_PREF_ALT1_BTN)) {
                findPreference(key).setSummary(sharedPreferences.getString(key, mAlt1BtnDefaultValue));
            }
            if (key.equals(KEY_PREF_ALT2_BTN)) {
                findPreference(key).setSummary(sharedPreferences.getString(key, mAlt2BtnDefaultValue));
            }
            if (key.equals(KEY_PREF_HOVER_BTN)) {
                findPreference(key).setSummary(sharedPreferences.getString(key, mHoverBtnDefaultValue));
            }

            if (key.equals(KEY_PREF_ALT1_ACTION)) {
                setSummaryArrayString(key, R.string.preferences_alt1_action_defaultValue, R.array.actionEntries, R.array.actionValues);
            }
            if (key.equals(KEY_PREF_ALT2_ACTION)) {
                setSummaryArrayString(key, R.string.preferences_alt2_action_defaultValue, R.array.actionEntries, R.array.actionValues);
            }

            // Advanced flight control settings
            if (key.equals(KEY_PREF_AFC_BOOL)) {
                Preference afcScreenPref = findPreference(KEY_PREF_AFC_SCREEN);
                afcScreenPref.setEnabled(sharedPreferences.getBoolean(key, false));
                if (!sharedPreferences.getBoolean(key, false)) {
                    Toast.makeText(getActivity(), "Resetting to default values:\n"
                                        + "Max roll/pitch angle: " + mMaxRollPitchAngleDefaultValue + "\n"
                                        + "Max yaw angle: " + mMaxYawAngleDefaultValue + "\n"
                                        + "Max thrust: " + mMaxThrustDefaultValue + "\n"
                                        + "Min thrust: " + mMinThrustDefaultValue, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getActivity(), "You have been warned!", Toast.LENGTH_LONG).show();
                }
            }
            if (key.equals(KEY_PREF_MAX_ROLLPITCH_ANGLE)) {
                findPreference(key).setSummary(sharedPreferences.getString(key,mMaxRollPitchAngleDefaultValue));
            }
            if (key.equals(KEY_PREF_MAX_YAW_ANGLE)) {
                findPreference(key).setSummary(sharedPreferences.getString(key,mMaxYawAngleDefaultValue));
            }
            if (key.equals(KEY_PREF_MAX_THRUST)) {
                findPreference(key).setSummary(sharedPreferences.getString(key,mMaxThrustDefaultValue));
            }
            if (key.equals(KEY_PREF_MIN_THRUST)) {
                findPreference(key).setSummary(sharedPreferences.getString(key,mMinThrustDefaultValue));
            }
            if (key.equals(KEY_PREF_XMODE)) {
                CheckBoxPreference pref = (CheckBoxPreference) findPreference(key);
                pref.setChecked(sharedPreferences.getBoolean(key, false));
            }

            // App settings
            if (key.equals(KEY_PREF_SCREEN_ROTATION_LOCK_BOOL)) {
                CheckBoxPreference pref = (CheckBoxPreference) findPreference(key);
                pref.setChecked(sharedPreferences.getBoolean(key, false));
            }
            if (key.equals(KEY_PREF_IMMERSIVE_MODE_BOOL)) {
                CheckBoxPreference pref = (CheckBoxPreference) findPreference(key);
                pref.setChecked(sharedPreferences.getBoolean(key, false));
            }
        }

        private void setControllerSpecificPreferences() {
            String controllerDefaultValue = getResources().getString(R.string.preferences_controller_defaultValue);
            int controllerIndex = Integer.parseInt(mSharedPreferences.getString(KEY_PREF_CONTROLLER, controllerDefaultValue));
            boolean useGyro = mSharedPreferences.getBoolean(KEY_PREF_USE_GYRO_BOOL, false);
            if (!mNoGyroSensor) {
                findPreference(KEY_PREF_USE_GYRO_BOOL).setEnabled(controllerIndex == 0);
                findPreference(KEY_PREF_GYRO_AMP).setEnabled(controllerIndex == 0 && useGyro);
            }
            findPreference(KEY_PREF_BTN_SCREEN).setEnabled(controllerIndex == 1);
            findPreference(KEY_PREF_TOUCH_THRUST_FULL_TRAVEL).setEnabled(controllerIndex == 0);
            findPreference(KEY_PREF_JOYSTICK_SIZE).setEnabled(controllerIndex == 0);
        }

        private String setInitialSummaryAndReturnDefaultValue(String pKey, int pRDefaultValue) {
            Preference pref = findPreference(pKey);
            String defaultValue = getResources().getString(pRDefaultValue);
            pref.setSummary(mSharedPreferences.getString(pKey, defaultValue));
            return defaultValue;
        }

        private void setSummaryInt(String key, String pDefaultValue, int pUpperLimit, String pValueName) {
            Preference pref = findPreference(key);
            try {
                int newPrefValue = Integer.parseInt(mSharedPreferences.getString(key, pDefaultValue));
                if (newPrefValue < 0 || newPrefValue > pUpperLimit) {
                    resetPreference(key, pDefaultValue, pValueName + " must be an integer value between 0 and " + pUpperLimit + ".");
                }
            } catch (NumberFormatException nfe) {
                resetPreference(key, pDefaultValue, pValueName + " must be an integer value between 0 and " + pUpperLimit + ".");
            }
            pref.setSummary(mSharedPreferences.getString(key, ""));
        }

        private void setSummaryArray(String key, int pRDefaultValue, int pRArray, int arrayOffset){
            Preference pref = findPreference(key);
            String preDefaultValue = getResources().getString(pRDefaultValue);
            String[] stringArray = getResources().getStringArray(pRArray);
            String keyString = mSharedPreferences.getString(key, preDefaultValue);
            pref.setSummary(stringArray[Integer.parseInt(keyString) + arrayOffset]);
        }

        private void setSummaryArrayString(String key, int pRDefaultValue, int pRArrayEntries, int pRArrayValues){
            Preference pref = findPreference(key);
            String preDefaultValue = getResources().getString(pRDefaultValue);
            String[] stringArrayEntries = getResources().getStringArray(pRArrayEntries);
            String[] stringArrayValues = getResources().getStringArray(pRArrayValues);
            String keyString = mSharedPreferences.getString(key, preDefaultValue);
            for(int i = 0; i < stringArrayValues.length; i++) {
                if (keyString.equals(stringArrayValues[i])) {
                    pref.setSummary(stringArrayEntries[i]);
                }
            }
        }

        private void resetPreference(String pKey, String pDefaultValue, String pErrorMessage) {
            if (pErrorMessage != null) {
                Toast.makeText(getActivity(), pErrorMessage + "\nResetting to default value " + pDefaultValue + ".", Toast.LENGTH_SHORT).show();
            }
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putString(pKey, pDefaultValue);
            editor.apply();
        }

        private void resetPreference(String pKey, boolean pDefaultValue) {
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putBoolean(pKey, pDefaultValue);
            editor.apply();
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            checkGyroSensors();
        }

        @Override
        public void onPause() {
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }

        private void radioScan() {
            new AsyncTask<Void, Void, ConnectionData[]>() {

                private Exception mException = null;
                private ProgressDialog mProgress;

                @Override
                protected void onPreExecute() {
                    mProgress = ProgressDialog.show(getActivity(), "Radio Scan", "Searching for the Crazyflie...", true, false);
                }

                @Override
                protected ConnectionData[] doInBackground(Void... arg0) {
                    UsbLinkAndroid usbLinkAndroid;
                    Crazyradio crlink = null;
                    try {
                        usbLinkAndroid = new UsbLinkAndroid(getActivity());
                        crlink = new Crazyradio(usbLinkAndroid);
                        if(!usbLinkAndroid.isUsbConnected()) {
                            throw new IllegalArgumentException("Scanning is only supported with Crazyradio.");
                        }
                        //For testing purposes only
//                        return new ConnectionData[0];
                        boolean useSlowScan = false;
                        //Use slow scan, when Crazyradio firmware version is 0.52 or 0.53
                        float firmwareVersion = usbLinkAndroid.getFirmwareVersion();
                        if(0.52f == firmwareVersion || 0.53f == firmwareVersion){
                            useSlowScan = true;
                        }
                        return crlink.scanChannels(useSlowScan);
                    } catch(IOException ioe) {
                        mException = ioe;
                        return new ConnectionData[0];
                    } catch(IllegalArgumentException iae) {
                        mException = iae;
                        return new ConnectionData[0];
                    } finally {
                        if (crlink != null) {
                            crlink.disconnect();
                        }
                    }
                }

                @Override
                protected void onPostExecute(ConnectionData[] result) {
                    mProgress.dismiss();

                    if(mException != null) {
                        Toast.makeText(getActivity(), mException.getMessage(), Toast.LENGTH_SHORT).show();
                    } else {
                        //TEST DATA for debugging SelectionConnectionDialogFragment (replace with test!)
//                      result = new ConnectionData[3];
//                      result[0] = new ConnectionData(13, 2);
//                      result[1] = new ConnectionData(15, 1);
//                      result[2] = new ConnectionData(125, 2);

                        if (result != null && result.length > 0) {
                            if(result.length > 1){
                                // let user choose connection, if there is more than one Crazyflie
                                showSelectConnectionDialog(result);
                            }else{
                                // use first channel
                                setRadioChannelAndDatarate(result[0].getChannel(), result[0].getDataRate());
                            }
                        } else {
                            Toast.makeText(getActivity(), "No connection found", Toast.LENGTH_SHORT).show();
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

        private void setRadioChannelAndDatarate(int channel, int datarate) {
            if (channel != -1 && datarate != -1) {
                SharedPreferences.Editor editor = mSharedPreferences.edit();
                editor.putString(PreferencesActivity.KEY_PREF_RADIO_CHANNEL, String.valueOf(channel));
                editor.putString(PreferencesActivity.KEY_PREF_RADIO_DATARATE, String.valueOf(datarate));
                editor.apply();

                Toast.makeText(getActivity(),"Channel: " + channel + " Data rate: " + mDatarateStrings[datarate] + "\nSetting preferences...", Toast.LENGTH_SHORT).show();
            }
        }

    }

    /**
     * Set default joystick size according to screen size
     *
     * @param context
     */
    public static void setDefaultJoystickSize(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        // If no default value exists, set new default value according to screen size
        if(!sharedPreferences.contains(KEY_PREF_JOYSTICK_SIZE)) {
            String defaultValue = getDefaultJoystickSize(context);
            Log.d(LOG_TAG, "Prefs do NOT contain joystick size. Setting new default: " + defaultValue);

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(PreferencesActivity.KEY_PREF_JOYSTICK_SIZE, String.valueOf(defaultValue));
            editor.apply();
        } else {
            Log.d(LOG_TAG, "Prefs already contain joystick size.");
        }
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            final ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static String getDefaultJoystickSize(Context context) {
        long screenSizeInInches = getScreenSizeInInches(context);
        int size = 100;
        if (screenSizeInInches >= 4) {
            size = 80;
        }
        if (screenSizeInInches >= 7) {
            size = 50;
        }
        if (screenSizeInInches >= 9) {
            size = 40;
        }
        if (screenSizeInInches >= 10) {
            size = 30;
        }
        return String.valueOf(size);
    }

    private static long getScreenSizeInInches(Context context) {
        DisplayMetrics dm = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(dm);
        int dens = dm.densityDpi;
        double wi = (double) dm.widthPixels / (double) dens;
        double hi = (double) dm.heightPixels / (double) dens;
        double x = Math.pow(wi, 2);
        double y = Math.pow(hi, 2);
        return Math.round(Math.sqrt(x + y));
    }

}
