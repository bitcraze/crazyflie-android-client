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

package se.bitcraze.crazyfliecontrol2;

import java.math.BigDecimal;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Compound component that groups together flight data UI elements
 *
 */
public class FlightDataView extends LinearLayout {

    private TextView mTextView_pitch;
    private TextView mTextView_roll;
    private TextView mTextView_thrust;
    private TextView mTextView_yaw;

    public FlightDataView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setOrientation(LinearLayout.HORIZONTAL);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_flight_data, this, true);

        mTextView_pitch = (TextView) findViewById(R.id.pitch);
        mTextView_roll = (TextView) findViewById(R.id.roll);
        mTextView_thrust = (TextView) findViewById(R.id.thrust);
        mTextView_yaw = (TextView) findViewById(R.id.yaw);
        //initialize
        mTextView_pitch.setText(format(R.string.pitch, 0.0));
        mTextView_roll.setText(format(R.string.roll, 0.0));
        mTextView_thrust.setText(format(R.string.thrust, 0.0));
        mTextView_yaw.setText(format(R.string.yaw, 0.0));
    }

    public FlightDataView(Context context) {
      this(context, null);
    }

    public void updateFlightData(float pitch, float roll, float thrust, float yaw) {
        mTextView_pitch.setText(format(R.string.pitch, round(pitch)));
        mTextView_roll.setText(format(R.string.roll, round(roll)));
        mTextView_thrust.setText(format(R.string.thrust, round(thrust)));
        mTextView_yaw.setText(format(R.string.yaw, round(yaw)));
    }

    private String format(int identifier, Object o){
        return String.format(getResources().getString(identifier), o);
    }

    public static double round(double unrounded) {
        BigDecimal bd = new BigDecimal(unrounded);
        BigDecimal rounded = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
        return rounded.doubleValue();
    }

}
