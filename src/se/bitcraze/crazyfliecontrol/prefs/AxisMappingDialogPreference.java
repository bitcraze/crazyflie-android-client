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

package se.bitcraze.crazyfliecontrol.prefs;

import java.util.List;

import se.bitcraze.crazyfliecontrol2.R;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.InputDevice.MotionRange;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnGenericMotionListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AxisMappingDialogPreference extends DialogPreference implements OnKeyListener, OnGenericMotionListener{

    private static final String TAG = "axisMappingDialogPreference";
    private TextView mValueTextView;
    private String mAxisName;

    public AxisMappingDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        super.onPrepareDialogBuilder(builder);

        builder.setOnKeyListener(this);
    }

    @Override
    protected View onCreateDialogView() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(6,6,6,6);

        TextView promptTextView = new TextView(getContext());
        promptTextView.setText(R.string.preferences_axis_mapping_dialog_text);
        promptTextView.setGravity(Gravity.CENTER_HORIZONTAL);

        mValueTextView = new TextView(getContext());
        mValueTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        mValueTextView.setGravity(Gravity.CENTER_HORIZONTAL);
        mValueTextView.setPadding(0, 12, 0, 12);


        mValueTextView.setOnGenericMotionListener(this);
        //TODO: is there an easier way to make this work?
        //motion events are not captured when view is not focusable
        mValueTextView.setFocusableInTouchMode(true);
        //if focus is not set, right analog stick events are only recognized after the left analog stick is moved!?!
        mValueTextView.requestFocus();

        layout.addView(promptTextView, params);
        layout.addView(mValueTextView, params);

        return layout;
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        super.onSetInitialValue(restorePersistedValue, defaultValue);
        if(restorePersistedValue) {
            mAxisName = getPersistedString((String) defaultValue);
        } else {
            mAxisName = (String) defaultValue;
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if(positiveResult) {
            persistString(mAxisName);
        }
        super.onDialogClosed(positiveResult);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        if(mAxisName == null || mAxisName.equals("")) {
            mValueTextView.setText("No axis");
        } else {
            mValueTextView.setText(mAxisName);
        }
    }

    @Override
    public boolean onGenericMotion(View v, MotionEvent event) {
        if ((event.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) != 0
                && event.getAction() == MotionEvent.ACTION_MOVE) {
            List<MotionRange> motionRanges = event.getDevice().getMotionRanges();
            for(MotionRange mr : motionRanges){
                int axis = mr.getAxis();
                if(event.getAxisValue(axis) > 0.5 || event.getAxisValue(axis) < -0.5){
                    Log.i(TAG, "Axis found: " + MotionEvent.axisToString(axis));
                    this.mAxisName = MotionEvent.axisToString(axis);
                    mValueTextView.setText(mAxisName);
                }
            }
        }else{
            Log.i(TAG, "Not a joystick event.");
        }
        return true;
    }

    @Override
    public boolean onKey(DialogInterface dialog, int pKeyCode, KeyEvent event) {
        if(pKeyCode == KeyEvent.KEYCODE_BACK) {
            dialog.dismiss();
        }
        return true;
    }

}