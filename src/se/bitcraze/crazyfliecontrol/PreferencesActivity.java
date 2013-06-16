package se.bitcraze.crazyfliecontrol;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceActivity;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;

public class PreferencesActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	public static final String KEY_PREF_RADIO_CHANNEL = "pref_radiochannel";
	public static final String KEY_PREF_RADIO_BANDWIDTH = "pref_radiobandwidth";
	public static final String KEY_PREF_MODE = "pref_mode";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		
        Preference radioChannelPref = findPreference(KEY_PREF_RADIO_CHANNEL);
        String radioChannelDefaultValue = getResources().getString(R.string.preferences_radio_channel_defaultvalue);
        radioChannelPref.setSummary(sharedPreferences.getString(KEY_PREF_RADIO_CHANNEL, radioChannelDefaultValue));
		
        Preference radioBandwidthPref = findPreference(KEY_PREF_RADIO_BANDWIDTH);
        String radioBandwidthDefaultValue = getResources().getString(R.string.preferences_radio_bandwidth_defaultvalue);
        String[] stringArray = getResources().getStringArray(R.array.radioBandwidthEntries);
    	String keyString = sharedPreferences.getString(KEY_PREF_RADIO_BANDWIDTH, radioBandwidthDefaultValue);
    	radioBandwidthPref.setSummary(stringArray[Integer.parseInt(keyString)]);

        Preference modePref = findPreference(KEY_PREF_MODE);
        String modeDefaultValue = getResources().getString(R.string.preferences_mode_defaultvalue);
        modePref.setSummary(sharedPreferences.getString(KEY_PREF_MODE, modeDefaultValue));
        
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
