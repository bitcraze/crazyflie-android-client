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

package se.bitcraze.crazyfliecontrol;

import java.io.IOException;
import java.util.Locale;

import se.bitcraze.crazyfliecontrol.SelectConnectionDialogFragment.SelectCrazyflieDialogListener;
import se.bitcraze.crazyfliecontrol.controller.Controller;
import se.bitcraze.crazyfliecontrol.controller.Controls;
import se.bitcraze.crazyfliecontrol.controller.Gyroscope;
import se.bitcraze.crazyfliecontrol.controller.Joystick;
import se.bitcraze.crazyfliecontrol.controller.TouchJoystick1;
import se.bitcraze.crazyfliecontrol.controller.TouchJoystick2;
import se.bitcraze.crazyfliecontrol.controller.TouchJoystick3;
import se.bitcraze.crazyfliecontrol.controller.TouchJoystick4;
import se.bitcraze.crazyflielib.ConnectionAdapter;
import se.bitcraze.crazyflielib.CrazyradioLink;
import se.bitcraze.crazyflielib.CrazyradioLink.ConnectionData;
import se.bitcraze.crazyflielib.Link;
import se.bitcraze.crazyflielib.crtp.CommanderPacket;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.hardware.SensorManager;

import com.MobileAnarchy.Android.Widgets.Joystick.DualJoystickView;

public class MainActivity extends Activity implements FlyingDataEvent, OnCheckedChangeListener {

    private static final String TAG = "CrazyflieControl";
   
    private Controller controller;
    private FlightDataView mFlightDataView;

    private CrazyradioLink mCrazyradioLink;

    private SharedPreferences mPreferences;

    private String mRadioChannelDefaultValue;
    private String mRadioDatarateDefaultValue;

    private boolean mDoubleBackToExitPressedOnce = false;

    private Thread mSendJoystickDataThread;

    private String[] mDatarateStrings;

    private Controls mControls;

    private SoundPool mSoundPool;
    private boolean mLoaded;
    private int mSoundConnect;
    private int mSoundDisconnect;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setDefaultPreferenceValues();

        mControls = new Controls(this, mPreferences);
        mControls.setDefaultPreferenceValues(getResources());

        
        mFlightDataView = (FlightDataView) findViewById(R.id.flightdataview);

        ((ToggleButton) findViewById(R.id.hovermode)).setOnCheckedChangeListener(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(this.getPackageName()+".USB_PERMISSION");
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filter);

