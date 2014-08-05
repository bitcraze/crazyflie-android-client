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
public class GyroscopeController extends TouchController implements SensorEventListener {

    private SensorManager mSensorManager;

    private int AMPLIFICATION = 2;

    private float mSensorRoll = 0;;
    private float mSensorPitch = 0;;

    public GyroscopeController(Controls controls, MainActivity activity, DualJoystickView dualJoystickView, SensorManager sensorManager) {
        super(controls, activity, dualJoystickView);
        mSensorManager = sensorManager;
    }

    @Override
    public void enable() {
        super.enable();
        //TODO: check which sensor is available
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void disable() {
        super.disable();
        mSensorManager.unregisterListener(this);
    }

    public String getControllerName() {
        return "gyroscope controller";
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // amplifying the sensitivity.
        mSensorRoll = event.values[0] * AMPLIFICATION;
        mSensorPitch = event.values[1] * AMPLIFICATION;
        updateFlightData();
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
