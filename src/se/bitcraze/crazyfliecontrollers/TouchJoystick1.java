package se.bitcraze.crazyfliecontrollers;

import se.bitcraze.crazyfliecontrol.Controls;

import com.MobileAnarchy.Android.Widgets.Joystick.DualJoystickView;
import com.MobileAnarchy.Android.Widgets.Joystick.JoystickMovedListener;

public class TouchJoystick1 extends Controller {	
	
	private int resolution = 1000;
	
	private DualJoystickView joystickView;
	 
	public TouchJoystick1(Controls controls, DualJoystickView joystickView) {
		super(controls);
		this.joystickView = joystickView;
		this.joystickView.setMovementRange(resolution, resolution);
	}

    @Override    
	public void enable() {
		joystickView.setOnJostickMovedListener(_listenerLeft, _listenerRight);
	}
	
	@Override
	public void disable() {
		joystickView.setOnJostickMovedListener(null, null);
		
	}

    private JoystickMovedListener _listenerLeft = new JoystickMovedListener() {

        @Override
        public void OnMoved(int pan, int tilt) {
            pitch = (float) tilt / resolution;
            yaw = (float) pan / resolution;
            updateFlightData();
        }

        @Override
        public void OnReleased() {
            pitch = 0;
            yaw = 0;
            updateFlightData();
        }

        public void OnReturnedToCenter() {
            pitch = 0;
            yaw = 0;
            updateFlightData();
        }
    };

    private JoystickMovedListener _listenerRight = new JoystickMovedListener() {

        @Override
        public void OnMoved(int pan, int tilt) {
            thrust = (float) tilt / resolution;
            roll = (float) pan / resolution;
            updateFlightData();
        }

		@Override
        public void OnReleased() {
        	thrust = 0;
            roll = 0;
            updateFlightData();
        }

        public void OnReturnedToCenter() {
            thrust = 0;
            roll = 0;
            updateFlightData();
        }
    };
}
