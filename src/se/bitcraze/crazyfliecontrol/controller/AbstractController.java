package se.bitcraze.crazyfliecontrol.controller;

import android.widget.Toast;
import se.bitcraze.crazyfliecontrol.MainActivity;


public abstract class AbstractController implements IController {

	protected Controls mControls;
	protected boolean mIsDisabled;
	protected MainActivity mActivity;

	public AbstractController(Controls controls, MainActivity activity) {
		mControls = controls;
		mActivity = activity;
	}

	public void enable(){
		mIsDisabled = false;
        Toast.makeText(mActivity, "Using " + getControllerName(), Toast.LENGTH_SHORT).show();
	}
	
    public void disable() {
        mIsDisabled = true;
    }

    public boolean isDisabled() {
        return mIsDisabled;
    }

    public String getControllerName(){
    	return "unknown controller";
    }
    
    public void updateFlightData() {
        mActivity.updateFlightData();		
	}
    
    public float getThrust() {
        float thrust = ((mControls.getMode() == 1 || mControls.getMode() == 3) ? mControls.getRightAnalog_Y() : mControls.getLeftAnalog_Y());
        if (thrust > mControls.getDeadzone()) {
            return mControls.getMinThrust() + (thrust * getThrustFactor());
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

    
    //TODO: move methods to Controls?
    public float getRollPitchFactor() {
        return mControls.getMaxRollPitchAngle();
    }

    public float getYawFactor() {
        return mControls.getMaxYawAngle();
    }

    public float getThrustFactor() {
        int addThrust = 0;
        if ((mControls.getMaxThrust() - mControls.getMinThrust()) < 0) {
            addThrust = 0; // do not allow negative values
        } else {
            addThrust = (mControls.getMaxThrust() - mControls.getMinThrust());
        }
        return addThrust;
    }

}
