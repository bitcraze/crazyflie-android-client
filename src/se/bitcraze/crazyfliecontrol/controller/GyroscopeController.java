/**
 *    ||          ____  _ __
 * +------+      / __ )(_) /_______________ _____  ___
 * | 0xBC |     / __  / / __/ ___/ ___/ __ `/_  / / _ \
 * +------+    / /_/ / / /_/ /__/ /  / /_/ / / /_/  __/
 *  ||  ||    /_____/_/\__/\___/_/   \__,_/ /___/\___/
 *
 * Copyright (C) 2013 Bitcraze AB
 *
 * Crazyflie Nano Quadcopter Client
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */

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
    private Sensor mSensor = null;
    private SensorEventListener mSeListener = null;

    private float mSensorRoll = 0;
    private float mSensorPitch = 0;

    public GyroscopeController(Controls controls, MainActivity activity, DualJoystickView dualJoystickView, SensorManager sensorManager) {
        super(controls, activity, dualJoystickView);
        mSensorManager = sensorManager;

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null) {
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            mSeListener = new RotationVectorListener();
        } else {
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mSeListener = new AccelerometerListener();
        }
    }

    @Override
    public void enable() {
        super.enable();
        mSensorManager.registerListener(mSeListener, mSensor, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void disable() {
        mSensorRoll = 0;
        mSensorPitch = 0;
        mSensorManager.unregisterListener(mSeListener);
        super.disable();
    }

    public String getControllerName() {
        return "gyroscope controller";
    }

    class AccelerometerListener implements SensorEventListener {
        //It divide back the 90 degree.
        private final float AMPLIFICATION = 1.5f;

        @Override
        public void onAccuracyChanged(Sensor sensor, int arg1) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            float d = (float) Math.max(Math.sqrt(event.values[0] * event.values[0] + event.values[1] * event.values[1] + event.values[2] * event.values[2]),0.001);
            mSensorPitch = event.values[0] / d * -1f * AMPLIFICATION;
            mSensorRoll = event.values[1] / d * AMPLIFICATION;
            updateFlightData();
        }
    }

    class RotationVectorListener implements SensorEventListener {
        private final int AMPLIFICATION = 2;

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
