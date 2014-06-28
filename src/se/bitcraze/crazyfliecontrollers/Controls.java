package se.bitcraze.crazyfliecontrollers;

import se.bitcraze.crazyfliecontrol.FlightDataView;
import se.bitcraze.crazyfliecontrol.MainActivity;
import se.bitcraze.crazyfliecontrol.PreferencesActivity;
import se.bitcraze.crazyfliecontrol.R;
import se.bitcraze.crazyfliecontrol.R.string;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.Toast;

public class Controls {

    private MainActivity mActivity;
    private SharedPreferences mPreferences;

    public static final int MAX_THRUST = 65535;
    
    //Advanced flight control
    private int mMaxRollPitchAngle;
    private int mMaxYawAngle;
    private int mMaxThrust;
    private int mMinThrust;
    private boolean mXmode; //determines Crazyflie flight configuration (false = +, true = x)

    //Advanced flight control default rules
    private String mMaxRollPitchAngleDefaultValue;
    private String mMaxYawAngleDefaultValue;
    private String mMaxThrustDefaultValue;
    private String mMinThrustDefaultValue;
    
    //Trim values
    private float mRollTrim;
    private float mPitchTrim;
    private float mMaxTrim = 0.5f;
    private float mTrimIncrements = 0.1f;
    private String mTrimDefaultValue;

    private int mMode;      //Controller axis mapping (Mode 1-4)
    private float mDeadzone;

    private String mModeDefaultValue;
    private String mDeadzoneDefaultValue;

    //Gamepad axis/buttons
    private int mRightAnalogXAxis;
    private int mRightAnalogYAxis;
    private int mLeftAnalogXAxis;
    private int mLeftAnalogYAxis;
    private int mSplitAxisYawLeftAxis;
    private int mSplitAxisYawRightAxis;
    private int mEmergencyBtn;
    private int mRollTrimPlusBtn;
    private int mRollTrimMinusBtn;
    private int mPitchTrimPlusBtn;
    private int mPitchTrimMinusBtn;
    
    //Default gamepad axis/buttons
    private String mRightAnalogXAxisDefaultValue;
    private String mRightAnalogYAxisDefaultValue;
    private String mLeftAnalogXAxisDefaultValue;
    private String mLeftAnalogYAxisDefaultValue;
    private String mSplitAxisYawLeftAxisDefaultValue;
    private String mSplitAxisYawRightAxisDefaultValue;
    private String mEmergencyBtnDefaultValue;
    private String mRollTrimPlusBtnDefaultValue;
    private String mRollTrimMinusBtnDefaultValue;
    private String mPitchTrimPlusBtnDefaultValue;
    private String mPitchTrimMinusBtnDefaultValue;

    private boolean mUseSplitAxisYaw = false;

    public Controls(MainActivity activity, SharedPreferences preferences) {
        this.mActivity = activity;
        this.mPreferences = preferences;
    }

    public void dealWithKeyEvent(KeyEvent event){
        switch (event.getAction()) {
        case KeyEvent.ACTION_DOWN:
            if(event.getKeyCode() == mEmergencyBtn){
                //quick solution
                //Todo: check this method
                //resetAxisValues();
                if (mActivity.getCrazyflieLink() != null) {
                    mActivity.linkDisconnect();
                }
                Toast.makeText(mActivity, "Emergency Stop", Toast.LENGTH_SHORT).show();
            }else if (event.getKeyCode() == mRollTrimPlusBtn) {
                increaseTrim(PreferencesActivity.KEY_PREF_ROLLTRIM);
            }else if (event.getKeyCode() == mRollTrimMinusBtn) {
                decreaseTrim(PreferencesActivity.KEY_PREF_ROLLTRIM);
            }else if (event.getKeyCode() == mPitchTrimPlusBtn) {
                increaseTrim(PreferencesActivity.KEY_PREF_PITCHTRIM);
            }else if (event.getKeyCode() == mPitchTrimMinusBtn) {
                decreaseTrim(PreferencesActivity.KEY_PREF_PITCHTRIM);
            }
            break;
        default:
            break;
        }
    }

