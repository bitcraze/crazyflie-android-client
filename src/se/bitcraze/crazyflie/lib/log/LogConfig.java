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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import se.bitcraze.crazyflie.lib.crtp.CrtpPacket;
import se.bitcraze.crazyflie.lib.toc.VariableType;


/**
 * Represents a log configuration that contains different {@link LogVariable}s.
 *
 * TODO: deal with maximum number of variables per log config
 * <br/>
 * NOTE: LogConfig is a pure data structure that can not interact directly with the crazyflie,
 * that's why it does not contain the following methods: create(), start(), stop(), delete().
 * These methods are contained in the Logg class instead.<br/>
 *
 */
public class LogConfig {

    private int mId = -1;
    private String mName;
    private int mPeriodInMs;

    private int mErrNo;

    private List<LogVariable> logVariables = new ArrayList<LogVariable>();
    private boolean mAdded = false;
    private boolean mStarted = false;
    private boolean mValid = false;

    /**
     * Create a log configuration
     *
     * @param name
     * @param periodInMs
     */
    public LogConfig(String name, int periodInMs) {
        this.mName = name;
        this.mPeriodInMs = periodInMs;
    }

    /**
     * Create a log configuration with the default period of 100ms
     *
     * @param name
     */
    public LogConfig(String name) {
        this(name, 100);
    }

    /**
     * Add a log variable to the log configuration
     *
     * @param name Complete name of the variable in the form group.name
     * @param type
     */
    public void addVariable(String name, VariableType type){
        logVariables.add(new LogVariable(name, type));
    }

    /**
     * Add a log variable to the log configuration
     *
     * @param name Complete name of the variable in the form group.name
     */
    public void addVariable(String name){
        logVariables.add(new LogVariable(name));
    }

    /**
     * Add a raw memory position to the log configuration
     *
     * @param name Arbitrary name of the variable
     * @param type
     * @param address The address of the data
     */
    public void addMemory(String name, VariableType type, int address) {
        logVariables.add(new LogVariable(name, type, LogVariable.MEM_TYPE, address));
    }

    /**
     * Returns log variable of the log configuration
     *
     * @return list of log variables
     */
    public List<LogVariable> getLogVariables(){
        return this.logVariables;
    }

    /**
     * Returns the name of the log configuration
     *
     * @return the name of the log configuration
     */
    public String getName() {
        return mName;
    }

    /**
     * Sets the name of the log configuration
     *
     * @param name
     */
    public void setName(String name) {
        this.mName = name;
    }

    /**
     * Returns the ID of the log configuration
     *
     * @return the ID of the log configuration
     */
    public int getId() {
        return mId;
    }

    /**
     * Sets the ID of the log configuration
     *
     * @param id
     */
    public void setId(int id) {
        this.mId = id;
    }

    /**
     * Returns the period in milliseconds
     *
     * @return the period in milliseconds
     */
    public int getPeriodInMs() {
        return mPeriodInMs;
    }

    /**
     * Sets the period in milliseconds
     *
     * @param periodInMs
     */
    public void setPeriodInMs(int periodInMs) {
        this.mPeriodInMs = periodInMs;
    }

    public int getPeriod() {
        return getPeriodInMs() / 10;
    }

    public void setErrNo(int errNo) {
        this.mErrNo = errNo;
    }

    public int getErrNo() {
        return this.mErrNo;
    }

    public String getErrMsg() {
        return Logg.getErrorMsg(this.mErrNo);
    }

    public boolean isAdded(){
        return this.mAdded;
    }

    public void setAdded(boolean added){
        this.mAdded = added;
    }

    public boolean isStarted() {
        return mStarted;
    }

    public void setStarted(boolean mStarted) {
        this.mStarted = mStarted;
    }

    public boolean isValid() {
        return mValid;
    }

    public void setValid(boolean valid) {
        this.mValid = valid;
    }

    /**
     * Unpack received logging data so it represent real values according to the configuration in the entry
     * @return
     */
    // def unpack_log_data(self, log_data, timestamp):
    public Map<String, Number> unpackLogData(byte[] logData) {
        ByteBuffer logVariablesRaw = ByteBuffer.wrap(logData).order(CrtpPacket.BYTE_ORDER);
        Map<String, Number> logDataMap = new ConcurrentHashMap<String, Number>();
        for(LogVariable logVariable : this.getLogVariables()) {
            Number parsedValue = logVariable.getVariableType().parse(logVariablesRaw);
            logDataMap.put(logVariable.getName(), parsedValue);
            //TODO: write to logger?
        }
        //TODO: timestamps and callback
        return logDataMap;
        /*
        ret_data = {}
        data_index = 0
        for var in self.variables:
            size = LogTocElement.get_size_from_id(var.fetch_as)
            name = var.name
            unpackstring = LogTocElement.get_unpack_string_from_id(var.fetch_as)
            value = struct.unpack(unpackstring, log_data[data_index:data_index + size])[0]
            data_index += size
            ret_data[name] = value
        self.data_received_cb.call(timestamp, ret_data, self)
        */
    }
}
