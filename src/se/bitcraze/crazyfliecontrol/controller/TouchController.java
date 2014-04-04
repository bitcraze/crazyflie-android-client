package se.bitcraze.crazyfliecontrol.controller;

import se.bitcraze.crazyfliecontrol.Controls;
import se.bitcraze.crazyfliecontrol.MainActivity;
import android.widget.Toast;

import com.MobileAnarchy.Android.Widgets.Joystick.JoystickMovedListener;

public class TouchController extends AbstractController {

	private int mResolution = 1000;
	private boolean mIsDisabled;
	private MainActivity mActivity;
	
	public TouchController(Controls controls, MainActivity activity) {
		super(controls);
		mActivity = activity;
        mActivity.getJoysticksView().setMovementRange(mResolution, mResolution);
        enable();
	}

	public void enable(){
		this.mIsDisabled = false;
        mActivity.getJoysticksView().setOnJostickMovedListener(_listenerLeft, _listenerRight);
        Toast.makeText(mActivity, "Using on-screen controller", Toast.LENGTH_SHORT).show();
	}
	
    public void disable() {
        Toast.makeText(mActivity, "Using external controller", Toast.LENGTH_SHORT).show();
        mActivity.getJoysticksView().setOnJostickMovedListener(null, null);
        this.mIsDisabled = true;
    }

    public boolean isDisabled() {
        return mIsDisabled;
    }	
	
    private JoystickMovedListener _listenerRight = new JoystickMovedListener() {

        @Override
        public void OnMoved(int pan, int tilt) {
            mControls.setRightAnalogY((float) tilt / mResolution);
            mControls.setRightAnalogX((float) pan / mResolution);

            mActivity.updateFlightData();
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

            mActivity.updateFlightData();
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
