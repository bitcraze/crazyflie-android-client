package se.bitcraze.crazyfliecontrol2;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import se.bitcraze.crazyflie.lib.BleLink;
import se.bitcraze.crazyflie.lib.crazyflie.ConnectionAdapter;
import se.bitcraze.crazyflie.lib.crazyflie.Crazyflie;
import se.bitcraze.crazyflie.lib.crazyradio.ConnectionData;
import se.bitcraze.crazyflie.lib.crazyradio.RadioDriver;
import se.bitcraze.crazyflie.lib.crtp.CommanderPacket;
import se.bitcraze.crazyflie.lib.crtp.CrtpDriver;
import se.bitcraze.crazyflie.lib.crtp.ZDistancePacket;
import se.bitcraze.crazyflie.lib.log.LogAdapter;
import se.bitcraze.crazyflie.lib.log.LogConfig;
import se.bitcraze.crazyflie.lib.log.Logg;
import se.bitcraze.crazyflie.lib.param.ParamListener;
import se.bitcraze.crazyflie.lib.toc.Toc;
import se.bitcraze.crazyflie.lib.toc.VariableType;
import se.bitcraze.crazyfliecontrol.console.ConsoleListener;
import se.bitcraze.crazyfliecontrol.controller.GamepadController;

public class MainPresenter {

    private static final String LOG_TAG = "Crazyflie-MainPresenter";

    private MainActivity mainActivity;

    private Crazyflie mCrazyflie;
    private CrtpDriver mDriver;

    private Logg mLogg;
    private LogConfig mLogConfigStandard = new LogConfig("Standard", 1000);

    private Toc mLogToc;
    private Toc mParamToc;

    private boolean mHeadlightToggle = false;
    private boolean mSoundToggle = false;
    private int mRingEffect = 0;
    private int mNoRingEffect = 0;
    private int mCpuFlash = 0;
    private boolean isZrangerAvailable = false;
    private boolean heightHold = false;

    private Thread mSendJoystickDataThread;

