package se.bitcraze.crazyfliecontrol.prefs;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Created by Da-Jin on 4/18/2015.
 */
public class JoystickSizeSliderPreference extends DialogPreference {

    int percent;
    private TextView indicator;
    private SeekBar slider;

    public JoystickSizeSliderPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected View onCreateDialogView(){
        //Params for adding views to LinearLayout. heavyparams fills
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        LinearLayout.LayoutParams fillparams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,1);
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(6,6,6,6);

        slider = new SeekBar(getContext());
        indicator = new TextView(getContext());

        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                indicator.setText(progress+"%");
                percent=progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //Percent was set by onSetInitialValue. ChangeListener will be triggered here
        slider.setProgress(percent);

        //Add the views
        layout.addView(slider,fillparams);
        layout.addView(indicator,params);

        return layout;
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue){
        super.onSetInitialValue(restorePersistedValue,defaultValue);
        if(restorePersistedValue){
            percent = Integer.parseInt(getPersistedString("100"));
        } else {
            percent = (int)defaultValue;
            persistString(percent + "");
        }
    }

    @Override
    protected  void onDialogClosed(boolean positiveResult){
        if(positiveResult){
            persistString(percent + "");
        }
    }

}
