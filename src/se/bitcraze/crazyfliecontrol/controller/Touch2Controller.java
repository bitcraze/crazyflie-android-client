package se.bitcraze.crazyfliecontrol.controller;

import com.MobileAnarchy.Android.Widgets.Joystick.DualJoystickView;
import com.MobileAnarchy.Android.Widgets.Joystick.JoystickMovedListener;

public class Touch2Controller extends Controller {	
	
	private int mResolution = 1000;
	
	private DualJoystickView joystickView;
	 
	public Touch2Controller(Controls controls, DualJoystickView joystickView) {
		super(controls);
		this.joystickView = joystickView;
		this.joystickView.setMovementRange(mResolution, mResolution);
	}
	
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
            thrust = (float) tilt / mResolution;
            yaw = (float) pan / mResolution;
            updateFlightData();
        }

        @Override
        public void OnReleased() {
            thrust = 0;
            yaw = 0;
            updateFlightData();
        }

        public void OnReturnedToCenter() {
            thrust = 0;
            yaw = 0;
            updateFlightData();
        }
    };

    private JoystickMovedListener _listenerRight = new JoystickMovedListener() {

        @Override
        public void OnMoved(int pan, int tilt) {
        	pitch = (float) tilt / mResolution;
            roll = (float) pan / mResolution;
            updateFlightData();
        }

		@Override
        public void OnReleased() {
        	roll = 0;
            pitch = 0;
            updateFlightData();
        }

        public void OnReturnedToCenter() {
            roll = 0;
            pitch = 0;
            updateFlightData();
        }
    };
}
