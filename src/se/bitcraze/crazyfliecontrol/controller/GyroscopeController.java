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

    Float meterMax;

    private float mSensorRoll = 0;
    private float mSensorPitch = 0;

    public GyroscopeController(Controls controls, MainActivity activity, DualJoystickView dualJoystickView, SensorManager sensorManager) {
        super(controls, activity, dualJoystickView);
        mSensorManager = sensorManager;
    }

    @Override
    public void enable() {
        super.enable();
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
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
  	public void onAccuracyChanged(Sensor sensor, int arg1) {
   		Float sensorMax = sensor.getMaximumRange();
   		//9.81 means the maximum rotation
   		meterMax = (sensorMax/(float) 100.0) * (float) 9.81;
   	}

   	@Override
   	public void onSensorChanged(SensorEvent event) {
   		mSensorPitch = (event.values[0] / meterMax ) * -1;
   		mSensorRoll = event.values[1] / meterMax;
   		updateFlightData();
   	}

    // overwrite getRoll() and getPitch() to only use values from gyro sensors
    public float getRoll() {
        float roll = mSensorRoll;
        
        //Filter the overshoot
        roll = (float) Math.min(1.0, Math.max(-1, roll+mControls.getRollTrim()));

        return (roll + mControls.getRollTrim()) * mControls.getRollPitchFactor() * mControls.getDeadzone(roll);
    }

    public float getPitch() {
        float pitch = mSensorPitch;
        
        //Filter the overshoot
        pitch = (float) Math.min(1.0, Math.max(-1, pitch+mControls.getPitchTrim()));
        
        return (pitch + mControls.getPitchTrim()) * mControls.getRollPitchFactor() * mControls.getDeadzone(pitch);
    }

}
