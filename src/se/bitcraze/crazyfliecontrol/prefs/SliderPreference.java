package se.bitcraze.crazyfliecontrol.prefs;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import java.math.BigDecimal;
import java.math.MathContext;

import se.bitcraze.crazyfliecontrol2.R;

/**
 * Created by Da-Jin on 4/18/2015.
 */
public class SliderPreference extends Preference {

    float min, max, setting;
    BigDecimal stepSize;
    int steps;
    private SeekBar slider;
    private TextView indicator;
    MathContext mc;
    boolean round;

    public SliderPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.slider_preference);

        mc = new MathContext(3);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SliderPreference, 0, 0);

        min = a.getFloat(R.styleable.SliderPreference_lowerLimit, 0);
        max = a.getFloat(R.styleable.SliderPreference_upperLimit, 100);
        steps = a.getInt(R.styleable.SliderPreference_steps, (int) max);
        round = a.getBoolean(R.styleable.SliderPreference_wholeNumber, false);

        stepSize = BigDecimal.valueOf(max).subtract(BigDecimal.valueOf(min)).divide(BigDecimal.valueOf(steps), mc);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return li.inflate(R.layout.slider_preference, parent, false);
    }

    @Override
    protected void onBindView(View v) {
        super.onBindView(v);
        slider = (SeekBar) v.findViewById(R.id.sliderSetting);
        indicator = (TextView) v.findViewById(R.id.summary);

        ((TextView) v.findViewById(R.id.slider_pref_title)).setText(getTitle());
        slider.setMax(steps);

        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setting = stepSize.multiply(BigDecimal.valueOf(progress)).add(BigDecimal.valueOf(min)).round(mc).floatValue();
                indicator.setText(settingString());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                persistString(settingString());
            }
        });

        //Percent was set by onSetInitialValue. ChangeListener will be triggered here
        slider.setProgress((int) ((setting - min) / stepSize.floatValue()));
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        super.onSetInitialValue(restorePersistedValue, defaultValue);

        if (restorePersistedValue) {
            setting = Float.parseFloat(getPersistedString("100"));
        } else {
            setting = Float.parseFloat((String) defaultValue);
            persistString(settingString());
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    private String settingString(){
        if(round){
            return (int)setting + "";
        }else{
            return setting+"";
        }
    }

    @Override
    public void setSummary(CharSequence summary) {
        if(slider!=null) {
            slider.setProgress((int) ((Float.parseFloat(summary.toString()) - min) / stepSize.floatValue()));
        }
    }
}
