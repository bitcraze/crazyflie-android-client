/**
 *    ||          ____  _ __
 * +------+      / __ )(_) /_______________ _____  ___
 * | 0xBC |     / __  / / __/ ___/ ___/ __ `/_  / / _ \
 * +------+    / /_/ / / /_/ /__/ /  / /_/ / / /_/  __/
 *  ||  ||    /_____/_/\__/\___/_/   \__,_/ /___/\___/
 *
 * Copyright (C) 2015 Bitcraze AB
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

package se.bitcraze.crazyflie.lib.log;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.bitcraze.crazyflie.lib.crazyflie.Crazyflie;
import se.bitcraze.crazyflie.lib.crazyflie.DataListener;
import se.bitcraze.crazyflie.lib.crtp.CrtpPacket;
import se.bitcraze.crazyflie.lib.crtp.CrtpPort;
import se.bitcraze.crazyflie.lib.crtp.CrtpPacket.Header;
import se.bitcraze.crazyflie.lib.toc.Toc;
import se.bitcraze.crazyflie.lib.toc.TocCache;
import se.bitcraze.crazyflie.lib.toc.TocElement;
import se.bitcraze.crazyflie.lib.toc.TocFetchFinishedListener;
import se.bitcraze.crazyflie.lib.toc.TocFetcher;
import se.bitcraze.crazyflie.lib.toc.VariableType;

//TODO: find better name
//TODO: add remaining callbacks/listeners
//TODO: dataReceived callback in Logg or LogConfig?

//TODO: save/load config to/from file (JSON?)

public class Logg {

    final Logger mLogger = LoggerFactory.getLogger("Logging");

    // The max size of a CRTP packet payload
    private final static int MAX_LOG_DATA_PACKET_SIZE = 30;

    private Crazyflie mCrazyflie;
    private Toc mToc = null;
    private TocCache mTocCache = null;
    private TocFetchFinishedListener mTocFetchFinishedListener;

    private List<LogConfig> mLogConfigs = new ArrayList<LogConfig>();
    private int mLogConfigIdCounter = 0;

    private Set<LogListener> mLogListeners = new CopyOnWriteArraySet<LogListener>();

    private static Map<Integer, String> mErrCodes = new HashMap<Integer, String>();

    /*
     * These codes can be decoded using os.stderror, but
     * some of the text messages will look very strange
     * in the UI, so they are redefined here
     */
    static {
        mErrCodes.put(12, "No more memory available");
        mErrCodes.put(8, "Command not found");
        mErrCodes.put(2, "No such log config ID");
        mErrCodes.put(7, "Log config too large");
        mErrCodes.put(17, "Log config already exists");
    }

    public static String getErrorMsg(int errNo) {
        return mErrCodes.containsKey(errNo) ? mErrCodes.get(errNo) : "Unknown error code";
    }


    public Logg(Crazyflie crazyflie) {
        this.mCrazyflie = crazyflie;

        /*
        # Called with newly created blocks
        self.block_added_cb = Caller()
         */

        // TODO: there are two dataListeners active on CrtpPort.LOGGING (one in Logg and one in TocFetcher)
        // self.cf.add_port_callback(CRTPPort.LOGGING, self._new_packet_cb)
        mCrazyflie.addDataListener(new DataListener(CrtpPort.LOGGING) {
            @Override
            public void dataReceived(CrtpPacket packet) {
                newPacketReceived(packet);
            }
        });

        /*
        self.toc_updated = Caller()
        self.state = IDLE
        self.fake_toc_crc = 0xDEADBEEF

        self._refresh_callback = None
        */
    }

    //TODO: only for debugging
    public Toc getToc() {
        return this.mToc;
    }

    /**
     * Add a log configuration to the logging framework <br/>
     * <br/>
     * When doing this the contents of the log configuration will be validated
     * and listeners for new log configurations will be notified.<br/>
     * When validating the configuration the variables are checked against the TOC
     * to see that they actually exist. If they don't exist then the configuration
     * cannot be used.<br/>
     * Since a valid TOC is required, a Crazyflie has to be
     * connected when calling this method, otherwise it will fail.
     *
     * @param logConfig
     */
    public void addConfig(LogConfig logConfig) {
        //TODO: check if really connected
        if (this.mCrazyflie.getDriver() == null) {
            mLogger.error("Cannot add configs without being connected to a Crazyflie!");
            return;
        }
        /*
         * If the log configuration contains variables that we added without
         * type (i.e we want the stored as type for fetching as well) then
         * resolve this now and add the variable type.
         */
        for(LogVariable logVariable : logConfig.getLogVariables()) {
            if (logVariable.getVariableType() == null) {
                String name = logVariable.getName();
                TocElement tocElement = mToc.getElementByCompleteName(name);
                if (tocElement == null) {
                    mLogger.warn(name + "is not in TOC, this log config cannot be used!");
                    logConfig.setValid(false);
                    return;
                    // raise KeyError("Variable {} not in TOC".format(name))
                } else {
                    // Now that we know what type this variable has, set the correct type
                    logVariable.setVariableType(tocElement.getCtype());
                }
            }
        }
        /*
         * Now check that all the added variables are in the TOC and that
         * the total size constraint of a data packet with logging data is not ???
         */

        // TODO: iterate only once over all log variables?

        int size = 0;
        for (LogVariable logVariable : logConfig.getLogVariables()) {
            // size += LogTocElement.get_size_from_id(var.fetch_as)
            size += logVariable.getVariableType().getSize();

            /*
             * Check that we are able to find the variable in the TOC so
             * we can return error already now and not when the config is sent
             */

            // TODO: seems to be redundant
            if (logVariable.isTocVariable()) {
                if (mToc.getElementByCompleteName(logVariable.getName()) == null) {
                    mLogger.warn(logVariable.getName() + " not in TOC, this log config cannot be used!");
                    logConfig.setValid(false);
                    return;
                    // raise KeyError("Variable {} not in TOC".format(var.name))
                }
            }
        }

        if (size <= MAX_LOG_DATA_PACKET_SIZE && (logConfig.getPeriod() > 0 && logConfig.getPeriod() < 0xFF)) {
            logConfig.setValid(true);
            // logconf.cf = self.cf         -> not necessary in Java

            // set log config ID
            logConfig.setId((mLogConfigIdCounter + 1) % 255);

            mLogConfigs.add(logConfig);
            // TODO: self.block_added_cb.call(logconf)
        } else {
            logConfig.setValid(false);
            return;
            // raise AttributeError("The log configuration is too large or has an invalid parameter")
        }
    }

    /**
     * Start refreshing the table of loggable variables
     */
    // def refresh_toc(self, refresh_done_callback, toc_cache):
    public void refreshToc(TocFetchFinishedListener listener, TocCache tocCache) {
        this.mToc = null;
        this.mTocCache = tocCache;
        // self._refresh_callback = refresh_done_callback
        this.mTocFetchFinishedListener = listener;

        Header header = new Header(CHAN_SETTINGS, CrtpPort.LOGGING);
        CrtpPacket packet = new CrtpPacket(header.getByte(), new byte[]{CMD_RESET_LOGGING});
        packet.setExpectedReply(new byte[]{CMD_RESET_LOGGING});
        this.mCrazyflie.sendPacket(packet);
    }

    private LogConfig findLogConfig (int id) {
        for (LogConfig logConfig : mLogConfigs) {
            if (logConfig.getId() == id) {
                return logConfig;
            }
        }
        return null;
    }

    /**
     * Callback for newly arrived packets with TOC information
     *
     * @param packet
     */
    public void newPacketReceived(CrtpPacket packet) {
        int channel = packet.getHeader().getChannel();

        //TODO: cmd vs id in payload[0] !?!
        //cmd = packet.datal[0]
        byte cmd = packet.getPayload()[0];

        // payload = struct.pack("B" * (len(packet.datal) - 1), *packet.datal[1:])
        //TODO: payload starts on third byte!?

        byte[] payload = packet.getPayload();

        if (channel == CHAN_SETTINGS) {
            int id = payload[1];
            int errorStatus = payload[2];
            LogConfig logConfig = findLogConfig(id);

            //TODO: use switch instead of if?
            if (cmd == CMD_CREATE_LOGCONFIG) {
                if (logConfig != null) {
                    if (errorStatus == 0x00) {
                        if (!logConfig.isAdded()) {
                            mLogger.debug("Successfully added log config ID=" + id);

                            // TODO: call start(LogConfig) instead?
                            // TODO: double check with start method (add & start vs just add)
                            Header header = new Header(CHAN_SETTINGS, CrtpPort.LOGGING);
                            CrtpPacket newPacket = new CrtpPacket(header.getByte(), new byte[]{CMD_START_LOGGING, (byte) id, (byte) logConfig.getPeriod()});
                            packet.setExpectedReply(new byte[]{CMD_START_LOGGING, (byte) id});
                            this.mCrazyflie.sendPacket(newPacket);

                            logConfig.setAdded(true);
                            notifyLogAdded(logConfig);
                        } else {
                            //TODO can this ever happen?
                            mLogger.warn("Log config ID=" + id + " is already added. Flag error?");
                        }
                    } else {
                        //TODO: logging is redundant if LogListener is set up correctly
                        // msg = self._err_codes[error_status]
                        String msg = getErrorMsg(errorStatus);
                        mLogger.warn("Error " + errorStatus + " when adding ID=" + id + " (" + msg + ")");
                        logConfig.setErrNo(errorStatus);
                        /*
                        TODO:
                        block.added_cb.call(False)
                        block.error_cb.call(block, msg)
                        */
                        notifyLogAdded(logConfig);
                        notifyLogError(logConfig);
                    }
                } else {
                    mLogger.warn("No LogEntry to assign log config to !!!");
                }
            } else if (cmd == CMD_START_LOGGING) {
                if (errorStatus == 0x00) {
                    mLogger.info("Successfully started logging for log config ID=" +id);
                    if (logConfig != null) {
                        logConfig.setStarted(true);
                        notifyLogStarted(logConfig);
                    }
                } else {
                    // msg = self._err_codes[error_status]
                    String msg = getErrorMsg(errorStatus);
                    mLogger.warn("Error " + errorStatus + " when starting ID=" + id + " (" + msg + ")");

                    if (logConfig != null) {
                        logConfig.setErrNo(errorStatus);
                        /*
                        block.started_cb.call(False)
                        # This is a temporary fix, we are adding a new issue
                        # for this. For some reason we get an error back after
                        # the log config has been started and added. This will show
                        # an error in the UI, but everything is still working.
                        #block.error_cb.call(block, msg)
                        */
                        notifyLogError(logConfig);
                    }
                }
            } else if (cmd == CMD_STOP_LOGGING) {
                if (errorStatus == 0x00) {
                    mLogger.info("Successfully stopped logging for ID=" + id);
                    if (logConfig != null) {
                        logConfig.setStarted(false);
                        notifyLogStarted(logConfig);
                    }
                } else {
                    mLogger.warn("Problem when stopping logging for ID=" +id);
                }
            } else if (cmd == CMD_DELETE_LOGCONFIG) {
                /*
                 * Accept deletion of a log config that hasn't been added. This could
                 * happen due to timing (i.e add/start/delete in fast sequence)
                 */
                if (errorStatus == 0x00) {
                    mLogger.info("Successfully deleted log config ID=" + id);
                    if (logConfig != null) {
//                        logConfig.setStarted(false);
                        logConfig.setAdded(false);
//                        notifyLogStarted(logConfig);
                        notifyLogAdded(logConfig);
                    }
                } else {
                    mLogger.warn("Problem when deleting log config ID=" +id);
                }
            } else if (cmd == CMD_RESET_LOGGING) {
                // Guard against multiple responses due to re-sending
                if (mToc == null) {
                    mLogger.debug("Logging reset, continue with TOC download");
                    mLogConfigs = new ArrayList<LogConfig>();

                    mToc = new Toc();
                    // toc_fetcher = TocFetcher(self.cf, LogTocElement, CRTPPort.LOGGING, self.toc, self._refresh_callback, self._toc_cache)
                    TocFetcher tocFetcher = new TocFetcher(mCrazyflie, CrtpPort.LOGGING, mToc, mTocCache);
                    tocFetcher.addTocFetchFinishedListener(mTocFetchFinishedListener);
                    tocFetcher.start();
                }
            }
        } else if (channel == CHAN_LOGDATA) {
            // TODO: fix payload offset
            int id = payload[0];
            LogConfig logConfig = findLogConfig(id);
            
            if (logConfig != null) {
                Map<String, Number> logDataMap = new HashMap<String, Number>();
                int timestamp = parseLogData(payload, logConfig, logDataMap);
                notifyLogDataReceived(logConfig, logDataMap, timestamp);
            } else {
                mLogger.warn("Error no LogEntry to handle id=" + id);
            }
        }
    }

    public static int parseLogData(byte[] payload, LogConfig logConfig, Map<String, Number> logDataMap) {
        //get timestamp
        int timestamp = parseTimestamp(payload[1], payload[2], payload[3]);
        // logdata = packet.data[4:]
        int offset = 4;
        byte[] logData = new byte[payload.length-offset];
        System.arraycopy(payload, offset, logData, 0, logData.length);

        logDataMap.putAll(logConfig.unpackLogData(logData));
        LoggerFactory.getLogger("Logging").debug("Unpacked log data (ID: " + logConfig.getId() + ") with time stamp " + timestamp);
        //TODO: what to do with the unpacked data?
        return timestamp;
    }

    // timestamps = struct.unpack("<BBB", packet.data[1:4])
    // timestamp = (timestamps[0] | timestamps[1] << 8 | timestamps[2] << 16)
    private static int parseTimestamp(byte data1, byte data2, byte data3) {
        //allocate 4 bytes for an int
        ByteBuffer buffer = ByteBuffer.allocate(4).order(CrtpPacket.BYTE_ORDER);
        buffer.put(data1);
        buffer.put(data2);
        buffer.put(data3);
        buffer.rewind();
        return buffer.getInt();
    }


    /* Methods from LogConfig class */

    // Commands used when accessing the Log configurations
    private final static int CMD_CREATE_LOGCONFIG = 0;
    private final static int CMD_APPEND_LOGCONFIG = 1;
    private final static int CMD_DELETE_LOGCONFIG = 2;
    private final static int CMD_START_LOGGING = 3;
    private final static int CMD_STOP_LOGGING = 4;
    private final static int CMD_RESET_LOGGING = 5;


    // Channels used for the logging port
    private final static int CHAN_TOC = 0;
    private final static int CHAN_SETTINGS = 1;
    private final static int CHAN_LOGDATA = 2;


    //TODO: callbacks

    /**
     * Save the log configuration in the Crazyflie
     */
    public void create(LogConfig logConfig) {
        int logConfigId = logConfig.getId();

        ByteBuffer bb = ByteBuffer.allocate(31);
        bb.put((byte) CMD_CREATE_LOGCONFIG);
        bb.put((byte) logConfigId);

        if (logConfig.getLogVariables().isEmpty()) {
            mLogger.warn("LogConfig " + logConfig.getName() + " is empty!");
            return;
        }
        for (LogVariable variable : logConfig.getLogVariables()) {
            VariableType variableType = variable.getVariableType();
            
            if(!variable.isTocVariable()) { // Memory location
                //create LogTocElement to get the variableTypeId
                LogTocElement memLogTocElement = new LogTocElement();
                memLogTocElement.setCtype(variableType);
                int variableTypeId = memLogTocElement.getVariableTypeId();
                
                // logger.debug("Logging to raw memory %d, 0x%04X", var.get_storage_and_fetch_byte(), var.address)
                mLogger.debug("Logging to raw memory " + variableType.name() + ", address: " + variable.getAddress());
                // pk.data += struct.pack('<B', var.get_storage_and_fetch_byte())
                // pk.data += struct.pack('<I', var.address)
                bb.put(new byte[] {(byte) variableTypeId, (byte) variable.getAddress()});
            } else { // Item in TOC
                String name = variable.getName();
                int tocElementId = mToc.getElementId(name);
                
                TocElement logTocElement = mToc.getElementByCompleteName(name);
                int variableTypeId = logTocElement.getVariableTypeId();
                if (variableTypeId == -1) {
                    mLogger.error("VariableType " + variableType.name() + " not found in LogTocElement.VARIABLE_TYPE_MAP.");
                    //TODO: return?
                } 
                // logger.debug("Adding %s with id=%d and type=0x%02X", var.name, self.cf.log.toc.get_element_id(var.name), var.get_storage_and_fetch_byte())
//                mLogger.debug("Adding " + name + " with id " + tocElementId + ", type " + variableType.name() + " and variableTypeId " + variableTypeId);
                // pk.data += struct.pack('<B', var.get_storage_and_fetch_byte())
                // pk.data += struct.pack('<B', self.cf.log.toc.get_element_id(var.name))
                bb.put(new byte[] {(byte) variableTypeId, (byte) tocElementId});
            }
        }
        mLogger.debug("Adding log config ID " + logConfigId);

        // Create packet
        Header header = new Header(CHAN_SETTINGS, CrtpPort.LOGGING);
        CrtpPacket packet = new CrtpPacket(header.getByte(), bb.array());
        packet.setExpectedReply(new byte[]{CMD_CREATE_LOGCONFIG, (byte) logConfigId});
        this.mCrazyflie.sendPacket(packet);
    }

    /**
     * Start the logging for this entry
     */
    public void start(LogConfig logConfig) {
        //if (self.cf.link is not None):
        // TODO:
        // if (mCrazyflie.getDriver() != null && mCrazyflie.getDriver().isConnected()) {
        if (mCrazyflie.getDriver() != null) {
            if (!logConfig.isAdded()) {
                create(logConfig);
                mLogger.debug("First time log config is started, add log config");
            } else {
                mLogger.debug("Log config already registered, starting logging for ID=" + logConfig.getId());
            }

            Header header = new Header(CHAN_SETTINGS, CrtpPort.LOGGING);
            // pk.data = (CMD_START_LOGGING, self.id, self.period)
            CrtpPacket packet = new CrtpPacket(header.getByte(), new byte[] {CMD_START_LOGGING, (byte) logConfig.getId(), (byte) logConfig.getPeriod()});
            packet.setExpectedReply(new byte[]{CMD_START_LOGGING, (byte) logConfig.getId()});
            mCrazyflie.sendPacket(packet);
        }
    }

    /**
     * Stop the logging for this entry
     */
    public void stop(LogConfig logConfig) {
        // TODO:
        // if (mCrazyflie.getDriver() != null && mCrazyflie.getDriver().isConnected()) {
        if (mCrazyflie.getDriver() != null) {
            if (logConfig.getId() == -1) {
                mLogger.warn("Stopping log config, but no log config registered");
            } else {
                mLogger.debug("Sending stop logging for ID=" + logConfig.getId());
                Header header = new Header(CHAN_SETTINGS, CrtpPort.LOGGING);
                CrtpPacket packet = new CrtpPacket(header.getByte(), new byte[] {CMD_STOP_LOGGING, (byte) logConfig.getId()});
                packet.setExpectedReply(new byte[]{CMD_STOP_LOGGING, (byte) logConfig.getId()});
                mCrazyflie.sendPacket(packet);
            }
        }
    }

    /**
     * Delete this entry in the Crazyflie
     */
    public void delete(LogConfig logConfig) {
        // TODO:
        // if (mCrazyflie.getDriver() != null && mCrazyflie.getDriver().isConnected()) {
        if (mCrazyflie.getDriver() != null) {
            if (logConfig.getId() == -1) {
                mLogger.warn("Delete log config, but no log config registered");
            } else {
                mLogger.debug("Sending delete logging for ID=" + logConfig.getId());
                Header header = new Header(CHAN_SETTINGS, CrtpPort.LOGGING);
                CrtpPacket packet = new CrtpPacket(header.getByte(), new byte[] {CMD_DELETE_LOGCONFIG, (byte) logConfig.getId()});
                packet.setExpectedReply(new byte[]{CMD_DELETE_LOGCONFIG, (byte) logConfig.getId()});
                mCrazyflie.sendPacket(packet);
            }
        }
        //hacky workarounds
        logConfig.setAdded(false);
        mLogConfigs.remove(logConfig);
    }

    public List<LogConfig> getLogConfigs() {
        return mLogConfigs;
    }

    /* Log listener methods*/


    /**
     * Add a log listener
     */
    public void addLogListener(LogListener logListener) {
        mLogListeners.add(logListener);
    }

    /**
     * Remove the log listener
     */
    public void removeLogListener(LogListener logListener) {
        mLogListeners.remove(logListener);
    }

    public void notifyLogAdded(LogConfig logConfig) {
        for(LogListener ll : this.mLogListeners) {
            ll.logConfigAdded(logConfig);
        }
    }

    public void notifyLogError(LogConfig logConfig) {
        for(LogListener ll : this.mLogListeners) {
            ll.logConfigError(logConfig);
        }
    }

    public void notifyLogStarted(LogConfig logConfig) {
        for(LogListener ll : this.mLogListeners) {
            ll.logConfigStarted(logConfig);
        }
    }

    public void notifyLogDataReceived(LogConfig logConfig, Map<String, Number> data, int timestamp) {
        for(LogListener ll : this.mLogListeners) {
            ll.logDataReceived(logConfig, data, timestamp);
        }
    }


}
