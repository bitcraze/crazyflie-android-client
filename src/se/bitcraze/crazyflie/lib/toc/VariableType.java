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

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.bitcraze.crazyflie.lib.crtp.CrtpPacket;

public enum VariableType {
    UINT8_T (1),
    UINT16_T (2),
    UINT32_T (4),
    UINT64_T (8),
    INT8_T (1),
    INT16_T (2),
    INT32_T (4),
    INT64_T (8),
    FLOAT (4),
    DOUBLE (8);

    int mSize;
    
    VariableType(int size) {
        this.mSize = size;
    }
    
    final Logger mLogger = LoggerFactory.getLogger(this.getClass().getSimpleName());

    /**
     * Parse one variable of the given type.
     *
     * @param buffer the buffer to read raw data from
     * @return the parsed variable
     */
    public Number parse(ByteBuffer buffer) {
        ByteBuffer tempBuffer = ByteBuffer.allocate(8).order(CrtpPacket.BYTE_ORDER);
        //TODO: simplify
        tempBuffer.put(buffer.get());
        tempBuffer.put(buffer.get());
        tempBuffer.put(buffer.get());
        tempBuffer.put(buffer.get());
        if(this == INT64_T || this == DOUBLE) {
            tempBuffer.put(buffer.get());
            tempBuffer.put(buffer.get());
            tempBuffer.put(buffer.get());
            tempBuffer.put(buffer.get());
        }
        tempBuffer.rewind();
        switch (this) {
            case UINT8_T:
                return ((short) tempBuffer.get()) & 0xff;
            case UINT16_T:
                return ((int) tempBuffer.getShort()) & 0xffff;
            case UINT32_T:
                return ((long) tempBuffer.getInt()) & 0xffffffffL;
            case UINT64_T:
                mLogger.warn("UINT64_T not yet implemented");
                return -1;
            case INT8_T:
                return tempBuffer.get();
            case INT16_T:
                return tempBuffer.getShort();
            case INT32_T:
                return tempBuffer.getInt();
            case INT64_T:
                return tempBuffer.getLong();
            case FLOAT:
                return tempBuffer.getFloat();
            case DOUBLE:
                return tempBuffer.getDouble();
            default:
                mLogger.warn("Parsing " + this.name() + " is not yet implemented");
                break;
        }
        return -1;
    }

    /**
     * Parse one variable of the given type.
     *
     * @param value
     * @return
     */
    public byte[] parse(Number value) {
        ByteBuffer tempBuffer4 = ByteBuffer.allocate(4).order(CrtpPacket.BYTE_ORDER);

        //Use ByteBuffer with 8 bytes for INT64_T and DOUBLE
        ByteBuffer tempBuffer8 = ByteBuffer.allocate(8).order(CrtpPacket.BYTE_ORDER);

        switch (this) {
            case UINT8_T:
                tempBuffer4.putShort((short) (value.byteValue() & 0xff));
                break;
            case UINT16_T:
                tempBuffer4.putInt((int) (value.shortValue() & 0xffff));
                break;
            case UINT32_T:
                tempBuffer4.putInt((int) (((long) value.intValue()) & 0xffffffffL));
                //tempBuffer.putLong((long) (value.intValue() & 0xffffffffL)); //only works if ByteBuffer is 8 bytes long
                break;
            case UINT64_T:
                mLogger.warn("UINT64_T not yet implemented");
                break;
            case INT8_T:
                tempBuffer4.put(value.byteValue());
                break;
            case INT16_T:
                tempBuffer4.putShort(value.shortValue());
                break;
            case INT32_T:
                tempBuffer4.putInt(value.intValue());
                break;
            case INT64_T:
                tempBuffer8.putLong(value.longValue());
                tempBuffer8.rewind();
                return tempBuffer8.array();
            case FLOAT:
                tempBuffer4.putFloat(value.floatValue());
                break;
            case DOUBLE:
                tempBuffer8.putDouble(value.doubleValue());
                tempBuffer8.rewind();
                return tempBuffer8.array();
            default:
                mLogger.warn("Parsing " + this.name() + " is not yet implemented");
                break;
        }
        tempBuffer4.rewind();
        return tempBuffer4.array();
    }

    public int getSize() {
        if (this == UINT64_T) {
            mLogger.warn("UINT64_T not yet implemented");
        }
        return this.mSize;
    }
}