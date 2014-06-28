package se.bitcraze.crazyfliecontrollers;

import com.MobileAnarchy.Android.Widgets.Joystick.DualJoystickView;
import com.MobileAnarchy.Android.Widgets.Joystick.JoystickMovedListener;

public class TouchJoystick3 extends Controller {	
	
	private int mResolution = 1000;
	
	private DualJoystickView joystickView;
	 
	public TouchJoystick3(Controls controls, DualJoystickView joystickView) {
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
            pitch = (float) tilt / mResolution;
            roll = (float) pan / mResolution;
            updateFlightData();
        }

        @Override
        public void OnReleased() {
            pitch = 0;
            roll = 0;
            updateFlightData();
        }

        public void OnReturnedToCenter() {
            pitch = 0;
            roll = 0;
            updateFlightData();
        }
    };
	
    private JoystickMovedListener _listenerRight = new JoystickMovedListener() {

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
}