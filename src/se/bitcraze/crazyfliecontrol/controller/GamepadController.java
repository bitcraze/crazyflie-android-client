package se.bitcraze.crazyfliecontrol.controller;

import se.bitcraze.crazyfliecontrol.MainActivity;
import se.bitcraze.crazyfliecontrol.PreferencesActivity;
import se.bitcraze.crazyfliecontrol.R;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.Toast;

public class GamepadController extends Controller {
	
	private SharedPreferences mPreferences;
	private MainActivity mActivity;
	
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
    
    private int mRightAnalogYAxisInvertFactor = -1;
    private int mLeftAnalogYAxisInvertFactor = -1;
        
    private boolean mUseSplitAxisYaw = false;
    
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
    
	public GamepadController(Controls controls, MainActivity activity, SharedPreferences preferences) {
		super(controls);
		this.mActivity = activity;
		this.mPreferences = preferences;
	}
	
	public void dealWithMotionEvent(MotionEvent event){
        //Log.i(LOG_TAG, "Input device: " + event.getDevice().getName());
        /*if axis has a range of 1 (0 -> 1) instead of 2 (-1 -> 0) do not invert axis value,
        this is necessary for analog R1 (Brake) or analog R2 (Gas) shoulder buttons on PS3 controller*/
        mRightAnalogYAxisInvertFactor = (event.getDevice().getMotionRange(mRightAnalogYAxis).getRange() == 1) ? 1 : -1;
        mLeftAnalogYAxisInvertFactor = (event.getDevice().getMotionRange(mLeftAnalogYAxis).getRange() == 1) ? 1 : -1;
        
        // default axis are set to work with PS3 controller
        roll = (float) (event.getAxisValue(mRightAnalogXAxis));
        thrust = (float) (event.getAxisValue(mRightAnalogYAxis) * mRightAnalogYAxisInvertFactor);                
        pitch = (float) (event.getAxisValue(mLeftAnalogYAxis) * mLeftAnalogYAxisInvertFactor);
        
        if(mUseSplitAxisYaw){
            yaw = (float) (event.getAxisValue(mSplitAxisYawRightAxis)) - (float) (event.getAxisValue(mSplitAxisYawLeftAxis));
        }else{
            //TODO:
        	//yaw = (mControls.getMode() == 1 || mControls.getMode() == 2) ? mControls.getLeftAnalog_X() : mControls.getRightAnalog_X();
        	
        	//TODO: with the mLeftAnagolXAxis's value should do *-1? 
            yaw = (float) (event.getAxisValue(mLeftAnalogXAxis));
        }
        updateFlightData();
    }
	
    public void dealWithKeyEvent(KeyEvent event){
        switch (event.getAction()) {
        case KeyEvent.ACTION_DOWN:
            if(event.getKeyCode() == mEmergencyBtn){
                //quick solution
            	roll = 0;
            	thrust = 0;
            	pitch = 0;
            	yaw = 0;
                if (mActivity.getCrazyflieLink() != null) {
                    mActivity.linkDisconnect();
                }
                Toast.makeText(mActivity, "Emergency Stop", Toast.LENGTH_SHORT).show();
            }else if (event.getKeyCode() == mRollTrimPlusBtn) {
                controls.increaseTrim(PreferencesActivity.KEY_PREF_ROLLTRIM);
            }else if (event.getKeyCode() == mRollTrimMinusBtn) {
                controls.decreaseTrim(PreferencesActivity.KEY_PREF_ROLLTRIM);
            }else if (event.getKeyCode() == mPitchTrimPlusBtn) {
                controls.increaseTrim(PreferencesActivity.KEY_PREF_PITCHTRIM);
            }else if (event.getKeyCode() == mPitchTrimMinusBtn) {
                controls.decreaseTrim(PreferencesActivity.KEY_PREF_PITCHTRIM);
            }
            break;
        default:
            break;
        }
    }
    
    public void setDefaultPreferenceValues(Resources res){
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
}