    public MainPresenter(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public void onDestroy() {
        this.mainActivity = null;
    }

    private ConnectionAdapter crazyflieConnectionAdapter = new ConnectionAdapter() {

        @Override
        public void connectionRequested() {
            mainActivity.showToastie("Connecting ...");
        }

        @Override
        public void connected() {
            mainActivity.showToastie("Connected");
            if (mCrazyflie != null && mCrazyflie.getDriver() instanceof BleLink) {
                mainActivity.setConnectionButtonConnectedBle();
                // TODO: Remove this once BleLink supports Param and Logg subsystems
                startSendJoystickDataThread();
            } else {
                mainActivity.setConnectionButtonConnected();
            }
        }

        @Override
        public void setupFinished() {
            final Toc paramToc = mCrazyflie.getParam().getToc();
            final Toc logToc = mCrazyflie.getLogg().getToc();
            if (paramToc != null) {
                mParamToc = paramToc;
                mainActivity.showToastie("Parameters TOC fetch finished: " + paramToc.getTocSize());
                checkForBuzzerDeck();
                checkForNoOfRingEffects();
                checkForZRanger();
            }
            if (logToc != null) {
                mLogToc = logToc;
                mainActivity.showToastie("Log TOC fetch finished: " + logToc.getTocSize());
                createLogConfigs();
                startLogConfigs();
            }
            startSendJoystickDataThread();
        }

        @Override
        public void connectionLost(final String msg) {
            mainActivity.showToastie(msg);
            mainActivity.setConnectionButtonDisconnected();
            disconnect();
        }

        @Override
        public void connectionFailed(final String msg) {
            mainActivity.showToastie(msg);
            disconnect();
        }

        @Override
        public void disconnected() {
            mainActivity.showToastie("Disconnected");
            mainActivity.setConnectionButtonDisconnected();
            mainActivity.disableButtonsAndResetBatteryLevel();
            stopLogConfigs();
        }

        @Override
        public void linkQualityUpdated(final int quality) {
            mainActivity.setLinkQualityText(quality + "%");
        }
    };

    // TODO: Replace with specific test for buzzer deck
    private void checkForBuzzerDeck() {
        //activate buzzer sound button when a CF2 is recognized (a buzzer can not yet be detected separately)
        mCrazyflie.getParam().addParamListener(new ParamListener("cpu", "flash") {
            @Override
            public void updated(String name, Number value) {
                mCpuFlash = mCrazyflie.getParam().getValue("cpu.flash").intValue();
                //enable buzzer action button when a CF2 is found (cpu.flash == 1024)
                if (mCpuFlash == 1024) {
                    mainActivity.setBuzzerSoundButtonEnablement(true);
                }
                Log.d(LOG_TAG, "CPU flash: " + mCpuFlash);
            }
        });
        mCrazyflie.getParam().requestParamUpdate("cpu.flash");
    }

    private void checkForZRanger() {
        //this should return true when either a zRanger or a flow deck is connected
        mCrazyflie.getParam().addParamListener(new ParamListener("deck.bcZRanger") {
            @Override
            public void updated(String name, Number value) {
                isZrangerAvailable = mCrazyflie.getParam().getValue("deck.bcZRanger").intValue() == 1;
                // TODO: indicate in the UI that the zRanger sensor is installed
                if (isZrangerAvailable) {
                    mainActivity.showToastie("Found zRanger sensor.");
                }
                Log.d(LOG_TAG, "is zRanger installed: " + isZrangerAvailable);
            }
        });
        mCrazyflie.getParam().requestParamUpdate("deck.bcZRanger");
    }

    private void checkForNoOfRingEffects() {
        //set number of LED ring effects
        mCrazyflie.getParam().addParamListener(new ParamListener("ring", "neffect") {
            @Override
            public void updated(String name, Number value) {
                mNoRingEffect = mCrazyflie.getParam().getValue("ring.neffect").intValue();
                //enable LED ring action buttons only when ring.neffect parameter is set correctly (=> hence it's a CF2 with a LED ring)
                if (mNoRingEffect > 0) {
                    mainActivity.setRingEffectButtonEnablement(true);
                    mainActivity.setHeadlightButtonEnablement(true);
                }
                Log.d(LOG_TAG, "No of ring effects: " + mNoRingEffect);
            }
        });
        mCrazyflie.getParam().requestParamUpdate("ring.neffect");
    }

    /**
     * Start thread to periodically send commands containing the user input
     */
    private void startSendJoystickDataThread() {
        mSendJoystickDataThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (mainActivity != null && mCrazyflie != null) {
                    float roll = mainActivity.getController().getRoll();
                    float pitch = mainActivity.getController().getPitch();
                    float yaw = mainActivity.getController().getYaw();
                    float thrustAbsolute = mainActivity.getController().getThrustAbsolute();
                    float targetHeight = mainActivity.getController().getTargetHeight();
                    boolean xmode = mainActivity.getControls().isXmode();

                    if (heightHold) {
                        mCrazyflie.sendPacket(new ZDistancePacket(roll, pitch, yaw, targetHeight));
                    } else {
                        mCrazyflie.sendPacket(new CommanderPacket(roll, pitch, yaw, (char) thrustAbsolute, xmode));
                    }                    
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

    public void connectCrazyradio(int radioChannel, int radioDatarate, File mCacheDir) {
        Log.d(LOG_TAG, "connectCrazyradio()");
        // ensure previous link is disconnected
        disconnect();
        mDriver = null;
        try {
            mDriver = new RadioDriver(new UsbLinkAndroid(mainActivity));
        } catch (IllegalArgumentException e) {
            Log.d(LOG_TAG, e.getMessage());
            mainActivity.showToastie(e.getMessage());
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage());
            mainActivity.showToastie(e.getMessage());
        }
        connect(mCacheDir, new ConnectionData(radioChannel, radioDatarate));
    }

    public void connectBle(boolean writeWithResponse, File mCacheDir) {
        Log.d(LOG_TAG, "connectBle()");
        // ensure previous link is disconnected
        disconnect();
        mDriver = null;
        mDriver = new BleLink(mainActivity, writeWithResponse);
        connect(mCacheDir, null);
    }

    private void connect(File mCacheDir, ConnectionData connectionData) {
        if (mDriver != null) {
            // add listener for connection status
            mDriver.addConnectionListener(crazyflieConnectionAdapter);

            mCrazyflie = new Crazyflie(mDriver, mCacheDir);
            if (mDriver instanceof RadioDriver) {
                mCrazyflie.setConnectionData(connectionData);
            }
            // connect
            mCrazyflie.connect();

            // add console listener
            ConsoleListener consoleListener = new ConsoleListener();
            consoleListener.setMainActivity(mainActivity);
            mCrazyflie.addDataListener(consoleListener);
        } else {
            mainActivity.showToastie("Cannot connect: Crazyradio not attached and Bluetooth LE not available");
        }
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

        // link quality is not available when there is no active connection
        mainActivity.setLinkQualityText("N/A");
    }

    public void enableAltHoldMode(boolean hover) {
        // For safety reasons, altHold mode is only supported when the Crazyradio and a game pad are used
        if (mCrazyflie != null && mCrazyflie.getDriver() instanceof RadioDriver && mainActivity.getController() instanceof GamepadController) {
            if (isZrangerAvailable) {
                heightHold = hover;
            } else {
//                Log.i(LOG_TAG, "flightmode.althold: getThrust(): " + mController.getThrustAbsolute());
                mCrazyflie.setParamValue("flightmode.althold", hover ? 1 : 0);
            }
        }
    }

    //TODO: make runAltAction more universal
    public void runAltAction(String action) {
        Log.i(LOG_TAG, "runAltAction: " + action);
        if (mCrazyflie != null) {
            if ("ring.headlightEnable".equalsIgnoreCase(action)) {
                // Toggle LED ring headlight
                mHeadlightToggle = !mHeadlightToggle;
                mCrazyflie.setParamValue(action, mHeadlightToggle ? 1 : 0);
                mainActivity.toggleHeadlightButtonColor(mHeadlightToggle);
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

    public Crazyflie getCrazyflie(){
        return mCrazyflie;
    }

    private LogAdapter standardLogAdapter = new LogAdapter() {

        public void logDataReceived(LogConfig logConfig, Map<String, Number> data, int timestamp) {
            super.logDataReceived(logConfig, data, timestamp);

            if ("Standard".equals(logConfig.getName())) {
                final float battery = (float) data.get("pm.vbat");
                mainActivity.setBatteryLevel(battery);
            }
            for (Map.Entry<String, Number> entry : data.entrySet()) {
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
}
