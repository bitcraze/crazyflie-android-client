package se.bitcraze.crazyfliecontrol.controller;

import se.bitcraze.crazyfliecontrol.Controls;


public class AbstractController implements IController {

	protected Controls mControls;

	public AbstractController(Controls controls) {
		mControls = controls;
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
