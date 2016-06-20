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

package se.bitcraze.crazyflie.lib.bootloader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Target {

    private int mId;
    private int mProtocolVersion;
    private int mPageSize;
    private int mBufferPages;
    private int mFlashPages;
    private int mStartPage;
    private String mCpuId; //12 bytes
    private Object mData;

    public Target(int id) {
        this.mId = id;
        this.mProtocolVersion = 0xFF;
        this.mPageSize = 0;
        this.mBufferPages = 0;
        this.mFlashPages = 0;
        this.mStartPage = 0;
        this.mCpuId = "";
        this.mData = null;
    }

    public void parseData(byte[] data) {
         // tab = struct.unpack("BBHHHH", pk.data[0:10])
        // 2 byte offset
        ByteBuffer bb = ByteBuffer.wrap(data, 2, data.length-2).order(ByteOrder.LITTLE_ENDIAN);
        this.mPageSize = bb.getShort();
        this.mBufferPages = bb.getShort();
        this.mFlashPages = bb.getShort();
        this.mStartPage = bb.getShort();

        // cpuid = struct.unpack("B" * 12, pk.data[10:22])
        //TODO: self.targets[target_id].addr = target_id

        int cpuIdSize = 12;
        // Concatenate CPU ID
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("%02X", bb.get()));
        for (int i = 1; i < cpuIdSize; i++) {
            sb.append(String.format(":%02X", bb.get()));
        }
        this.mCpuId = sb.toString();

        if (data.length > 22) {
            this.mProtocolVersion = data[22];
        }
    }

    public int getId() {
        return mId;
    }

    public int getProtocolVersion() {
        return mProtocolVersion;
    }

    public int getPageSize() {
        return mPageSize;
    }

    public int getBufferPages() {
        return mBufferPages;
    }

    public int getFlashPages() {
        return mFlashPages;
    }

    public int getStartPage() {
        return mStartPage;
    }

    public String getCpuId() {
        return mCpuId;
    }

    public Object getData() {
        return mData;
    }

    public int getAvailableFlash() {
        return ((this.mFlashPages - this.mStartPage) * this.mPageSize / 1024);
    }

    @Override
    public String toString() {
        String ret = "";
        //ret += "Target info: {} (0x{:X})\n".format(TargetTypes.to_string(self.id), self.id)
        ret += "Target info: "+ TargetTypes.toString(this.mId) + String.format(" (0x%02X)\n", this.mId);
        ret += "Flash pages: " + this.mFlashPages + " | Page size: " + this.mPageSize + " | Buffer pages: " + this.mBufferPages + " | Start page: " + this.mStartPage + "\n";
        ret += getAvailableFlash() + " KBytes of flash available for firmware image.";
        return ret;
    }


    /* TargetTypes */

    public static class TargetTypes {
        public final static int STM32 = 0xFF;
        public final static int NRF51 = 0xFE;

        public static String toString(int target) {
            if (target == TargetTypes.STM32) {
                return "stm32";
            } else if (target == TargetTypes.NRF51) {
                return "nrf51";
            }
            return "Unknown";
        }

        public static int fromString(String name) {
            if ("stm32".equalsIgnoreCase(name)) {
                return TargetTypes.STM32;
            } else if ("nrf51".equalsIgnoreCase(name)) {
                return TargetTypes.NRF51;
            }
            return 0;
        }
    }

}
