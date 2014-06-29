package se.bitcraze.crazyfliecontrol.controller;

import com.MobileAnarchy.Android.Widgets.Joystick.DualJoystickView;
import com.MobileAnarchy.Android.Widgets.Joystick.JoystickMovedListener;

public class TouchController extends Controller {

	private int resolution = 1000;

	private DualJoystickView joystickView;
	private float leftX;
	private float leftY;
	private float rightX;
	private float rightY;

	public TouchController(Controls controls, DualJoystickView joystickView) {
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
			leftX = (float) pan / resolution;
			leftY = (float) tilt / resolution;
			moved();
		}

		@Override
		public void OnReleased() {
			leftX = 0;
			leftY = 0;
			moved();
		}

		public void OnReturnedToCenter() {
			leftX = 0;
			leftY = 0;
			moved();
		}
	};

	private JoystickMovedListener _listenerRight = new JoystickMovedListener() {

		@Override
		public void OnMoved(int pan, int tilt) {
			rightX = (float) pan / resolution;
			rightY = (float) tilt / resolution;
			moved();
		}

		@Override
		public void OnReleased() {
			rightX = 0;
			rightY = 0;
			moved();
		}

		public void OnReturnedToCenter() {
			rightX = 0;
			rightY = 0;
			moved();
		}
	};

	private void moved() {
		thrust = (controls.getMode() == 1 || controls.getMode() == 3) ? rightY
				: leftY;
		roll = (controls.getMode() == 1 || controls.getMode() == 2) ? rightX
				: leftX;
		pitch = (controls.getMode() == 1 || controls.getMode() == 3) ? leftY
				: rightY;
		yaw = (controls.getMode() == 1 || controls.getMode() == 2) ? leftX
				: rightX;

		updateFlightData();
	}
}
