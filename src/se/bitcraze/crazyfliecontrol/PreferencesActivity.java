package se.bitcraze.crazyfliecontrol;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceActivity;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.widget.Toast;

public class PreferencesActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	public static final String KEY_PREF_RADIO_CHANNEL = "pref_radiochannel";
	public static final String KEY_PREF_RADIO_BANDWIDTH = "pref_radiobandwidth";
	public static final String KEY_PREF_MODE = "pref_mode";
	public static final String KEY_PREF_DEADZONE = "pref_deadzone";

	private SharedPreferences sharedPreferences;
	
	private String radioChannelDefaultValue;
	private String deadzoneDefaultValue;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
		
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		
        Preference radioChannelPref = findPreference(KEY_PREF_RADIO_CHANNEL);
        radioChannelDefaultValue = getResources().getString(R.string.preferences_radio_channel_defaultvalue);
        radioChannelPref.setSummary(sharedPreferences.getString(KEY_PREF_RADIO_CHANNEL, radioChannelDefaultValue));
		
        Preference radioBandwidthPref = findPreference(KEY_PREF_RADIO_BANDWIDTH);
        String radioBandwidthDefaultValue = getResources().getString(R.string.preferences_radio_bandwidth_defaultvalue);
        String[] stringArray = getResources().getStringArray(R.array.radioBandwidthEntries);
    	String keyString = sharedPreferences.getString(KEY_PREF_RADIO_BANDWIDTH, radioBandwidthDefaultValue);
    	radioBandwidthPref.setSummary(stringArray[Integer.parseInt(keyString)]);

        Preference modePref = findPreference(KEY_PREF_MODE);
        String modeDefaultValue = getResources().getString(R.string.preferences_mode_defaultvalue);
        modePref.setSummary(sharedPreferences.getString(KEY_PREF_MODE, modeDefaultValue));

        Preference deadzonePref = findPreference(KEY_PREF_DEADZONE);
        deadzoneDefaultValue = getResources().getString(R.string.preferences_deadzone_defaultvalue);
        deadzonePref.setSummary(sharedPreferences.getString(KEY_PREF_DEADZONE, deadzoneDefaultValue));
        
		setupActionBar();
	}
	
	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	// Set summary to be the user-description for the selected value
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(KEY_PREF_RADIO_CHANNEL)) {
            Preference radioChannelPref = findPreference(key);
    		try{
    			int radioChannel = Integer.parseInt(sharedPreferences.getString(key, radioChannelDefaultValue));
    			if(radioChannel < 0 || radioChannel > 125){
    				resetPreference(key, radioChannelDefaultValue, "Radio channel must be an integer value between 0 and 125.");
    			}
    		}catch(NumberFormatException nfe){
    			resetPreference(key, radioChannelDefaultValue, "Radio channel must be an integer value between 0 and 125.");
    		}  
            radioChannelPref.setSummary(sharedPreferences.getString(key, ""));
        }
        if (key.equals(KEY_PREF_RADIO_BANDWIDTH)) {
        	Preference radioBandwidthPref = findPreference(key);
        	String[] stringArray = getResources().getStringArray(R.array.radioBandwidthEntries);
        	String keyString = sharedPreferences.getString(key, "");
        	radioBandwidthPref.setSummary(stringArray[Integer.parseInt(keyString)]);
        }
        if (key.equals(KEY_PREF_MODE)) {
            Preference modePref = findPreference(key);
            modePref.setSummary(sharedPreferences.getString(key, ""));
        }
        if (key.equals(KEY_PREF_DEADZONE)) {
        	Preference deadzonePref = findPreference(key);
    		try{
    			float deadzone = Float.parseFloat(sharedPreferences.getString(key, deadzoneDefaultValue));
    			if(deadzone < 0.0 || deadzone > 1.0){
    				resetPreference(key, deadzoneDefaultValue, "Deadzone must be a float value between 0.0 and 1.0.");
    			}
    		}catch(NumberFormatException nfe){
    			resetPreference(key, deadzoneDefaultValue, "Deadzone must be a float value between 0.0 and 1.0.");
    		}       	
        	deadzonePref.setSummary(sharedPreferences.getString(key, ""));
        }
    }
    
    private void resetPreference(String key, String defaultValue, String errorMessage){
    	Toast.makeText(this, errorMessage + "\nResetting to default value " + defaultValue + ".", Toast.LENGTH_SHORT).show();
    	SharedPreferences.Editor editor = sharedPreferences.edit();
    	editor.putString(key, defaultValue);
    	editor.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

}
