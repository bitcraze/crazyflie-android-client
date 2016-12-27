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

package se.bitcraze.crazyfliecontrol.controller;

import se.bitcraze.crazyflie.lib.crazyradio.RadioDriver;
import se.bitcraze.crazyfliecontrol.prefs.PreferencesActivity;
import se.bitcraze.crazyfliecontrol2.MainActivity;
import se.bitcraze.crazyfliecontrol2.R;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.Toast;

/**
 * The GamepadController deals with external controllers (e.g. a PS3 or XBox game pad).
 * When the split axis yaw setting is activated, the shoulder buttons of the game pads
 * can be used to control the yaw axis.
 *
 */
public class GamepadController extends AbstractController {

    private SharedPreferences mPreferences;

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
    private int mAlt1Btn;
    private int mAlt2Btn;
    private int mHoverBtn;

    private int mRightAnalogYAxisInvertFactor = -1;
    private int mLeftAnalogYAxisInvertFactor = -1;

    //Split axis
    private float mSplit_axis_yaw_right;
    private float mSplit_axis_yaw_left;
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
    private String mAlt1BtnDefaultValue;
    private String mAlt2BtnDefaultValue;
    private String mHoverBtnDefaultValue;

    private boolean mHover = false;

    public GamepadController(Controls controls, MainActivity activity, SharedPreferences preferences) {
        super(controls, activity);
        this.mPreferences = preferences;
    }

    public String getControllerName(){
    	return "gamepad controller";
    }

    public void dealWithMotionEvent(MotionEvent event){
        //Log.i(LOG_TAG, "Input device: " + event.getDevice().getName());
        /*if axis has a range of 1 (0 -> 1) instead of 2 (-1 -> 0) do not invert axis value,
        this is necessary for analog R1 (Brake) or analog R2 (Gas) shoulder buttons on PS3 controller*/
        mRightAnalogYAxisInvertFactor = (event.getDevice().getMotionRange(mRightAnalogYAxis).getRange() == 1) ? 1 : -1;
        mLeftAnalogYAxisInvertFactor = (event.getDevice().getMotionRange(mLeftAnalogYAxis).getRange() == 1) ? 1 : -1;

        // default axis are set to work with PS3 controller
        mControls.setRightAnalogX((float) event.getAxisValue(mRightAnalogXAxis));
        mControls.setRightAnalogY((float) (event.getAxisValue(mRightAnalogYAxis)) * mRightAnalogYAxisInvertFactor);
        mControls.setLeftAnalogX((float) event.getAxisValue(mLeftAnalogXAxis));
        mControls.setLeftAnalogY((float) (event.getAxisValue(mLeftAnalogYAxis)) * mLeftAnalogYAxisInvertFactor);

        mSplit_axis_yaw_right = (float) event.getAxisValue(mSplitAxisYawRightAxis);
        mSplit_axis_yaw_left = (float) event.getAxisValue(mSplitAxisYawLeftAxis);
    }

    public void dealWithKeyEvent(KeyEvent event){
        switch (event.getAction()) {
        case KeyEvent.ACTION_DOWN:
            if (event.getKeyCode() == mEmergencyBtn){
                //quick solution
                mControls.resetAxisValues();
                if (mActivity.getCrazyflie() != null) {
                    mActivity.disconnect();
                }
                Toast.makeText(mActivity, "Emergency Stop", Toast.LENGTH_SHORT).show();
            } else if (event.getKeyCode() == mRollTrimPlusBtn) {
                mControls.increaseTrim(PreferencesActivity.KEY_PREF_ROLLTRIM);
            } else if (event.getKeyCode() == mRollTrimMinusBtn) {
                mControls.decreaseTrim(PreferencesActivity.KEY_PREF_ROLLTRIM);
            } else if (event.getKeyCode() == mPitchTrimPlusBtn) {
                mControls.increaseTrim(PreferencesActivity.KEY_PREF_PITCHTRIM);
            } else if (event.getKeyCode() == mPitchTrimMinusBtn) {
                mControls.decreaseTrim(PreferencesActivity.KEY_PREF_PITCHTRIM);
            } else if (event.getKeyCode() == mAlt1Btn) {
                mActivity.runAltAction(mControls.getAlt1Action());
            } else if (event.getKeyCode() == mAlt2Btn) {
                mActivity.runAltAction(mControls.getAlt2Action());
            } else if (event.getKeyCode() == mHoverBtn) {
                if (mActivity.getCrazyflie().getDriver() instanceof RadioDriver) {
                    mHover = true;
                    mActivity.enableAltHoldMode(mHover);
                }
            }
            break;
        case KeyEvent.ACTION_UP:
            if(event.getKeyCode() == mHoverBtn) {
                if (mActivity.getCrazyflie().getDriver() instanceof RadioDriver) {
                    mHover = false;
                    mActivity.enableAltHoldMode(mHover);
                }
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
        mAlt1BtnDefaultValue = res.getString(R.string.preferences_alt1_btn_defaultValue);
        mAlt2BtnDefaultValue = res.getString(R.string.preferences_alt2_btn_defaultValue);
        mHoverBtnDefaultValue = res.getString(R.string.preferences_hover_btn_defaultValue);
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
        this.mAlt1Btn = KeyEvent.keyCodeFromString(mPreferences.getString(PreferencesActivity.KEY_PREF_ALT1_BTN, mAlt1BtnDefaultValue));
        this.mAlt2Btn = KeyEvent.keyCodeFromString(mPreferences.getString(PreferencesActivity.KEY_PREF_ALT2_BTN, mAlt2BtnDefaultValue));
        this.mHoverBtn = KeyEvent.keyCodeFromString(mPreferences.getString(PreferencesActivity.KEY_PREF_HOVER_BTN, mHoverBtnDefaultValue));
    }

    private boolean useSplitAxisYaw(){
        return mUseSplitAxisYaw;
    }

    /*
     * Handles split axis
     * (non-Javadoc)
     * @see se.bitcraze.crazyfliecontrol.controller.AbstractController#getYaw()
     */
    @Override
    public float getYaw() {
        float yaw = 0;
        if(useSplitAxisYaw()){
            yaw = mSplit_axis_yaw_right - mSplit_axis_yaw_left;
        }else{
            yaw = (mControls.getMode() == 1 || mControls.getMode() == 2) ? mControls.getLeftAnalog_X() : mControls.getRightAnalog_X();
        }
        return yaw * mControls.getYawFactor() * mControls.getDeadzone(yaw);
    }

    public boolean isHover() {
        return this.mHover;
    }

    /*
     * Thrust value in percent (used in the UI)
     */
    public float getThrust() {
        float thrust = ((mControls.getMode() == 1 || mControls.getMode() == 3) ? mControls.getRightAnalog_Y() : mControls.getLeftAnalog_Y());
        //Hacky Hover Mode
        if (isHover() && mControls.getDeadzone(thrust) == 1) {
            float minThrust = mControls.getMinThrust();
            if (thrust < 0) {
                minThrust = minThrust * -1;
            }
            return minThrust + (thrust * mControls.getThrustFactor());
        } else if (thrust > mControls.getDeadzone()) {
            return mControls.getMinThrust() + (thrust * mControls.getThrustFactor());
        }
        return 0;
    }

    /*
     * Absolute thrust value (gets send to the Crazyflie)
     */
    public float getThrustAbsolute() {
        float thrust = getThrust();
        float absThrust = thrust/100 * MAX_THRUST;

        //Hacky Hover Mode
        if(isHover()) {
            return 32767 + ((absThrust/65535)*32767);
        } else {
            if(thrust > 0) {
                return absThrust;
            }
        }
        return 0;
    }

}
