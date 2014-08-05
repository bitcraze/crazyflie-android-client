package se.bitcraze.crazyfliecontrol.controller;

import se.bitcraze.crazyfliecontrol.FlightDataView;
import se.bitcraze.crazyfliecontrol.MainActivity;
import se.bitcraze.crazyfliecontrol.R;
import se.bitcraze.crazyfliecontrol.prefs.PreferencesActivity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.widget.Toast;

/**
 * This class encapsulates the common preferences for all types of controllers.
 * 
 * TODO: rename Class
 */
public class Controls {

    private static final String LOG_TAG = "Controls";

    private MainActivity mActivity;
    private SharedPreferences mPreferences;

    // Raw input values
    private float mRight_analog_x;
    private float mRight_analog_y;
    private float mLeft_analog_x;
    private float mLeft_analog_y;

    // Trim values
    private float mRollTrim;
    private float mPitchTrim;
    private float mMaxTrim = 0.5f;
    private float mTrimIncrements = 0.1f;
    private String mTrimDefaultValue;

    private int mMode; // Controller axis mapping (Mode 1-4)
    private float mDeadzone;

    private boolean mUseGyro;

    private String mModeDefaultValue;
    private String mDeadzoneDefaultValue;

    private boolean mXmode; // determines Crazyflie flight configuration (false = +, true = x)

    //Advanced flight control
    private int mMaxRollPitchAngle;
    private int mMaxYawAngle;
    private int mMaxThrust;
    private int mMinThrust;

    //Advanced flight control default values
    private String mMaxRollPitchAngleDefaultValue;
    private String mMaxYawAngleDefaultValue;
    private String mMaxThrustDefaultValue;
    private String mMinThrustDefaultValue;

    public Controls(MainActivity activity, SharedPreferences preferences) {
        this.mActivity = activity;
        this.mPreferences = preferences;
    }

    public void setDefaultPreferenceValues(Resources res) {
        mModeDefaultValue = res.getString(R.string.preferences_mode_defaultValue);
        mDeadzoneDefaultValue = res.getString(R.string.preferences_deadzone_defaultValue);

        mTrimDefaultValue = res.getString(R.string.preferences_trim_defaultValue);

        //Advanced flight control
        mMaxRollPitchAngleDefaultValue = res.getString(R.string.preferences_maxRollPitchAngle_defaultValue);
        mMaxYawAngleDefaultValue = res.getString(R.string.preferences_maxYawAngle_defaultValue);
        mMaxThrustDefaultValue = res.getString(R.string.preferences_maxThrust_defaultValue);
        mMinThrustDefaultValue = res.getString(R.string.preferences_minThrust_defaultValue);
    }

    public void setControlConfig() {
        this.mMode = Integer.parseInt(mPreferences.getString(PreferencesActivity.KEY_PREF_MODE, mModeDefaultValue));
        this.mDeadzone = Float.parseFloat(mPreferences.getString(PreferencesActivity.KEY_PREF_DEADZONE, mDeadzoneDefaultValue));

        this.mRollTrim = Float.parseFloat(mPreferences.getString(PreferencesActivity.KEY_PREF_ROLLTRIM, mTrimDefaultValue));
        this.mPitchTrim = Float.parseFloat(mPreferences.getString(PreferencesActivity.KEY_PREF_PITCHTRIM, mTrimDefaultValue));

        this.mUseGyro = mPreferences.getBoolean(PreferencesActivity.KEY_PREF_USE_GYRO_BOOL, false);

        //Advanced flight control
        if (mPreferences.getBoolean(PreferencesActivity.KEY_PREF_AFC_BOOL, false)) {
            this.mMaxRollPitchAngle = Integer.parseInt(mPreferences.getString(PreferencesActivity.KEY_PREF_MAX_ROLLPITCH_ANGLE, mMaxRollPitchAngleDefaultValue));
            this.mMaxYawAngle = Integer.parseInt(mPreferences.getString(PreferencesActivity.KEY_PREF_MAX_YAW_ANGLE, mMaxYawAngleDefaultValue));
            this.mMaxThrust = Integer.parseInt(mPreferences.getString(PreferencesActivity.KEY_PREF_MAX_THRUST, mMaxThrustDefaultValue));
            this.mMinThrust = Integer.parseInt(mPreferences.getString(PreferencesActivity.KEY_PREF_MIN_THRUST, mMinThrustDefaultValue));
            this.mXmode = mPreferences.getBoolean(PreferencesActivity.KEY_PREF_XMODE, false);
        } else {
            this.mMaxRollPitchAngle = Integer.parseInt(mMaxRollPitchAngleDefaultValue);
            this.mMaxYawAngle = Integer.parseInt(mMaxYawAngleDefaultValue);
            this.mMaxThrust = Integer.parseInt(mMaxThrustDefaultValue);
            this.mMinThrust = Integer.parseInt(mMinThrustDefaultValue);
            this.mXmode = false;
        }
    }

