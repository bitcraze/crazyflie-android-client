package se.bitcraze.crazyfliecontrollers;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.MobileAnarchy.Android.Widgets.Joystick.DualJoystickView;
import com.MobileAnarchy.Android.Widgets.Joystick.JoystickMovedListener;

public class Gyroscope extends Controller implements SensorEventListener {

    private SensorManager mSensorManager;
    private DualJoystickView joystickView;
    private int resolution = 1000;

    public Gyroscope(Controls controls, SensorManager sensorManager, DualJoystickView dualJoystickView) {
        super(controls);
        this.joystickView = dualJoystickView;
        this.joystickView.setMovementRange(resolution, resolution);
        mSensorManager = sensorManager;
        Log.d("e: ","inited");
    }

    @Override
    public void enable() {
        joystickView.setOnJostickMovedListener(_listenerLeft, _listenerRight);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void disable() {
        mSensorManager.unregisterListener(this);
        joystickView.setOnJostickMovedListener(null, null);
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // amplifying the sensitivity.    	
        pitch = (event.values[0] / 10 ) * -1;
        roll = event.values[1] / 10;       
        updateFlightData();
    }

    private JoystickMovedListener _listenerRight = new JoystickMovedListener() {

        @Override
        public void OnMoved(int pan, int tilt) {
            yaw = (float) pan / resolution;
            updateFlightData();
        }

        @Override
        public void OnReleased() {
            yaw=0;
        }

        public void OnReturnedToCenter() {
            yaw=0;
        }
    };

    private JoystickMovedListener _listenerLeft = new JoystickMovedListener() {

        @Override
        public void OnMoved(int pan, int tilt) {
            thrust = (float) tilt / resolution;
            updateFlightData();
        }

        @Override
        public void OnReleased() {
            thrust = 0;
        }

        public void OnReturnedToCenter() {
            thrust = 0;
        }
    };

}
