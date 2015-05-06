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

import se.bitcraze.crazyfliecontrol2.MainActivity;
import android.widget.Toast;


/**
 * The AbstractController implements the basic methods of IController class
 *
 */
public abstract class AbstractController implements IController {

	protected Controls mControls;
	protected boolean mIsDisabled;
	protected MainActivity mActivity;

    protected static final int MAX_THRUST = 65000;

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

    /*
     * Thrust value in percent (used in the UI)
     */
    public float getThrust() {
        float thrust = ((mControls.getMode() == 1 || mControls.getMode() == 3) ? mControls.getRightAnalog_Y() : mControls.getLeftAnalog_Y());
        if (thrust > mControls.getDeadzone()) {
            return mControls.getMinThrust() + (thrust * mControls.getThrustFactor());
        }
        return 0;
    }

    /*
     * Absolute thrust value (gets send to the Crazyflie)
     */
    public float getThrustAbsolute() {
        float thrust = getThrust();
        if(thrust > 0) {
            return thrust/100 * MAX_THRUST;
        }
        return 0;
    }

    public float getRoll() {
        float roll = (mControls.getMode() == 1 || mControls.getMode() == 2) ? mControls.getRightAnalog_X() : mControls.getLeftAnalog_X();
        return ((roll * mControls.getDeadzone(roll)) + mControls.getRollTrim()) * mControls.getRollPitchFactor();
    }

    public float getPitch() {
        float pitch = (mControls.getMode() == 1 || mControls.getMode() == 3) ? mControls.getLeftAnalog_Y() : mControls.getRightAnalog_Y();
        return ((pitch * mControls.getDeadzone(pitch)) + mControls.getPitchTrim()) * mControls.getRollPitchFactor();
    }

    public float getYaw() {
        float yaw = 0;
        yaw = (mControls.getMode() == 1 || mControls.getMode() == 2) ? mControls.getLeftAnalog_X() : mControls.getRightAnalog_X();
        return yaw * mControls.getYawFactor() * mControls.getDeadzone(yaw);
    }

    /**
     * Disabled by default
     *
     * @return
     */
    public boolean isHover() {
        return false;
    }
}