    public float getRollTrim() {
        return mRollTrim;
    }

    public float getPitchTrim() {
        return mPitchTrim;
    }

    public void increaseTrim(String prefKey) {
        changeTrim(prefKey, true);
    }

    public void decreaseTrim(String prefKey) {
        changeTrim(prefKey, false);
    }

    private void changeTrim(String prefKey, boolean increase) {
        float axis;
        String axisName;
        if (PreferencesActivity.KEY_PREF_ROLLTRIM.equals(prefKey)) {
            axisName = "Roll";
            axis = mRollTrim;
        } else {
            axisName = "Pitch";
            axis = mPitchTrim;
        }

        if (increase && axis < mMaxTrim) {
            axis += mTrimIncrements;
        } else if (!increase && axis > (mMaxTrim * -1)) {
            axis -= mTrimIncrements;
        }

        setPreference(prefKey, String.valueOf(axis));
        Toast.makeText(mActivity, axisName + " Trim: " + FlightDataView.round(axis), Toast.LENGTH_SHORT).show();

        if (PreferencesActivity.KEY_PREF_ROLLTRIM.equals(prefKey)) {
            mRollTrim = axis;
        } else {
            mPitchTrim = axis;
        }
    }

    private void setPreference(String pKey, String pDefaultValue) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(pKey, pDefaultValue);
        editor.commit();
    }

    public void resetAxisValues() {
        mRight_analog_y = 0;
        mRight_analog_x = 0;
        mLeft_analog_y = 0;
        mLeft_analog_x = 0;
    }

    public int getMode() {
        return mMode;
    }

    public float getDeadzone(float axis) {
        if (axis < mDeadzone && axis > mDeadzone * -1) {
            return 0;
        }
        return 1;
    }

    public float getDeadzone() {
        return mDeadzone;
    }

    public boolean isUseGyro() {
        return mUseGyro;
    }

    public float getRightAnalog_X() {
        return mRight_analog_x;
    }

    public float getRightAnalog_Y() {
        return mRight_analog_y;
    }

    public float getLeftAnalog_X() {
        return mLeft_analog_x;
    }

    public float getLeftAnalog_Y() {
        return mLeft_analog_y;
    }

    // TODO: move methods to AbstractController?

    public void setRightAnalogX(float right_analog_x) {
        this.mRight_analog_x = right_analog_x;
    }

    public void setRightAnalogY(float right_analog_y) {
        this.mRight_analog_y = right_analog_y;
    }

    public void setLeftAnalogX(float left_analog_x) {
        this.mLeft_analog_x = left_analog_x;
    }

    public void setLeftAnalogY(float left_analog_y) {
        this.mLeft_analog_y = left_analog_y;
    }

    public boolean isXmode() {
        return this.mXmode;
    }

    public int getMaxRollPitchAngle() {
        return mMaxRollPitchAngle;
    }

    public int getMaxYawAngle() {
        return mMaxYawAngle;
    }

    public int getMaxThrust() {
        return mMaxThrust;
    }

    public int getMinThrust() {
        return mMinThrust;
    }

    // TODO: move methods to Controls?
    public float getRollPitchFactor() {
        return getMaxRollPitchAngle();
    }

    public float getYawFactor() {
        return getMaxYawAngle();
    }

    public float getThrustFactor() {
        int addThrust = 0;
        if ((getMaxThrust() - getMinThrust()) < 0) {
            addThrust = 0; // do not allow negative values
        } else {
            addThrust = (getMaxThrust() - getMinThrust());
        }
        return addThrust;
    }
}
