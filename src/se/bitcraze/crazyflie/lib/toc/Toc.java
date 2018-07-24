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

package se.bitcraze.crazyflie.lib.toc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Container for TocElements
 */
public class Toc {

    final Logger mLogger = LoggerFactory.getLogger("Toc");

    private int mCrc;

    private Map<String, TocElement> mTocElementMap = new HashMap<String, TocElement>();

    private final static Map<Integer, VariableType> mVariableTypeMapParam = new HashMap<Integer, VariableType>(10);
    private final static Map<Integer, VariableType> mVariableTypeMapLog = new HashMap<Integer, VariableType>(10);

    static {
        fillVariableTypeMapParam();
        fillVariableTypeMapLog();
    }
    
    public Toc() {
    }

    public void setCrc(int crc) {
        this.mCrc = crc;
    }

    public int getCrc() {
        return this.mCrc;
    }

    /**
     * Clear the TOC
     */
    public void clear() {
        this.mTocElementMap.clear();
    }

    /**
     * Add a new TocElement to the TOC container
     *
     * @param tocElement
     */
    public void addElement(TocElement tocElement) {
        if (tocElement.getGroup().isEmpty()) {
            throw new IllegalStateException("TocElement has no group!");
        }
        mTocElementMap.put(tocElement.getCompleteName(), tocElement);
    }

    /**
     * Get a TocElement element identified by complete name from the container.
     *
     * @param completeName
     * @return
     */
    public TocElement getElementByCompleteName(String completeName) {
        return mTocElementMap.get(completeName);
    }

    /**
     * Get the TocElement element id-number of the element with the supplied name.
     *
     * @param completeName
     * @return
     */
    public int getElementId(String completeName) {
        TocElement tocElement= mTocElementMap.get(completeName);
        if(tocElement != null) {
            return tocElement.getIdent();
        }
        mLogger.warn("Unable to find TOC element for complete name '" + completeName + "'");
        return -1;
    }

    /**
     * Get a TocElement element identified by name and group from the container
     *
     * @param group
     * @param name
     * @return
     */
    public TocElement getElement(String group, String name) {
        return getElementByCompleteName(group + "." + name);
    }

    /**
     * Get a TocElement element identified by index number from the container
     *
     * @param ident
     * @return
     */
    public TocElement getElementById(int ident) {
        for(TocElement tocElement : mTocElementMap.values()) {
            if (tocElement.getIdent() == ident) {
                return tocElement;
            }
        }
        mLogger.warn("Unable to find TOC element with ID " + ident);
        return null;
    }

    /**
     * Get TocElements as list sorted by ID
     *
     * @return list of TocElements sorted by ID
     */
    //TODO: generate list not every time
    public List<TocElement> getElements() {
        List<TocElement> values = new ArrayList<TocElement>(mTocElementMap.values());
        Collections.sort(values);
        return values;
    }

    public Map<String, TocElement> getTocElementMap() {
        return mTocElementMap;
    }

    public void setTocElementMap(Map<String, TocElement> map) {
        this.mTocElementMap = map;
    }

    public int getTocSize() {
        return mTocElementMap.size();
    }


    private static void fillVariableTypeMapParam() {
        /*
        types = {0x08: ("uint8_t",  '<B'),
                 0x09: ("uint16_t", '<H'),
                 0x0A: ("uint32_t", '<L'),
                 0x0B: ("uint64_t", '<Q'),
                 0x00: ("int8_t",   '<b'),
                 0x01: ("int16_t",  '<h'),
                 0x02: ("int32_t",  '<i'),
                 0x03: ("int64_t",  '<q'),
                 0x05: ("FP16",     ''),
                 0x06: ("float",    '<f'),
                 0x07: ("double",   '<d')}
        */
        
        mVariableTypeMapParam.put(0x00, VariableType.INT8_T);
        mVariableTypeMapParam.put(0x01, VariableType.INT16_T);
        mVariableTypeMapParam.put(0x02, VariableType.INT32_T);
        mVariableTypeMapParam.put(0x03, VariableType.INT64_T);
        /*TODO: 0x05 FP16*/
        mVariableTypeMapParam.put(0x06, VariableType.FLOAT);
        mVariableTypeMapParam.put(0x07, VariableType.DOUBLE);
        mVariableTypeMapParam.put(0x08, VariableType.UINT8_T);
        mVariableTypeMapParam.put(0x09, VariableType.UINT16_T);
        mVariableTypeMapParam.put(0x0A, VariableType.UINT32_T);
        mVariableTypeMapParam.put(0x0B, VariableType.UINT64_T);
    }

    private static void fillVariableTypeMapLog() {
        /*
        types = {0x01: ("uint8_t",  '<B', 1),
                 0x02: ("uint16_t", '<H', 2),
                 0x03: ("uint32_t", '<L', 4),
                 0x04: ("int8_t",   '<b', 1),
                 0x05: ("int16_t",  '<h', 2),
                 0x06: ("int32_t",  '<i', 4),
                 0x08: ("FP16",     '<h', 2),
                 0x07: ("float",    '<f', 4)}
        */
        
        mVariableTypeMapLog.put(0x01, VariableType.UINT8_T);
        mVariableTypeMapLog.put(0x02, VariableType.UINT16_T);
        mVariableTypeMapLog.put(0x03, VariableType.UINT32_T);
        mVariableTypeMapLog.put(0x04, VariableType.INT8_T);
        mVariableTypeMapLog.put(0x05, VariableType.INT16_T);
        mVariableTypeMapLog.put(0x06, VariableType.INT32_T);
        mVariableTypeMapLog.put(0x07, VariableType.FLOAT);
        /*TODO: 0x08 FP16*/
    }

    public Map<Integer, VariableType> getVariableTypeMapParam() {
        return mVariableTypeMapParam;
    }

    public Map<Integer, VariableType> getVariableTypeMapLog() {
        return mVariableTypeMapLog;
    }
    
    public int getVariableTypeIdLog (VariableType vt) {
        for (Entry<Integer, VariableType> entry : getVariableTypeMapLog().entrySet()) {
            if (entry.getValue() == vt) {
                return entry.getKey();
            }
        }
        return -1;
    }
    
}
