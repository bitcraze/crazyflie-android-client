package se.bitcraze.crazyfliecontrol;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.Toast;

public class Controls {

    private static final String LOG_TAG = "Controls";

    private MainActivity mActivity;
    private SharedPreferences mPreferences;

    //Raw input values
    private float mRight_analog_x;
    private float mRight_analog_y;
    private float mLeft_analog_x;
    private float mLeft_analog_y;
    private float mSplit_axis_yaw_right;
    private float mSplit_axis_yaw_left;

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

    public void dealWithMotionEvent(MotionEvent event){
        //Log.i(LOG_TAG, "Input device: " + event.getDevice().getName());

        if (!mActivity.isOnscreenControllerDisabled()) {
            mActivity.disableOnscreenController();
        }

        // default axis are set to work with PS3 controller
        mRight_analog_x = (float) (event.getAxisValue(mRightAnalogXAxis));
        mRight_analog_y = (float) (event.getAxisValue(mRightAnalogYAxis)) * (needsInverting(mRightAnalogYAxis) ? -1 : 1);
        mLeft_analog_x = (float) (event.getAxisValue(mLeftAnalogXAxis));
        mLeft_analog_y = (float) (event.getAxisValue(mLeftAnalogYAxis)) * (needsInverting(mLeftAnalogYAxis) ? -1 : 1);

        mSplit_axis_yaw_right = (float) (event.getAxisValue(mSplitAxisYawRightAxis));
        mSplit_axis_yaw_left = (float) (event.getAxisValue(mSplitAxisYawLeftAxis));
    }

    /**
     * Only invert if axis is not analog R1 (Brake) or analog R2 (Gas) shoulder button
     * 
     * TODO: might need to be improved to work for other controllers
     * 
     * @param axis
     * @return
     */
    private boolean needsInverting(int axis) {
        return !(axis == MotionEvent.AXIS_BRAKE || axis == MotionEvent.AXIS_GAS);
    }

    public void dealWithKeyEvent(KeyEvent event){
        switch (event.getAction()) {
        case KeyEvent.ACTION_DOWN:
            if(event.getKeyCode() == mEmergencyBtn){
                //quick solution
                resetAxisValues();
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
    }
    
    public float getRollTrim() {
        return mRollTrim;
    }

    public float getPitchTrim() {
        return mPitchTrim;
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

    public void resetAxisValues() {
        mRight_analog_y = 0;
        mRight_analog_x = 0;
        mLeft_analog_y = 0;
        mLeft_analog_x = 0;
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
    
    public float getDeadzone() {
        return mDeadzone;
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

    public boolean useSplitAxisYaw(){
        return mUseSplitAxisYaw;
    }

    public float getSplitAxisYawRight() {
        return mSplit_axis_yaw_right;
    }

    public float getSplitAxisYawLeft() {
        return mSplit_axis_yaw_left;
    }
}
