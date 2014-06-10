package se.bitcraze.crazyfliecontrollers;

import se.bitcraze.crazyfliecontrol.Controls;
import se.bitcraze.crazyfliecontrol.FlyingDataEvent;

public abstract class Controller {
	private FlyingDataEvent flyingDataEvent;
	protected Controls controls;

	public Controller(Controls controls) {
		this.controls = controls;		
	}
	
	public float roll;
	public float pitch;
	public float yaw;
	public float thrust;
	
	public abstract void enable();
	public abstract void disable();
	
	public void setOnFlyingDataListener(FlyingDataEvent flyingDataListener) {
		flyingDataEvent = flyingDataListener;
	}
	
    void updateFlightData() {
        flyingDataEvent.flyingDataEvent(getPitch(), getRoll(), getThrust(), getYaw());		
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
           return (controls.getMinThrust() + (thrust * addThrust))/100 * controls.MAX_THRUST;
        } else {
            return 0;
        }
    }
}
