package se.bitcraze.crazyflie.lib.log;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter class for LogListener
 */
public abstract class LogAdapter implements LogListener {

    final Logger mLogger = LoggerFactory.getLogger("LogAdapter");

    @Override
    public void logConfigAdded(LogConfig logConfig) {
        String msg = logConfig.isAdded() ? "' added" : "' deleted";
        mLogger.debug("LogConfig '" + logConfig.getName() + msg);
    }

    @Override
    public void logConfigError(LogConfig logConfig) {
        mLogger.debug("Error when logging '" + logConfig.getName() + "': " + logConfig.getErrMsg());
    }

    @Override
    public void logConfigStarted(LogConfig logConfig) {
        String msg = logConfig.isStarted() ? "' started" : "' stopped";
        mLogger.debug("LogConfig '" + logConfig.getName() + msg);
    }

    @Override
    public void logDataReceived(LogConfig logConfig, Map<String, Number> data, int timestamp) {
        mLogger.debug("LogConfig '" + logConfig.getName()  + "', timestamp: " + timestamp + ", data : ");
    }
}
