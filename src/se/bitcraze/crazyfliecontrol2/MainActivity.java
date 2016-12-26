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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import se.bitcraze.crazyflie.lib.BleLink;
import se.bitcraze.crazyflie.lib.crazyflie.ConnectionAdapter;
import se.bitcraze.crazyflie.lib.crazyflie.Crazyflie;
import se.bitcraze.crazyflie.lib.crazyradio.ConnectionData;
import se.bitcraze.crazyflie.lib.crazyradio.Crazyradio;
import se.bitcraze.crazyflie.lib.crazyradio.RadioDriver;
import se.bitcraze.crazyflie.lib.crtp.CommanderPacket;
import se.bitcraze.crazyflie.lib.crtp.CrtpDriver;
import se.bitcraze.crazyflie.lib.log.LogAdapter;
import se.bitcraze.crazyflie.lib.log.LogConfig;
import se.bitcraze.crazyflie.lib.log.Logg;
import se.bitcraze.crazyflie.lib.param.ParamListener;
import se.bitcraze.crazyflie.lib.toc.Toc;
import se.bitcraze.crazyflie.lib.toc.VariableType;
import se.bitcraze.crazyfliecontrol.controller.Controls;
import se.bitcraze.crazyfliecontrol.controller.GamepadController;
import se.bitcraze.crazyfliecontrol.controller.GyroscopeController;
import se.bitcraze.crazyfliecontrol.controller.IController;
import se.bitcraze.crazyfliecontrol.controller.TouchController;
import se.bitcraze.crazyfliecontrol.prefs.PreferencesActivity;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.MobileAnarchy.Android.Widgets.Joystick.DualJoystickView;

public class MainActivity extends Activity {

    private static final String LOG_TAG = "CrazyflieControl";

    private DualJoystickView mDualJoystickView;
    private FlightDataView mFlightDataView;

    private Crazyflie mCrazyflie;
    private CrtpDriver mDriver;
    private Toc mParamToc;
    private Toc mLogToc;

    private Logg mLogg;
    private LogConfig mLogConfigStandard = new LogConfig("Standard", 1000);

    private SharedPreferences mPreferences;

    private IController mController;
    private GamepadController mGamepadController;

    private String mRadioChannelDefaultValue;
    private String mRadioDatarateDefaultValue;

    private boolean mDoubleBackToExitPressedOnce = false;

    private Thread mSendJoystickDataThread;

    private Controls mControls;

    private SoundPool mSoundPool;
    private boolean mLoaded;
    private int mSoundConnect;
    private int mSoundDisconnect;

    private ImageButton mToggleConnectButton;

    private boolean mHeadlightToggle = false;
    private boolean mSoundToggle = false;
    private int mRingEffect = 0;
    private int mNoRingEffect = 0;
    private int mCpuFlash = 0;
    private ImageButton mRingEffectButton;
    private ImageButton mHeadlightButton;
    private ImageButton mBuzzerSoundButton;
    private File mCacheDir;

    private TextView mTextView_battery;
    private TextView mTextView_linkQuality;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setDefaultPreferenceValues();

        mTextView_battery = (TextView) findViewById(R.id.battery_text);
        mTextView_linkQuality = (TextView) findViewById(R.id.linkQuality_text);

        setBatteryLevel(-1.0f);
        setLinkQualityText("N/A");

        mControls = new Controls(this, mPreferences);
        mControls.setDefaultPreferenceValues(getResources());

        //Default controller
        mDualJoystickView = (DualJoystickView) findViewById(R.id.joysticks);
        mController = new TouchController(mControls, this, mDualJoystickView);

        //initialize gamepad controller
        mGamepadController = new GamepadController(mControls, this, mPreferences);
        mGamepadController.setDefaultPreferenceValues(getResources());

        //initialize buttons
        mToggleConnectButton = (ImageButton) findViewById(R.id.imageButton_connect);
        initializeMenuButtons();

        mFlightDataView = (FlightDataView) findViewById(R.id.flightdataview);

        //action buttons
        mRingEffectButton = (ImageButton) findViewById(R.id.button_ledRing);
        mHeadlightButton = (ImageButton) findViewById(R.id.button_headLight);
        mBuzzerSoundButton = (ImageButton) findViewById(R.id.button_buzzerSound);