        initializeSounds();
    }

    private void initializeSounds() {
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        // Load sounds
        mSoundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        mSoundPool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                mLoaded = true;
            }
        });
        mSoundConnect = mSoundPool.load(this, R.raw.proxima, 1);
        mSoundDisconnect = mSoundPool.load(this, R.raw.tejat, 1);
    }

    private void setDefaultPreferenceValues(){
        // Set default preference values
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        // Initialize preferences
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        mRadioChannelDefaultValue = getString(R.string.preferences_radio_channel_defaultValue);
        mRadioDatarateDefaultValue = getString(R.string.preferences_radio_datarate_defaultValue);

        mDatarateStrings = getResources().getStringArray(R.array.radioDatarateEntries);
    }

    private void checkScreenLock() {
        boolean isScreenLock = mPreferences.getBoolean(PreferencesActivity.KEY_PREF_SCREEN_ROTATION_LOCK_BOOL, false);
        if(isScreenLock){
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }else{
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                try {
                    linkConnect();
                } catch (IllegalStateException e) {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.menu_disconnect:
                linkDisconnect();
                break;
            case R.id.menu_radio_scan:
                radioScan();
                break;
            case R.id.preferences:
                Intent intent = new Intent(this, PreferencesActivity.class);
                startActivity(intent);
                break;
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        mControls.setControlConfig();
        Log.d("Chopter: ","in on resume and the mod is"+Integer.toString(mControls.getMode()));
        switch(mControls.getMode()){
            case(0):
                controller = new TouchJoystick1(mControls, (DualJoystickView) findViewById(R.id.joysticks));
                break;
            case(1):
                controller = new TouchJoystick2(mControls, (DualJoystickView) findViewById(R.id.joysticks));
                break;
            case(2):
                controller = new TouchJoystick3(mControls, (DualJoystickView) findViewById(R.id.joysticks));
                break;
            case(3):
                controller = new TouchJoystick4(mControls, (DualJoystickView) findViewById(R.id.joysticks));
                break;
            case(4):
                controller = new Joystick(mControls);
                break;
            case(5):
                controller = new Gyroscope(mControls,  (SensorManager) getSystemService(Context.SENSOR_SERVICE), (DualJoystickView) findViewById(R.id.joysticks));
                break;
        }

        controller.setOnFlyingDataListener(this);
        controller.enable();
        checkScreenLock();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCrazyradioLink != null) {
            linkDisconnect();
        }
        controller.disable();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mUsbReceiver);
        mSoundPool.release();
        mSoundPool = null;
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (mDoubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }
        this.mDoubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                mDoubleBackToExitPressedOnce = false;

            }
        }, 2000);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        // Check that the event came from a joystick since a generic motion event could be almost anything.
        if ((event.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) != 0 && event.getAction() == MotionEvent.ACTION_MOVE && mControls.getMode() == 4) {
        	((Joystick) controller).dealWithMotionEvent(event);
            return true;
        } else {
            return super.dispatchGenericMotionEvent(event);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // TODO: works for PS3 controller, but does it also work for other controllers?
        // do not call super if key event comes from a gamepad, otherwise the buttons can quit the app
        if (event.getSource() == 1281) {
            mControls.dealWithKeyEvent(event);
            // exception for OUYA controllers
            if (!Build.MODEL.toUpperCase(Locale.getDefault()).contains("OUYA")) {
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void setRadioChannelAndDatarate(int channel, int datarate) {
        if (channel != -1 && datarate != -1) {
            SharedPreferences.Editor editor = mPreferences.edit();
            editor.putString(PreferencesActivity.KEY_PREF_RADIO_CHANNEL, String.valueOf(channel));
            editor.putString(PreferencesActivity.KEY_PREF_RADIO_DATARATE, String.valueOf(datarate));
            editor.commit();

            Toast.makeText(this,"Channel: " + channel + " Data rate: " + mDatarateStrings[datarate] + "\nSetting preferences...", Toast.LENGTH_SHORT).show();
        }
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "mUsbReceiver action: " + action);
            if ((MainActivity.this.getPackageName()+".USB_PERMISSION").equals(action)) {
                //reached only when USB permission on physical connect was canceled and "Connect" or "Radio Scan" is clicked
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            Toast.makeText(MainActivity.this, "Crazyradio attached", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.d(TAG, "permission denied for device " + device);
                    }
                }
            }
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null && CrazyradioLink.isCrazyradio(device)) {
                    Log.d(TAG, "Crazyradio detached");
                    Toast.makeText(MainActivity.this, "Crazyradio detached", Toast.LENGTH_SHORT).show();
                    playSound(mSoundDisconnect);
                    if (mCrazyradioLink != null) {
                        Log.d(TAG, "linkDisconnect()");
                        linkDisconnect();
                    }
                }
            }
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null && CrazyradioLink.isCrazyradio(device)) {
                    Log.d(TAG, "Crazyradio attached");
                    Toast.makeText(MainActivity.this, "Crazyradio attached", Toast.LENGTH_SHORT).show();
                    playSound(mSoundConnect);
                }
            }
        }
    };

    private void playSound(int sound){
        if (mLoaded) {
            float volume = 1.0f;
            mSoundPool.play(sound, volume, volume, 1, 0, 1f);
        }
    }

    private void linkConnect() {
        // ensure previous link is disconnected
        linkDisconnect();

        int radioChannel = Integer.parseInt(mPreferences.getString(PreferencesActivity.KEY_PREF_RADIO_CHANNEL, mRadioChannelDefaultValue));
        int radioDatarate = Integer.parseInt(mPreferences.getString(PreferencesActivity.KEY_PREF_RADIO_DATARATE, mRadioDatarateDefaultValue));

        try {
            // create link
            mCrazyradioLink = new CrazyradioLink(this, new CrazyradioLink.ConnectionData(radioChannel, radioDatarate));

            // add listener for connection status
            mCrazyradioLink.addConnectionListener(new ConnectionAdapter() {
                @Override
                public void connectionSetupFinished(Link l) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void connectionLost(Link l) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Connection lost", Toast.LENGTH_SHORT).show();
                        }
                    });
                    linkDisconnect();
                }

                @Override
                public void connectionFailed(Link l) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Connection failed", Toast.LENGTH_SHORT).show();
                        }
                    });
                    linkDisconnect();
                }

                @Override
                public void linkQualityUpdate(Link l, final int quality) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mFlightDataView.setLinkQualityText(quality + "%");
                        }
                    });
                }
            });

            // connect and start thread to periodically send commands containing
            // the user input
            mCrazyradioLink.connect();
            mSendJoystickDataThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (mCrazyradioLink != null) {
                        mCrazyradioLink.send(new CommanderPacket(controller.getRoll(), controller.getPitch(), controller.getYaw(), (char) (controller.getThrust()), mControls.getXmode()));
                        
                        try {
                            Thread.sleep(20, 0);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
            });
            mSendJoystickDataThread.start();
        } catch (IllegalArgumentException e) {
            Log.d(TAG, e.getMessage());
            Toast.makeText(this, "Crazyradio not attached", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public Link getCrazyflieLink(){
        return mCrazyradioLink;
    }
    
    public void linkDisconnect() {
        if (mCrazyradioLink != null) {
            mCrazyradioLink.disconnect();
            mCrazyradioLink = null;
        }
        if (mSendJoystickDataThread != null) {
            mSendJoystickDataThread.interrupt();
            mSendJoystickDataThread = null;
        }
        
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // link quality is not available when there is no active connection
                mFlightDataView.setLinkQualityText("n/a");
            }
        });
    }

    private void radioScan() {
        new AsyncTask<Void, Void, ConnectionData[]>() {

            private Exception mException = null;
            private ProgressDialog mProgress;
            
            @Override
            protected void onPreExecute() {
                mProgress = ProgressDialog.show(MainActivity.this, "Radio Scan", "Searching for the Crazyflie...", true, false);
            }

            @Override
            protected ConnectionData[] doInBackground(Void... arg0) {
                try {
                    return CrazyradioLink.scanChannels(MainActivity.this);
                } catch(IllegalStateException e) {
                    mException = e;
                    return null;
                }
            }

            @Override
            protected void onPostExecute(ConnectionData[] result) {
                mProgress.dismiss();
                
                if(mException != null) {
                    Toast.makeText(MainActivity.this, mException.getMessage(), Toast.LENGTH_SHORT).show();
                } else {
                    
                    if (result != null && result.length > 0) {
                        if(result.length > 1){
                            // let user choose connection, if there is more than one Crazyflie 
                            showSelectConnectionDialog(result);
                        }else{
                            // use first channel
                            setRadioChannelAndDatarate(result[0].getChannel(), result[0].getDataRate());
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "No connection found", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }.execute();
    }

    private void showSelectConnectionDialog(final ConnectionData[] result) {
        SelectConnectionDialogFragment selectConnectionDialogFragment = new SelectConnectionDialogFragment();
        //supply list of Crazyflie connections as arguments
        Bundle args = new Bundle();
        String[] crazyflieArray = new String[result.length];
        for(int i = 0; i < result.length; i++){
            crazyflieArray[i] = i + ": Channel " + result[i].getChannel() + ", Data rate " + mDatarateStrings[result[i].getDataRate()];
        }
        args.putStringArray("connection_array", crazyflieArray);
        selectConnectionDialogFragment.setArguments(args);
        selectConnectionDialogFragment.setListener(new SelectCrazyflieDialogListener(){
            @Override
            public void onClick(int which) {
                setRadioChannelAndDatarate(result[which].getChannel(), result[which].getDataRate());
            }
        });
        selectConnectionDialogFragment.show(getFragmentManager(), "select_crazyflie");
    }


	@Override
	public void flyingDataEvent(float pitch, float roll, float thrust, float yaw) {
		mFlightDataView.updateFlightData(pitch, roll, thrust, yaw);
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		if(mCrazyradioLink != null) {
				mCrazyradioLink.param.setHoverMode(isChecked);
		} else {
			((ToggleButton) findViewById(R.id.hovermode)).setChecked(false);
		}
	}
}
