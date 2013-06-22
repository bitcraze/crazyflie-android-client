package se.bitcraze.crazyfliecontrol;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.widget.Toast;

public class PreferencesActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	public static final String KEY_PREF_RADIO_CHANNEL = "pref_radiochannel";
	public static final String KEY_PREF_RADIO_BANDWIDTH = "pref_radiobandwidth";
	public static final String KEY_PREF_MODE = "pref_mode";
	public static final String KEY_PREF_DEADZONE = "pref_deadzone";
	public static final String KEY_PREF_AFC_BOOL = "pref_afc_bool";
	public static final String KEY_PREF_AFC_SCREEN = "pref_afc_screen";
	public static final String KEY_PREF_MAX_ROLLPITCH_ANGLE = "pref_maxrollpitchangle";
	public static final String KEY_PREF_MAX_YAW_ANGLE = "pref_maxyawangle";
	public static final String KEY_PREF_MAX_THRUST = "pref_maxthrust";
	public static final String KEY_PREF_MIN_THRUST = "pref_minthrust";
	public static final String KEY_PREF_XMODE = "pref_xmode";
	public static final String KEY_PREF_RESET_AFC = "pref_reset_afc";

	private static final int RADIOCHANNEL_UPPER_LIMIT = 125; 
	private static final float DEADZONE_UPPER_LIMIT = 1.0f; 
	private static final int MAX_ROLLPITCH_ANGLE_UPPER_LIMIT = 50;
	private static final int MAX_YAW_ANGLE_UPPER_LIMIT = 500;
	private static final int MAX_THRUST_UPPER_LIMIT = 100;
	private static final int MIN_THRUST_UPPER_LIMIT = 50;

	private SharedPreferences sharedPreferences;
	
	private String radioChannelDefaultValue;
	private String modeDefaultValue;
	private String deadzoneDefaultValue;
	private String maxRollPitchAngleDefaultValue;
	private String maxYawAngleDefaultValue;
	private String maxThrustDefaultValue;
	private String minThrustDefaultValue;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
		
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		
		//Set initial summaries and get default values
		radioChannelDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_RADIO_CHANNEL, R.string.preferences_radio_channel_defaultValue);
		
        Preference radioBandwidthPref = findPreference(KEY_PREF_RADIO_BANDWIDTH);
        String radioBandwidthDefaultValue = getResources().getString(R.string.preferences_radio_bandwidth_defaultValue);
        String[] stringArray = getResources().getStringArray(R.array.radioBandwidthEntries);
    	String keyString = sharedPreferences.getString(KEY_PREF_RADIO_BANDWIDTH, radioBandwidthDefaultValue);
    	radioBandwidthPref.setSummary(stringArray[Integer.parseInt(keyString)]);

        modeDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_MODE, R.string.preferences_mode_defaultValue);
        deadzoneDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_DEADZONE, R.string.preferences_deadzone_defaultValue);
        
    	Preference afcScreenPref = findPreference(KEY_PREF_AFC_SCREEN);
    	afcScreenPref.setEnabled(sharedPreferences.getBoolean(KEY_PREF_AFC_BOOL, false));

        maxRollPitchAngleDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_MAX_ROLLPITCH_ANGLE, R.string.preferences_maxRollPitchAngle_defaultValue);
        maxYawAngleDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_MAX_YAW_ANGLE, R.string.preferences_maxYawAngle_defaultValue);
        maxThrustDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_MAX_THRUST, R.string.preferences_maxThrust_defaultValue);
        minThrustDefaultValue = setInitialSummaryAndReturnDefaultValue(KEY_PREF_MIN_THRUST, R.string.preferences_minThrust_defaultValue);

        Preference resetAfcPref = findPreference(KEY_PREF_RESET_AFC);
        resetAfcPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
			@Override
			public boolean onPreferenceClick(Preference preference) {
				resetPreference(KEY_PREF_MAX_ROLLPITCH_ANGLE, maxRollPitchAngleDefaultValue, null);
				resetPreference(KEY_PREF_MAX_YAW_ANGLE, maxYawAngleDefaultValue, null);
				resetPreference(KEY_PREF_MAX_THRUST, maxThrustDefaultValue, null);
				resetPreference(KEY_PREF_MIN_THRUST, minThrustDefaultValue, null);
				resetPreference(KEY_PREF_XMODE, false);
				return true;
			}
		});
        
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
        	setSummaryInt(key, radioChannelDefaultValue, RADIOCHANNEL_UPPER_LIMIT, "Radio channel");
        }
        if (key.equals(KEY_PREF_RADIO_BANDWIDTH)) {
        	Preference radioBandwidthPref = findPreference(key);
        	String[] stringArray = getResources().getStringArray(R.array.radioBandwidthEntries);
        	String keyString = sharedPreferences.getString(key, "");
        	radioBandwidthPref.setSummary(stringArray[Integer.parseInt(keyString)]);
        }
        if (key.equals(KEY_PREF_MODE)) {
            Preference modePref = findPreference(key);
            modePref.setSummary(sharedPreferences.getString(key, modeDefaultValue));
        }
        if (key.equals(KEY_PREF_DEADZONE)) {
        	Preference deadzonePref = findPreference(key);
    		try{
    			float deadzone = Float.parseFloat(sharedPreferences.getString(key, deadzoneDefaultValue));
    			if(deadzone < 0.0 || deadzone > DEADZONE_UPPER_LIMIT){
    				resetPreference(key, deadzoneDefaultValue, "Deadzone must be a float value between 0.0 and " + DEADZONE_UPPER_LIMIT + ".");
    			}
    		}catch(NumberFormatException nfe){
    			resetPreference(key, deadzoneDefaultValue, "Deadzone must be a float value between 0.0 and " + DEADZONE_UPPER_LIMIT + ".");
    		}       	
        	deadzonePref.setSummary(sharedPreferences.getString(key, ""));
        }
        if (key.equals(KEY_PREF_AFC_BOOL)){
        	Preference afcScreenPref = findPreference(KEY_PREF_AFC_SCREEN);
        	afcScreenPref.setEnabled(sharedPreferences.getBoolean(key, false));
        	if(!sharedPreferences.getBoolean(key, false)){
        		Toast.makeText(this, "Resetting to default values:\n" +
        							 "Max roll/pitch angle: " + maxRollPitchAngleDefaultValue + "\n" +
        							 "Max yaw angle: " + maxYawAngleDefaultValue + "\n" +
        							 "Max thrust: " + maxThrustDefaultValue + "\n" +
        							 "Min thrust: " + minThrustDefaultValue, Toast.LENGTH_LONG).show();
        	}else{
        		Toast.makeText(this, "You have been warned!", Toast.LENGTH_LONG).show();
        	}
        }
        if (key.equals(KEY_PREF_MAX_ROLLPITCH_ANGLE)) {
        	setSummaryInt(key, maxRollPitchAngleDefaultValue, MAX_ROLLPITCH_ANGLE_UPPER_LIMIT, "Max roll/pitch angle");
        }
        if (key.equals(KEY_PREF_MAX_YAW_ANGLE)) {
        	setSummaryInt(key, maxYawAngleDefaultValue, MAX_YAW_ANGLE_UPPER_LIMIT, "Max yaw angle");
        }
        if (key.equals(KEY_PREF_MAX_THRUST)) {
        	setSummaryInt(key, maxThrustDefaultValue, MAX_THRUST_UPPER_LIMIT, "Max thrust");
        }
        if (key.equals(KEY_PREF_MIN_THRUST)) {
        	setSummaryInt(key, minThrustDefaultValue, MIN_THRUST_UPPER_LIMIT, "Min thrust");
        }
        if (key.equals(KEY_PREF_XMODE)){
        	CheckBoxPreference pref = (CheckBoxPreference) findPreference(key);
        	pref.setChecked(sharedPreferences.getBoolean(key, false));
        }
    }
    
	private String setInitialSummaryAndReturnDefaultValue(String pKey, int pRDefaultValue){
        Preference pref = findPreference(pKey);
        String defaultValue = getResources().getString(pRDefaultValue);
        pref.setSummary(sharedPreferences.getString(pKey, defaultValue));
        return defaultValue;
	}
    
    private void setSummaryInt(String key, String pDefaultValue, int pUpperLimit, String pValueName){
    	Preference pref = findPreference(key);
    	try{
    		int newPrefValue = Integer.parseInt(sharedPreferences.getString(key, pDefaultValue));
    		if(newPrefValue < 0 || newPrefValue > pUpperLimit){
    			resetPreference(key, pDefaultValue, pValueName + " must be an integer value between 0 and " + pUpperLimit + ".");
    		}
    	}catch(NumberFormatException nfe){
    		resetPreference(key, pDefaultValue, pValueName + " must be an integer value between 0 and " + pUpperLimit + ".");
    	}       	
    	pref.setSummary(sharedPreferences.getString(key, ""));
    }
    
    private void resetPreference(String pKey, String pDefaultValue, String pErrorMessage){
    	if(pErrorMessage != null){
    		Toast.makeText(this, pErrorMessage + "\nResetting to default value " + pDefaultValue + ".", Toast.LENGTH_SHORT).show();
    	}
    	SharedPreferences.Editor editor = sharedPreferences.edit();
    	editor.putString(pKey, pDefaultValue);
    	editor.commit();
    }

    private void resetPreference(String pKey, boolean pDefaultValue){
    	SharedPreferences.Editor editor = sharedPreferences.edit();
    	editor.putBoolean(pKey, pDefaultValue);
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