        IntentFilter filter = new IntentFilter();
        filter.addAction(this.getPackageName()+".USB_PERMISSION");
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filter);

        initializeSounds();

        setCacheDir();
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

    private void setCacheDir() {
        if (isExternalStorageWriteable()) {
            Log.d(LOG_TAG, "External storage is writeable.");
            if (mCacheDir == null) {
                File appDir = getApplicationContext().getExternalFilesDir(null);
                mCacheDir = new File(appDir, "TOC_cache");
                mCacheDir.mkdirs();
            }
        } else {
            Log.d(LOG_TAG, "External storage is not writeable.");
            mCacheDir = null;
        }
    }

    private boolean isExternalStorageWriteable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private void setDefaultPreferenceValues(){
        // Set default preference values
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        // Initialize preferences
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        mRadioChannelDefaultValue = getString(R.string.preferences_radio_channel_defaultValue);
        mRadioDatarateDefaultValue = getString(R.string.preferences_radio_datarate_defaultValue);
    }

    private void checkScreenLock() {
        boolean isScreenLock = mPreferences.getBoolean(PreferencesActivity.KEY_PREF_SCREEN_ROTATION_LOCK_BOOL, false);
        if(isScreenLock){
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }else{
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
    }

    private void initializeMenuButtons() {
        mToggleConnectButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    if (mCrazyflie != null && mCrazyflie.isConnected()) {
                        disconnect();
                    } else {
                        connect();
                    }
                } catch (IllegalStateException e) {
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        ImageButton settingsButton = (ImageButton) findViewById(R.id.imageButton_settings);
        settingsButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
              Intent intent = new Intent(MainActivity.this, PreferencesActivity.class);
              startActivity(intent);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        //TODO: improve
        PreferencesActivity.setDefaultJoystickSize(this);
        mDualJoystickView.setPreferences(mPreferences);
        mControls.setControlConfig();
        mGamepadController.setControlConfig();
        resetInputMethod();
        checkScreenLock();
        //disable action buttons
        mRingEffectButton.setEnabled(false);
        mHeadlightButton.setEnabled(false);
        mBuzzerSoundButton.setEnabled(false);
        if (mPreferences.getBoolean(PreferencesActivity.KEY_PREF_IMMERSIVE_MODE_BOOL, false)) {
            setHideyBar();
        }
        mDualJoystickView.requestLayout();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mControls.resetAxisValues();
        mController.disable();
        disconnect();
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
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(mPreferences.getBoolean(PreferencesActivity.KEY_PREF_IMMERSIVE_MODE_BOOL, false) && hasFocus){
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    setHideyBar();
                }
            }, 2000);
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void setHideyBar() {
        Log.i(LOG_TAG, "Activating immersive mode");
        int uiOptions = getWindow().getDecorView().getSystemUiVisibility();
        int newUiOptions = uiOptions;

        if(Build.VERSION.SDK_INT >= 14){
            newUiOptions |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        }
        if(Build.VERSION.SDK_INT >= 16){
            newUiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN;
        }
        if(Build.VERSION.SDK_INT >= 18){
            newUiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }
        getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
    }

    //TODO: fix indirection
    public void updateFlightData(){
        mFlightDataView.updateFlightData(mController.getPitch(), mController.getRoll(), mController.getThrust(), mController.getYaw());
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        // Check that the event came from a joystick since a generic motion event could be almost anything.
        if ((event.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) != 0 && event.getAction() == MotionEvent.ACTION_MOVE && mController instanceof GamepadController) {
            mGamepadController.dealWithMotionEvent(event);
            updateFlightData();
            return true;
        } else {
            return super.dispatchGenericMotionEvent(event);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // do not call super if key event comes from a gamepad, otherwise the buttons can quit the app
        if (isJoystickButton(event.getKeyCode()) && mController instanceof GamepadController) {
            mGamepadController.dealWithKeyEvent(event);
            // exception for OUYA controllers
            if (!Build.MODEL.toUpperCase(Locale.getDefault()).contains("OUYA")) {
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    // this workaround is necessary because DPad buttons are not considered to be "Gamepad buttons"
    private static boolean isJoystickButton(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                return true;
            default:
                return KeyEvent.isGamepadButton(keyCode);
        }
    }

    private void resetInputMethod() {
        mController.disable();
        switch (mControls.getControllerType()) {
            case 0:
                // Use GyroscopeController if activated in the preferences
                if (mControls.isUseGyro()) {
                    mController = new GyroscopeController(mControls, this, mDualJoystickView);
                } else {
                    // TODO: reuse existing touch controller?
                    mController = new TouchController(mControls, this, mDualJoystickView);
                }
                break;
            case 1:
                    // TODO: show warning if no game pad is found?
                    mController = mGamepadController;
                break;
            default:
                break;

        }
        mController.enable();
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(LOG_TAG, "mUsbReceiver action: " + action);
            if ((MainActivity.this.getPackageName()+".USB_PERMISSION").equals(action)) {
                //reached only when USB permission on physical connect was canceled and "Connect" or "Radio Scan" is clicked
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            Toast.makeText(MainActivity.this, "Crazyradio attached", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.d(LOG_TAG, "permission denied for device " + device);
                    }
                }
            }
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null && UsbLinkAndroid.isUsbDevice(device, Crazyradio.CRADIO_VID, Crazyradio.CRADIO_PID)) {
                    Log.d(LOG_TAG, "Crazyradio detached");
                    Toast.makeText(MainActivity.this, "Crazyradio detached", Toast.LENGTH_SHORT).show();
                    playSound(mSoundDisconnect);
                    if (mCrazyflie != null) {
                        Log.d(LOG_TAG, "linkDisconnect()");
                        disconnect();
                    }
                }
            }
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null && UsbLinkAndroid.isUsbDevice(device, Crazyradio.CRADIO_VID, Crazyradio.CRADIO_PID)) {
                    Log.d(LOG_TAG, "Crazyradio attached");
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

    private ConnectionAdapter crazyflieConnectionAdapter = new ConnectionAdapter() {

        @Override
        public void connectionRequested(String connectionInfo) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Connecting ...", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void connected(String connectionInfo) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                    if (mCrazyflie != null && mCrazyflie.getDriver() instanceof BleLink) {
                        mToggleConnectButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.custom_button_connected_ble));
                        // TODO: Remove this once BleLink supports Param and Logg subsystems
                        startSendJoystickDataThread();
                    } else {
                        mToggleConnectButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.custom_button_connected));
                    }
                }
            });
        }

        @Override
        public void setupFinished(String connectionInfo) {
           final Toc paramToc = mCrazyflie.getParam().getToc();
           final Toc logToc = mCrazyflie.getLogg().getToc();
           if (paramToc != null) {
               mParamToc = paramToc;
               runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Parameters TOC fetch finished: " + paramToc.getTocSize(), Toast.LENGTH_SHORT).show();
                    }
                });
                //activate buzzer sound button when a CF2 is recognized (a buzzer can not yet be detected separately)
                mCrazyflie.getParam().addParamListener(new ParamListener("cpu", "flash") {
                    @Override
                    public void updated(String name, Number value) {
                        mCpuFlash = mCrazyflie.getParam().getValue("cpu.flash").intValue();
                        //enable buzzer action button when a CF2 is found (cpu.flash == 1024)
                        if (mCpuFlash == 1024) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mBuzzerSoundButton.setEnabled(true);
                                }
                            });
                        }
                        Log.d(LOG_TAG, "CPU flash: " + mCpuFlash);
                    }
                });
                mCrazyflie.getParam().requestParamUpdate("cpu.flash");
                //set number of LED ring effects
                mCrazyflie.getParam().addParamListener(new ParamListener("ring", "neffect") {
                    @Override
                    public void updated(String name, Number value) {
                        mNoRingEffect = mCrazyflie.getParam().getValue("ring.neffect").intValue();
                        //enable LED ring action buttons only when ring.neffect parameter is set correctly (=> hence it's a CF2 with a LED ring)
                        if (mNoRingEffect > 0) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mRingEffectButton.setEnabled(true);
                                    mHeadlightButton.setEnabled(true);
                                }
                            });
                        }
                        Log.d(LOG_TAG, "No of ring effects: " + mNoRingEffect);
                    }
                });
                mCrazyflie.getParam().requestParamUpdate("ring.neffect");
            }
            if (logToc != null) {
                mLogToc = logToc;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Log TOC fetch finished: " + logToc.getTocSize(), Toast.LENGTH_SHORT).show();
                    }
                });
                createLogConfigs();
                startLogConfigs();
            }

            startSendJoystickDataThread();
        }

        @Override
        public void connectionLost(String connectionInfo, final String msg) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                    mToggleConnectButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.custom_button));
                }
            });
            disconnect();
        }

        @Override
        public void connectionFailed(String connectionInfo, final String msg) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                }
            });
            disconnect();
        }

        @Override
        public void disconnected(String connectionInfo) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_SHORT).show();
                    mToggleConnectButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.custom_button));
                    //disable action buttons after disconnect
                    mRingEffectButton.setEnabled(false);
                    mHeadlightButton.setEnabled(false);
                    mBuzzerSoundButton.setEnabled(false);
                    setBatteryLevel(-1.0f);
                }
            });
            stopLogConfigs();
        }

        @Override
        public void linkQualityUpdated(final int quality) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setLinkQualityText(quality + "%");
                }
            });
        }
    };

    private void connect() {
        Log.d(LOG_TAG, "connect()");
        // ensure previous link is disconnected
        disconnect();

        int radioChannel = Integer.parseInt(mPreferences.getString(PreferencesActivity.KEY_PREF_RADIO_CHANNEL, mRadioChannelDefaultValue));
        int radioDatarate = Integer.parseInt(mPreferences.getString(PreferencesActivity.KEY_PREF_RADIO_DATARATE, mRadioDatarateDefaultValue));

        mDriver = null;

        if(isCrazyradioAvailable(this)) {
            try {
                mDriver = new RadioDriver(new UsbLinkAndroid(this));
            } catch (IllegalArgumentException e) {
                Log.d(LOG_TAG, e.getMessage());
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Log.e(LOG_TAG, e.getMessage());
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            //use BLE
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) &&
                    getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
                boolean writeWithResponse = mPreferences.getBoolean(PreferencesActivity.KEY_PREF_BLATENCY_BOOL, false);
                Log.d(LOG_TAG, "Using bluetooth write with response - " + writeWithResponse);
                mDriver = new BleLink(this, writeWithResponse);
            } else {
                // TODO: improve error message
                Log.e(LOG_TAG, "No BLE support available.");
            }
        }

        if (mDriver != null) {

            // add listener for connection status
            mDriver.addConnectionListener(crazyflieConnectionAdapter);

            mCrazyflie = new Crazyflie(mDriver, mCacheDir);

            // connect
            mCrazyflie.connect(new ConnectionData(radioChannel, radioDatarate));

//            mCrazyflie.addDataListener(new DataListener(CrtpPort.CONSOLE) {
//
//                @Override
//                public void dataReceived(CrtpPacket packet) {
//                    Log.d(LOG_TAG, "Received console packet: " + packet);
//                }
//
//            });
        } else {
            Toast.makeText(this, "Cannot connect: Crazyradio not attached and Bluetooth LE not available", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Start thread to periodically send commands containing the user input
     */
    private void startSendJoystickDataThread() {
        mSendJoystickDataThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (mCrazyflie != null) {
                    // Log.d(LOG_TAG, "Thrust absolute: " + mController.getThrustAbsolute());
                    mCrazyflie.sendPacket(new CommanderPacket(mController.getRoll(), mController.getPitch(), mController.getYaw(), (char) (mController.getThrustAbsolute()), mControls.isXmode()));
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        Log.d(LOG_TAG, "SendJoystickDataThread was interrupted.");
                        break;
                    }
                }
            }
        });
        mSendJoystickDataThread.start();
    }

    // extra method for onClick attribute in XML
    public void switchLedRingEffect(View view) {
        runAltAction("ring.effect");
    }

    // extra method for onClick attribute in XML
    public void toggleHeadlight(View view) {
        runAltAction("ring.headlightEnable");
    }

    // extra method for onClick attribute in XML
    public void playBuzzerSound(View view) {
        runAltAction("sound.effect:10");
    }

    //TODO: make runAltAction more universal
    public void runAltAction(String action) {
        Log.i(LOG_TAG, "runAltAction: " + action);
        if (mCrazyflie != null) {
            if ("ring.headlightEnable".equalsIgnoreCase(action)) {
                // Toggle LED ring headlight
                mHeadlightToggle = !mHeadlightToggle;
                mCrazyflie.setParamValue(action, mHeadlightToggle ? 1 : 0);
                mHeadlightButton.setColorFilter(mHeadlightToggle ? Color.parseColor("#00FF00") : Color.BLACK);
            } else if ("ring.effect".equalsIgnoreCase(action)) {
                // Cycle through LED ring effects
                Log.i(LOG_TAG, "Ring effect: " + mRingEffect);
                mCrazyflie.setParamValue(action, mRingEffect);
                mRingEffect++;
                mRingEffect = (mRingEffect > mNoRingEffect) ? 0 : mRingEffect;
            } else if (action.startsWith("sound.effect")) {
                // Toggle buzzer deck sound effect
                String[] split = action.split(":");
                Log.i(LOG_TAG, "Sound effect: " + split[1]);
                mCrazyflie.setParamValue(split[0], mSoundToggle ? Integer.parseInt(split[1]) : 0);
                mSoundToggle = !mSoundToggle;
            }
        } else {
            Log.d(LOG_TAG, "runAltAction - crazyflie is null");
        }
    }

    public void enableAltHoldMode(boolean hover) {
        // For safety reasons, altHold mode is only supported when the Crazyradio and a game pad are used
        if (mCrazyflie != null && mCrazyflie.getDriver() instanceof RadioDriver && mController instanceof GamepadController) {
//            Log.i(LOG_TAG, "flightmode.althold: getThrust(): " + mController.getThrustAbsolute());
            mCrazyflie.setParamValue("flightmode.althold", hover ? 1 : 0);
        }
    }

    public Crazyflie getCrazyflie(){
        return mCrazyflie;
    }

    public void disconnect() {
        Log.d(LOG_TAG, "disconnect()");
        if (mCrazyflie != null) {
            mCrazyflie.disconnect();
            mCrazyflie = null;
        }
        if (mSendJoystickDataThread != null) {
            mSendJoystickDataThread.interrupt();
            mSendJoystickDataThread = null;
        }

        if (mDriver != null) {
            mDriver.removeConnectionListener(crazyflieConnectionAdapter);
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // link quality is not available when there is no active connection
                setLinkQualityText("N/A");
            }
        });
    }

    public IController getController(){
        return mController;
    }

    public static boolean isCrazyradioAvailable(Context context) {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        if (usbManager == null) {
            throw new IllegalArgumentException("UsbManager == null!");
        }
        List<UsbDevice> usbDeviceList = UsbLinkAndroid.findUsbDevices(usbManager, (short) Crazyradio.CRADIO_VID, (short) Crazyradio.CRADIO_PID);
        return !usbDeviceList.isEmpty();
    }

    private LogAdapter standardLogAdapter = new LogAdapter() {

        public void logDataReceived(LogConfig logConfig, Map<String, Number> data, int timestamp) {
            super.logDataReceived(logConfig, data, timestamp);

            if ("Standard".equals(logConfig.getName())) {
                final float battery = (float) data.get("pm.vbat");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setBatteryLevel(battery);
                    }
                });
            }
            for (Entry<String, Number> entry : data.entrySet()) {
                Log.d(LOG_TAG, "\t Name: " + entry.getKey() + ", data: " + entry.getValue());
            }
        }

    };

    private void createLogConfigs() {
        mLogConfigStandard.addVariable("pm.vbat", VariableType.FLOAT);
        mLogg = mCrazyflie.getLogg();

        if (mLogg != null) {
            mLogg.addConfig(mLogConfigStandard);
            mLogg.addLogListener(standardLogAdapter);
        } else {
            Log.e(LOG_TAG, "Logg was null!!");
        }
    }

    /**
     * Start basic logging config
     */
    private void startLogConfigs() {
        if (mLogg != null) {
            mLogg.start(mLogConfigStandard);
        }
    }

    /**
     * Stop basic logging config
     */
    private void stopLogConfigs() {
        if (mLogg != null) {
            mLogg.stop(mLogConfigStandard);
            mLogg.delete(mLogConfigStandard);
            mLogg.removeLogListener(standardLogAdapter);
        }
    }

    public void setBatteryLevel(float battery) {
        float normalizedBattery = battery - 3.0f;
        int batteryPercentage = (int) (normalizedBattery * 100);
        if (battery == -1f) {
            batteryPercentage = 0;
        } else if (normalizedBattery < 0f && normalizedBattery > -1f) {
            batteryPercentage = 0;
        } else if (normalizedBattery > 1f) {
            batteryPercentage = 100;
        }
        mTextView_battery.setText(format(R.string.battery_text, batteryPercentage));
    }

    public void setLinkQualityText(String quality){
        mTextView_linkQuality.setText(format(R.string.linkQuality_text, quality));
    }

    private String format(int identifier, Object o){
        return String.format(getResources().getString(identifier), o);
    }
}
