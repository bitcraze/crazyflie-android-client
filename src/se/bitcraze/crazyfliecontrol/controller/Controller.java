package se.bitcraze.crazyfliecontrol.controller;

import se.bitcraze.crazyfliecontrol.FlyingDataEvent;

public abstract class Controller implements IController {
	private FlyingDataEvent flyingDataEvent;
	protected Controls controls;
	protected boolean mIsDisabled;
    
	private final int MAX_THRUST = 65535;

	public Controller(Controls controls) {
		this.controls = controls;
	}
	
	public float roll;
	public float pitch;
	public float yaw;
	public float thrust;
	
	public void setOnFlyingDataListener(FlyingDataEvent flyingDataListener) {
		flyingDataEvent = flyingDataListener;
	}
	
    void updateFlightData() {
        flyingDataEvent.flyingDataEvent(getPitch(), getRoll(), getThrust(), getYaw());		
	}
    
    public void enable() {
		mIsDisabled = false;        
	}

    public void disable() {
        mIsDisabled = true;
    }
    
    public float getRoll() {
        return (roll + controls.getRollTrim()) * controls.getMaxRollPitchAngle() * controls.getDeadzone(roll);
    }

    public float getPitch() {    	
        return (pitch + controls.getPitchTrim()) * controls.getMaxRollPitchAngle() * controls.getDeadzone(pitch);
    }
    
	public float getYaw() {
		return yaw * controls.getMaxYawAngle() * controls.getDeadzone(yaw);
	}

    public float getThrust() {
    	//thrust >= 0 and thrust <= 1
        if (thrust > controls.getDeadzone()) {
           float addThrust = 0;
           if ((controls.getMaxThrust() - controls.getMinThrust()) < 0) {
               addThrust = 0; // do not allow negative values
           } else {
               addThrust = (controls.getMaxThrust() - controls.getMinThrust());
           }
           return (controls.getMinThrust() + (thrust * addThrust))/100 * MAX_THRUST;
        } else {
            return 0;
        }
    }
}
