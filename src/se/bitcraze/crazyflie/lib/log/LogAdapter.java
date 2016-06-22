package se.bitcraze.crazyflie.lib.log;

import android.util.Log;

import java.util.Map;

/**
 * Adapter class for LogListener
 */
public abstract class LogAdapter implements LogListener {

    private static final String LOG_TAG = "LogAdapter";

    @Override
    public void logConfigAdded(LogConfig logConfig) {
        String msg = logConfig.isAdded() ? "' added" : "' deleted";
        Log.d(LOG_TAG, "LogConfig '" + logConfig.getName() + msg);
    }

    @Override
    public void logConfigError(LogConfig logConfig) {
        Log.e(LOG_TAG, "Error when logging '" + logConfig.getName() + "': " + logConfig.getErrNo());
    }

    @Override
    public void logConfigStarted(LogConfig logConfig) {
        String msg = logConfig.isStarted() ? "' started" : "' stopped";
        Log.d(LOG_TAG, "LogConfig '" + logConfig.getName() + msg);
    }

    @Override
    public void logDataReceived(LogConfig logConfig, Map<String, Number> data, int timestamp) {
        Log.d(LOG_TAG, "LogConfig '" + logConfig.getName()  + "', timestamp: " + timestamp + ", data : ");
    }
}
