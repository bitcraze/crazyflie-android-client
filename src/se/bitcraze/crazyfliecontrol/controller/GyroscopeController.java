package se.bitcraze.crazyfliecontrol.controller;

import se.bitcraze.crazyfliecontrol.MainActivity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.MobileAnarchy.Android.Widgets.Joystick.DualJoystickView;

/**
 * The GyroscopeController extends the TouchController and uses the gyroscope sensors 
 * of the device to control the roll and pitch values.
 * The yaw and thrust values are still controlled by the touch controls according
 * to the chosen "mode" setting.
 * 
 */
public class GyroscopeController extends TouchController {

    private SensorManager mSensorManager;
    private SensorEventListener seListener = null;

    private float mSensorRoll = 0;;
    private float mSensorPitch = 0;;

    public GyroscopeController(Controls controls, MainActivity activity, DualJoystickView dualJoystickView, SensorManager sensorManager) {
        super(controls, activity, dualJoystickView);
        mSensorManager = sensorManager;
        seListener = new RotationVectorListener();
    }

    @Override
    public void enable() {
        super.enable();
        mSensorManager.registerListener(seListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void disable() {
        super.disable();
        mSensorManager.unregisterListener(seListener);
    }

    public String getControllerName() {
        return "gyroscope controller";
    }

    class RotationVectorListener implements SensorEventListener {
    	private final int AMPLIFICATION = 2;

    	@Override
    	public void onAccuracyChanged(Sensor arg0, int arg1) {
    	}

    	@Override
    	public void onSensorChanged(SensorEvent event) {
    		//amplifying the sensitivity.
    		mSensorRoll = event.values[0] * AMPLIFICATION;
    		mSensorPitch = event.values[1] * AMPLIFICATION;
    		updateFlightData();
    	}
    }

    // overwrite getRoll() and getPitch() to only use values from gyro sensors
    public float getRoll() {
        float roll = mSensorRoll;
        return (roll + mControls.getRollTrim()) * mControls.getRollPitchFactor() * mControls.getDeadzone(roll);
    }

    public float getPitch() {
        float pitch = mSensorPitch;
        return (pitch + mControls.getPitchTrim()) * mControls.getRollPitchFactor() * mControls.getDeadzone(pitch);
    }

}
