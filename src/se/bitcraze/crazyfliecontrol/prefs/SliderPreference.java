package se.bitcraze.crazyfliecontrol.prefs;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import se.bitcraze.crazyfliecontrol2.R;

/**
 * Created by Da-Jin on 4/18/2015.
 */
public class SliderPreference extends Preference {

    float setting,min,max,stepSize;
    int steps;
    private SeekBar slider;
    private TextView indicator;

    public SliderPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.slider_preference);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SliderPreference, 0, 0);

        min=a.getFloat(R.styleable.SliderPreference_lowerLimit,0);
        max=a.getFloat(R.styleable.SliderPreference_upperLimit,100);
        steps=a.getInt(R.styleable.SliderPreference_steps, (int) max);

        stepSize=(max-min)/steps;
    }

    @Override
    protected View onCreateView( ViewGroup parent )
    {
        LayoutInflater li = (LayoutInflater)getContext().getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        return li.inflate( R.layout.slider_preference, parent, false);
    }

    @Override
    protected void onBindView(View v){
        super.onBindView(v);
        slider = (SeekBar)v.findViewById(R.id.sliderSetting);
        indicator = (TextView)v.findViewById(R.id.summary);

        ((TextView)v.findViewById(R.id.slider_pref_title)).setText(getTitle());
        slider.setMax(steps);

        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setting=progress*stepSize;
                indicator.setText(setting+"");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                persistString(setting+"");
            }
        });

        //Percent was set by onSetInitialValue. ChangeListener will be triggered here
        slider.setProgress((int) (setting/stepSize));
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue){
        Log.d("Slide","onSetInitialValue");
        super.onSetInitialValue(restorePersistedValue,defaultValue);

        if(restorePersistedValue){
            setting = Float.parseFloat(getPersistedString("100"));
            Log.d("Slider", setting+"");
        } else {
            setting = (float) defaultValue;
            persistString(setting + "");
        }
    }
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getFloat(index, max);
    }
}
