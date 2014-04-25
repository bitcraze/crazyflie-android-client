package se.bitcraze.crazyfliecontrol.controller;

import se.bitcraze.crazyfliecontrol.MainActivity;

import com.MobileAnarchy.Android.Widgets.Joystick.DualJoystickView;
import com.MobileAnarchy.Android.Widgets.Joystick.JoystickMovedListener;

public class TouchController extends AbstractController {

	private int mResolution = 1000;

	private DualJoystickView dualJoystickView;
	
	public TouchController(Controls controls, MainActivity activity, DualJoystickView dualJoystickview) {
		super(controls, activity);
		this.dualJoystickView = dualJoystickview;
        this.dualJoystickView.setMovementRange(mResolution, mResolution);
        enable();
	}

	@Override
	public void enable(){
		super.enable();
		this.dualJoystickView.setOnJostickMovedListener(_listenerLeft, _listenerRight);
	}
	
	@Override
    public void disable() {
    	super.disable();
    	this.dualJoystickView.setOnJostickMovedListener(null, null);
    }
	
    public String getControllerName(){
    	return "touch controller";
    }
	
    private JoystickMovedListener _listenerRight = new JoystickMovedListener() {

        @Override
        public void OnMoved(int pan, int tilt) {
            mControls.setRightAnalogY((float) tilt / mResolution);
            mControls.setRightAnalogX((float) pan / mResolution);

            updateFlightData();
        }

        @Override
        public void OnReleased() {
            // Log.i("Joystick-Right", "Release");
            mControls.setRightAnalogY(0);
            mControls.setRightAnalogX(0);
        }

        public void OnReturnedToCenter() {
            // Log.i("Joystick-Right", "Center");
            mControls.setRightAnalogY(0);
            mControls.setRightAnalogX(0);
        }
    };

    private JoystickMovedListener _listenerLeft = new JoystickMovedListener() {

        @Override
        public void OnMoved(int pan, int tilt) {
            mControls.setLeftAnalogY((float) tilt / mResolution);
            mControls.setLeftAnalogX((float) pan / mResolution);

            updateFlightData();
        }

        @Override
        public void OnReleased() {
            mControls.setLeftAnalogY(0);
            mControls.setLeftAnalogX(0);
        }

        public void OnReturnedToCenter() {
            mControls.setLeftAnalogY(0);
            mControls.setLeftAnalogX(0);
        }
    };
	
}
