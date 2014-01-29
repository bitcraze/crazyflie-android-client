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

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.widget.Toast;

public class PreferencesActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
    public static final String KEY_PREF_RADIO_CHANNEL = "pref_radiochannel";
    public static final String KEY_PREF_RADIO_DATARATE = "pref_radiodatarate";
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
    public static final String KEY_PREF_SCREEN_ROTATION_LOCK_BOOL = "pref_screen_rotation_lock_bool";

    private static final int RADIOCHANNEL_UPPER_LIMIT = 125;
    private static final float DEADZONE_UPPER_LIMIT = 1.0f;
    private static final float TRIM_UPPER_LIMIT = 0.5f;
    private static final int MAX_ROLLPITCH_ANGLE_UPPER_LIMIT = 50;
    private static final int MAX_YAW_ANGLE_UPPER_LIMIT = 500;
    private static final int MAX_THRUST_UPPER_LIMIT = 100;
    private static final int MIN_THRUST_UPPER_LIMIT = 50;

    private SharedPreferences sharedPreferences;

    private String radioChannelDefaultValue;
    private String modeDefaultValue;
    private String deadzoneDefaultValue;
    private String trimDefaultValue;
    private String maxRollPitchAngleDefaultValue;
    private String maxYawAngleDefaultValue;
    private String maxThrustDefaultValue;
    private String minThrustDefaultValue;

    private String rightAnalogXAxisDefaultValue;
    private String rightAnalogYAxisDefaultValue;
    private String leftAnalogXAxisDefaultValue;
    private String leftAnalogYAxisDefaultValue;
    private String splitAxisLeftAxisDefaultValue;
    private String splitAxisRightAxisDefaultValue;
    private String emergencyBtnDefaultValue;
    private String rollTrimPlusBtnDefaultValue;
    private String rollTrimMinusBtnDefaultValue;
    private String pitchTrimPlusBtnDefaultValue;
    private String pitchTrimMinusBtnDefaultValue;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        setInitialSummaries();

        setupActionBar();
    }

    private void setInitialSummaries() {
        // Set initial summaries and get default values
        radioChannelDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_RADIO_CHANNEL, R.string.preferences_radio_channel_defaultValue);

        Preference radioDataratePref = findPreference(KEY_PREF_RADIO_DATARATE);
        String radioDatarateDefaultValue = getResources().getString(R.string.preferences_radio_datarate_defaultValue);
        String[] stringArray = getResources().getStringArray(R.array.radioDatarateEntries);
        String keyString = sharedPreferences.getString(KEY_PREF_RADIO_DATARATE, radioDatarateDefaultValue);
        radioDataratePref.setSummary(stringArray[Integer.parseInt(keyString)]);

        modeDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_MODE, R.string.preferences_mode_defaultValue);
        deadzoneDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_DEADZONE, R.string.preferences_deadzone_defaultValue);
        trimDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_ROLLTRIM, R.string.preferences_trim_defaultValue);
        setInitialSummaryAndReturnDefaultValue(KEY_PREF_PITCHTRIM, R.string.preferences_trim_defaultValue);

        rightAnalogXAxisDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_RIGHT_ANALOG_X_AXIS, R.string.preferences_right_analog_x_axis_defaultValue);
        rightAnalogYAxisDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_RIGHT_ANALOG_Y_AXIS, R.string.preferences_right_analog_y_axis_defaultValue);
        leftAnalogXAxisDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_LEFT_ANALOG_X_AXIS, R.string.preferences_left_analog_x_axis_defaultValue);
        leftAnalogYAxisDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_LEFT_ANALOG_Y_AXIS, R.string.preferences_left_analog_y_axis_defaultValue);

        findPreference(KEY_PREF_SPLITAXIS_YAW_LEFT_AXIS).setEnabled(sharedPreferences.getBoolean(KEY_PREF_SPLITAXIS_YAW_BOOL, false));
        findPreference(KEY_PREF_SPLITAXIS_YAW_RIGHT_AXIS).setEnabled(sharedPreferences.getBoolean(KEY_PREF_SPLITAXIS_YAW_BOOL, false));
        splitAxisLeftAxisDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_SPLITAXIS_YAW_LEFT_AXIS, R.string.preferences_splitaxis_yaw_left_axis_defaultValue);
        splitAxisRightAxisDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_SPLITAXIS_YAW_RIGHT_AXIS, R.string.preferences_splitaxis_yaw_right_axis_defaultValue);

        emergencyBtnDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_EMERGENCY_BTN, R.string.preferences_emergency_btn_defaultValue);
        rollTrimPlusBtnDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_ROLLTRIM_PLUS_BTN , R.string.preferences_rolltrim_plus_btn_defaultValue);
        rollTrimMinusBtnDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_ROLLTRIM_MINUS_BTN, R.string.preferences_rolltrim_minus_btn_defaultValue);
        pitchTrimPlusBtnDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_PITCHTRIM_PLUS_BTN, R.string.preferences_pitchtrim_plus_btn_defaultValue);
        pitchTrimMinusBtnDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_PITCHTRIM_MINUS_BTN, R.string.preferences_pitchtrim_minus_btn_defaultValue);
        
        findPreference(KEY_PREF_RESET_BTN).setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                resetPreference(KEY_PREF_RIGHT_ANALOG_X_AXIS, rightAnalogXAxisDefaultValue, null);
                resetPreference(KEY_PREF_RIGHT_ANALOG_Y_AXIS, rightAnalogYAxisDefaultValue, null);
                resetPreference(KEY_PREF_LEFT_ANALOG_X_AXIS, leftAnalogXAxisDefaultValue, null);
                resetPreference(KEY_PREF_LEFT_ANALOG_Y_AXIS, leftAnalogYAxisDefaultValue, null);
                resetPreference(KEY_PREF_SPLITAXIS_YAW_BOOL, false);
                resetPreference(KEY_PREF_SPLITAXIS_YAW_LEFT_AXIS, splitAxisLeftAxisDefaultValue, null);
                resetPreference(KEY_PREF_SPLITAXIS_YAW_RIGHT_AXIS, splitAxisRightAxisDefaultValue, null);

                resetPreference(KEY_PREF_EMERGENCY_BTN, emergencyBtnDefaultValue, null);
                resetPreference(KEY_PREF_ROLLTRIM_PLUS_BTN, rollTrimPlusBtnDefaultValue, null);
                resetPreference(KEY_PREF_ROLLTRIM_MINUS_BTN, rollTrimMinusBtnDefaultValue, null);
                resetPreference(KEY_PREF_PITCHTRIM_PLUS_BTN, pitchTrimPlusBtnDefaultValue, null);
                resetPreference(KEY_PREF_PITCHTRIM_MINUS_BTN, pitchTrimMinusBtnDefaultValue, null);
                Toast.makeText(PreferencesActivity.this, "Resetting to default values...", Toast.LENGTH_SHORT).show();
                return true;
            }
        });
        
        findPreference(KEY_PREF_AFC_SCREEN).setEnabled(sharedPreferences.getBoolean(KEY_PREF_AFC_BOOL, false));

        maxRollPitchAngleDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_MAX_ROLLPITCH_ANGLE, R.string.preferences_maxRollPitchAngle_defaultValue);
        maxYawAngleDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_MAX_YAW_ANGLE, R.string.preferences_maxYawAngle_defaultValue);
        maxThrustDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_MAX_THRUST, R.string.preferences_maxThrust_defaultValue);
        minThrustDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_MIN_THRUST, R.string.preferences_minThrust_defaultValue);

        findPreference(KEY_PREF_RESET_AFC).setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                resetPreference(KEY_PREF_MAX_ROLLPITCH_ANGLE, maxRollPitchAngleDefaultValue, null);
                resetPreference(KEY_PREF_MAX_YAW_ANGLE, maxYawAngleDefaultValue, null);
                resetPreference(KEY_PREF_MAX_THRUST, maxThrustDefaultValue, null);
                resetPreference(KEY_PREF_MIN_THRUST, minThrustDefaultValue, null);
                resetPreference(KEY_PREF_XMODE, false);
                return true;
            }
        });
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
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

    // Set summary to be the user-description for the selected value
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(KEY_PREF_RADIO_CHANNEL)) {
            setSummaryInt(key, radioChannelDefaultValue, RADIOCHANNEL_UPPER_LIMIT, "Radio channel");
        }
        if (key.equals(KEY_PREF_RADIO_DATARATE)) {
            Preference radioDataratePref = findPreference(key);
            String[] stringArray = getResources().getStringArray(R.array.radioDatarateEntries);
            String keyString = sharedPreferences.getString(key, "");
            radioDataratePref.setSummary(stringArray[Integer.parseInt(keyString)]);
        }
        if (key.equals(KEY_PREF_MODE)) {
            Preference modePref = findPreference(key);
            modePref.setSummary(sharedPreferences.getString(key, modeDefaultValue));
        }
        if (key.equals(KEY_PREF_DEADZONE)) {
            Preference deadzonePref = findPreference(key);
            try {
                float deadzone = Float.parseFloat(sharedPreferences.getString(key, deadzoneDefaultValue));
                if (deadzone < 0.0 || deadzone > DEADZONE_UPPER_LIMIT) {
                    resetPreference(key, deadzoneDefaultValue, "Deadzone must be a float value between 0.0 and " + DEADZONE_UPPER_LIMIT + ".");
                }
            } catch (NumberFormatException nfe) {
                resetPreference(key, deadzoneDefaultValue, "Deadzone must be a float value between 0.0 and " + DEADZONE_UPPER_LIMIT + ".");
            }
            deadzonePref.setSummary(sharedPreferences.getString(key, ""));
        }

        if (key.equals(KEY_PREF_RIGHT_ANALOG_X_AXIS)){
            findPreference(key).setSummary(sharedPreferences.getString(key, rightAnalogXAxisDefaultValue));
        }
        if (key.equals(KEY_PREF_RIGHT_ANALOG_Y_AXIS)){
            findPreference(key).setSummary(sharedPreferences.getString(key, rightAnalogYAxisDefaultValue));
        }
        if (key.equals(KEY_PREF_LEFT_ANALOG_X_AXIS)){
            findPreference(key).setSummary(sharedPreferences.getString(key, leftAnalogXAxisDefaultValue));
        }
        if (key.equals(KEY_PREF_LEFT_ANALOG_Y_AXIS)){
            findPreference(key).setSummary(sharedPreferences.getString(key, leftAnalogYAxisDefaultValue));
        }
        
        if (key.equals(KEY_PREF_SPLITAXIS_YAW_BOOL)) {
            CheckBoxPreference pref = (CheckBoxPreference) findPreference(key);
            pref.setChecked(sharedPreferences.getBoolean(key, false));
            findPreference(KEY_PREF_SPLITAXIS_YAW_LEFT_AXIS).setEnabled(sharedPreferences.getBoolean(key, false));
            findPreference(KEY_PREF_SPLITAXIS_YAW_RIGHT_AXIS).setEnabled(sharedPreferences.getBoolean(key, false));
        }
        
        if (key.equals(KEY_PREF_SPLITAXIS_YAW_LEFT_AXIS)){
            findPreference(key).setSummary(sharedPreferences.getString(key, splitAxisLeftAxisDefaultValue));
        }
        if (key.equals(KEY_PREF_SPLITAXIS_YAW_RIGHT_AXIS)){
            findPreference(key).setSummary(sharedPreferences.getString(key, splitAxisRightAxisDefaultValue));
        }

        if (key.equals(KEY_PREF_ROLLTRIM) || key.equals(KEY_PREF_PITCHTRIM)) {
            Preference trimPref = findPreference(key);
            try {
                float trim = Float.parseFloat(sharedPreferences.getString(key, trimDefaultValue));
                if (Math.abs(trim) < 0.0 || Math.abs(trim) > TRIM_UPPER_LIMIT) {
                    resetPreference(key, trimDefaultValue, "Roll/Pitch trim must be a float value between 0.0 and " + TRIM_UPPER_LIMIT + ".");
                }
            } catch (NumberFormatException nfe) {
                resetPreference(key, trimDefaultValue, "Roll/Pitch trim must be a float value between 0.0 and " + TRIM_UPPER_LIMIT + ".");
            }
            trimPref.setSummary(sharedPreferences.getString(key, ""));
        }
        if (key.equals(KEY_PREF_EMERGENCY_BTN)) {
            findPreference(key).setSummary(sharedPreferences.getString(key, emergencyBtnDefaultValue));
        }
        if (key.equals(KEY_PREF_ROLLTRIM_PLUS_BTN)) {
            findPreference(key).setSummary(sharedPreferences.getString(key, rollTrimPlusBtnDefaultValue));
        }
        if (key.equals(KEY_PREF_ROLLTRIM_MINUS_BTN)) {
            findPreference(key).setSummary(sharedPreferences.getString(key, rollTrimMinusBtnDefaultValue));
        }
        if (key.equals(KEY_PREF_PITCHTRIM_PLUS_BTN)) {
            findPreference(key).setSummary(sharedPreferences.getString(key, pitchTrimPlusBtnDefaultValue));
        }
        if (key.equals(KEY_PREF_PITCHTRIM_MINUS_BTN)) {
            findPreference(key).setSummary(sharedPreferences.getString(key, pitchTrimMinusBtnDefaultValue));
        }
        
        if (key.equals(KEY_PREF_AFC_BOOL)) {
            Preference afcScreenPref = findPreference(KEY_PREF_AFC_SCREEN);
            afcScreenPref.setEnabled(sharedPreferences.getBoolean(key, false));
            if (!sharedPreferences.getBoolean(key, false)) {
                Toast.makeText(this, "Resetting to default values:\n" + "Max roll/pitch angle: " + maxRollPitchAngleDefaultValue + "\n" + "Max yaw angle: " + maxYawAngleDefaultValue + "\n" + "Max thrust: " + maxThrustDefaultValue + "\n" + "Min thrust: " + minThrustDefaultValue, Toast.LENGTH_LONG)
                        .show();
            } else {
                Toast.makeText(this, "You have been warned!", Toast.LENGTH_LONG).show();
            }
        }
        if (key.equals(KEY_PREF_MAX_ROLLPITCH_ANGLE)) {
            setSummaryInt(key, maxRollPitchAngleDefaultValue, MAX_ROLLPITCH_ANGLE_UPPER_LIMIT, "Max roll/pitch angle");
        }
        if (key.equals(KEY_PREF_MAX_YAW_ANGLE)) {
            setSummaryInt(key, maxYawAngleDefaultValue, MAX_YAW_ANGLE_UPPER_LIMIT, "Max yaw angle");
        }
        if (key.equals(KEY_PREF_MAX_THRUST)) {
            setSummaryInt(key, maxThrustDefaultValue, MAX_THRUST_UPPER_LIMIT, "Max thrust");
        }
        if (key.equals(KEY_PREF_MIN_THRUST)) {
            setSummaryInt(key, minThrustDefaultValue, MIN_THRUST_UPPER_LIMIT, "Min thrust");
        }
        if (key.equals(KEY_PREF_XMODE)) {
            CheckBoxPreference pref = (CheckBoxPreference) findPreference(key);
            pref.setChecked(sharedPreferences.getBoolean(key, false));
        }

        if (key.equals(KEY_PREF_SCREEN_ROTATION_LOCK_BOOL)) {
            CheckBoxPreference pref = (CheckBoxPreference) findPreference(key);
            pref.setChecked(sharedPreferences.getBoolean(key, false));
        }
    }

    private String setInitialSummaryAndReturnDefaultValue(String pKey, int pRDefaultValue) {
        Preference pref = findPreference(pKey);
        String defaultValue = getResources().getString(pRDefaultValue);
        pref.setSummary(sharedPreferences.getString(pKey, defaultValue));
        return defaultValue;
    }

    private void setSummaryInt(String key, String pDefaultValue, int pUpperLimit, String pValueName) {
        Preference pref = findPreference(key);
        try {
            int newPrefValue = Integer.parseInt(sharedPreferences.getString(key, pDefaultValue));
            if (newPrefValue < 0 || newPrefValue > pUpperLimit) {
                resetPreference(key, pDefaultValue, pValueName + " must be an integer value between 0 and " + pUpperLimit + ".");
            }
        } catch (NumberFormatException nfe) {
            resetPreference(key, pDefaultValue, pValueName + " must be an integer value between 0 and " + pUpperLimit + ".");
        }
        pref.setSummary(sharedPreferences.getString(key, ""));
    }

    private void resetPreference(String pKey, String pDefaultValue, String pErrorMessage) {
        if (pErrorMessage != null) {
            Toast.makeText(this, pErrorMessage + "\nResetting to default value " + pDefaultValue + ".", Toast.LENGTH_SHORT).show();
        }
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(pKey, pDefaultValue);
        editor.commit();
    }

    private void resetPreference(String pKey, boolean pDefaultValue) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(pKey, pDefaultValue);
        editor.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

}