    public void setDefaultPreferenceValues(Resources res){
        mModeDefaultValue = res.getString(R.string.preferences_mode_defaultValue);
        mDeadzoneDefaultValue = res.getString(R.string.preferences_deadzone_defaultValue);

        mTrimDefaultValue = res.getString(R.string.preferences_trim_defaultValue);

        mRightAnalogXAxisDefaultValue = res.getString(R.string.preferences_right_analog_x_axis_defaultValue);
        mRightAnalogYAxisDefaultValue = res.getString(R.string.preferences_right_analog_y_axis_defaultValue);
        mLeftAnalogXAxisDefaultValue = res.getString(R.string.preferences_left_analog_x_axis_defaultValue);
        mLeftAnalogYAxisDefaultValue = res.getString(R.string.preferences_left_analog_y_axis_defaultValue);

        mSplitAxisYawLeftAxisDefaultValue = res.getString(R.string.preferences_splitaxis_yaw_left_axis_defaultValue);
        mSplitAxisYawRightAxisDefaultValue = res.getString(R.string.preferences_splitaxis_yaw_right_axis_defaultValue);

        mEmergencyBtnDefaultValue = res.getString(R.string.preferences_emergency_btn_defaultValue);
        mRollTrimPlusBtnDefaultValue = res.getString(R.string.preferences_rolltrim_plus_btn_defaultValue);
        mRollTrimMinusBtnDefaultValue = res.getString(R.string.preferences_rolltrim_minus_btn_defaultValue);
        mPitchTrimPlusBtnDefaultValue = res.getString(R.string.preferences_pitchtrim_plus_btn_defaultValue);
        mPitchTrimMinusBtnDefaultValue = res.getString(R.string.preferences_pitchtrim_minus_btn_defaultValue);
        
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
        this.mRightAnalogXAxis = MotionEvent.axisFromString(mPreferences.getString(PreferencesActivity.KEY_PREF_RIGHT_ANALOG_X_AXIS, mRightAnalogXAxisDefaultValue)); 
        this.mRightAnalogYAxis = MotionEvent.axisFromString(mPreferences.getString(PreferencesActivity.KEY_PREF_RIGHT_ANALOG_Y_AXIS, mRightAnalogYAxisDefaultValue)); 
        this.mLeftAnalogXAxis = MotionEvent.axisFromString(mPreferences.getString(PreferencesActivity.KEY_PREF_LEFT_ANALOG_X_AXIS, mLeftAnalogXAxisDefaultValue)); 
        this.mLeftAnalogYAxis = MotionEvent.axisFromString(mPreferences.getString(PreferencesActivity.KEY_PREF_LEFT_ANALOG_Y_AXIS, mLeftAnalogYAxisDefaultValue)); 
        this.mUseSplitAxisYaw = mPreferences.getBoolean(PreferencesActivity.KEY_PREF_SPLITAXIS_YAW_BOOL, false);
        this.mSplitAxisYawLeftAxis = MotionEvent.axisFromString(mPreferences.getString(PreferencesActivity.KEY_PREF_SPLITAXIS_YAW_LEFT_AXIS, mSplitAxisYawLeftAxisDefaultValue)); 
        this.mSplitAxisYawRightAxis = MotionEvent.axisFromString(mPreferences.getString(PreferencesActivity.KEY_PREF_SPLITAXIS_YAW_RIGHT_AXIS, mSplitAxisYawRightAxisDefaultValue)); 
        this.mEmergencyBtn = KeyEvent.keyCodeFromString(mPreferences.getString(PreferencesActivity.KEY_PREF_EMERGENCY_BTN, mEmergencyBtnDefaultValue));
        this.mRollTrimPlusBtn = KeyEvent.keyCodeFromString(mPreferences.getString(PreferencesActivity.KEY_PREF_ROLLTRIM_PLUS_BTN, mRollTrimPlusBtnDefaultValue));
        this.mRollTrimMinusBtn = KeyEvent.keyCodeFromString(mPreferences.getString(PreferencesActivity.KEY_PREF_ROLLTRIM_MINUS_BTN, mRollTrimMinusBtnDefaultValue));
        this.mPitchTrimPlusBtn = KeyEvent.keyCodeFromString(mPreferences.getString(PreferencesActivity.KEY_PREF_PITCHTRIM_PLUS_BTN, mPitchTrimPlusBtnDefaultValue));
        this.mPitchTrimMinusBtn = KeyEvent.keyCodeFromString(mPreferences.getString(PreferencesActivity.KEY_PREF_PITCHTRIM_MINUS_BTN, mPitchTrimMinusBtnDefaultValue));
        
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
        Toast.makeText(mActivity, axisName + " Trim: " + FlightDataView.round(axis), Toast.LENGTH_SHORT).show();
        
        if(PreferencesActivity.KEY_PREF_ROLLTRIM.equals(prefKey)){
            mRollTrim = axis;
        }else{
            mPitchTrim = axis;
        }
    }

    private void setPreference(String pKey, String pDefaultValue) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(pKey, pDefaultValue);
        editor.commit();
    }

    public int getMode(){
        return mMode;
    }

    public float getDeadzone(float axis) {
        if (axis < mDeadzone && axis > mDeadzone * -1) {
            return 0;
        }
        return 1;
    }
    
    public float getMinThrust(){
    	return mMinThrust;
    }

    public float getMaxThrust(){
    	return mMaxThrust;
    }
    
    public boolean getXmode(){
    	return mXmode;
    }
    
    public float getDeadzone() {
        return mDeadzone;
    }

    public int getRightAnalogXAxis() { return mRightAnalogXAxis; }
    public int getRightAnalogYAxis() { return mRightAnalogYAxis; }
    public int getLeftAnalogXAxis() { return mLeftAnalogXAxis; }
    public int getLeftAnalogYAxis() { return mLeftAnalogYAxis; }

    public boolean useSplitAxisYaw() { return mUseSplitAxisYaw; }

    public int getSplitAxisYawLeftAxis() { return mSplitAxisYawLeftAxis; }
    public int getSplitAxisYawRightAxis() { return mSplitAxisYawRightAxis; }

    public float getRollTrim() {
        return mRollTrim;
    }

    public float getPitchTrim() {
        return mPitchTrim;
    }

    public float getMaxRollPitchAngle() {
        return mMaxRollPitchAngle;
    }
    
    public float getMaxYawAngle() {
    	return mMaxYawAngle;
    }

}
