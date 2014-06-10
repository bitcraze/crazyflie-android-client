package se.bitcraze.crazyfliecontrollers;

import android.view.MotionEvent;
import se.bitcraze.crazyfliecontrol.Controls;

public class Joystick extends Controller {
	
	public Joystick(Controls controls) {
		super(controls);
	}
	
	public void dealWithMotionEvent(MotionEvent event){
        // default axis are set to work with PS3 controller
        roll = (float) (event.getAxisValue(controls.getRightAnalogXAxis()));
        thrust = (float) (event.getAxisValue(controls.getRightAnalogYAxis())) * -1; //invert axis
        pitch = (float) (event.getAxisValue(controls.getLeftAnalogYAxis())) * -1;  //invert axis

        if(controls.useSplitAxisYaw()){
            yaw = (float) (event.getAxisValue(controls.getSplitAxisYawRightAxis())) - (float) (event.getAxisValue(controls.getSplitAxisYawRightAxis()));
        }else{
            yaw = (float) (event.getAxisValue(controls.getLeftAnalogXAxis()));
        }
        updateFlightData();
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
