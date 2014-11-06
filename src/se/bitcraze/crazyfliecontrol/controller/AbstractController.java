package se.bitcraze.crazyfliecontrol.controller;

import se.bitcraze.crazyfliecontrol.MainActivity;
import android.widget.Toast;


/**
 * The AbstractController implements the basic methods of IController class
 *
 */
public abstract class AbstractController implements IController {

	protected Controls mControls;
	protected boolean mIsDisabled;
	protected MainActivity mActivity;

    private static final int MAX_THRUST = 65535;

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
        updateFlightData();
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
            return mControls.getMinThrust() + (thrust * mControls.getThrustFactor())/100 * MAX_THRUST;
        }
        return 0;
    }

    public float getRoll() {
        float roll = (mControls.getMode() == 1 || mControls.getMode() == 2) ? mControls.getRightAnalog_X() : mControls.getLeftAnalog_X();
        return (roll + mControls.getRollTrim()) * mControls.getRollPitchFactor() * mControls.getDeadzone(roll);
    }

    public float getPitch() {
        float pitch = (mControls.getMode() == 1 || mControls.getMode() == 3) ? mControls.getLeftAnalog_Y() : mControls.getRightAnalog_Y();
        return (pitch + mControls.getPitchTrim()) * mControls.getRollPitchFactor() * mControls.getDeadzone(pitch);
    }

    public float getYaw() {
        float yaw = 0;
        yaw = (mControls.getMode() == 1 || mControls.getMode() == 2) ? mControls.getLeftAnalog_X() : mControls.getRightAnalog_X();
        return yaw * mControls.getYawFactor() * mControls.getDeadzone(yaw);
    }

}
