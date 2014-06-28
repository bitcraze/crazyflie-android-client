package se.bitcraze.crazyfliecontrol.controller;

import android.view.MotionEvent;

public class Joystick extends Controller {
	
	public Joystick(Controls controls) {
		super(controls);
	}
	
	public void dealWithMotionEvent(MotionEvent event){
        // default axis are set to work with PS3 controller
        roll = (float) (event.getAxisValue(controls.getRightAnalogXAxis()));
        thrust = (float) (event.getAxisValue(controls.getRightAnalogYAxis())) * (needsInverting(controls.getRightAnalogXAxis()) ? -1 : 1);
        
        pitch = (float) (event.getAxisValue(controls.getLeftAnalogYAxis())) * -1;  //invert axis
        pitch = (float) (event.getAxisValue(controls.getLeftAnalogYAxis())) * (needsInverting(controls.getLeftAnalogYAxis()) ? -1 : 1);
        if(controls.useSplitAxisYaw()){
            yaw = (float) (event.getAxisValue(controls.getSplitAxisYawRightAxis())) - (float) (event.getAxisValue(controls.getSplitAxisYawRightAxis()));
        }else{
            yaw = (float) (event.getAxisValue(controls.getLeftAnalogXAxis()));
        }
        updateFlightData();
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

	@Override
	public void enable() {
		// TODO Auto-generated method stub
	}

	@Override
	public void disable() {
		// TODO Auto-generated method stub
	}

}
