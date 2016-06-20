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

public class Cf1Config {

    private int mChannel;
    private int mSpeed;
    private float mPitchTrim;
    private float mRollTrim;

    public Cf1Config() {
    }

    public Cf1Config(int channel, int speed, int pitchTrim, int rollTrim) {
        this.mChannel = channel;
        this.mSpeed = speed;
        this.mPitchTrim = pitchTrim;
        this.mRollTrim = rollTrim;
    }

    public byte[] prepareConfig() {
        ByteBuffer bb = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
        bb.put((byte) '0');
        bb.put((byte) 'x');
        bb.put((byte) 'B');
        bb.put((byte) 'C');
        bb.put((byte) 0x00);
        bb.put((byte) mChannel);
        bb.put((byte) mSpeed);
        bb.putFloat(mPitchTrim);
        bb.putFloat(mRollTrim);
        int checksum = checksum256(bb.array());
        bb.put((byte) (256 - checksum));
        return bb.array();
    }

    // public for testing
    public int checksum256(byte[] array) {
        // return reduce(lambda x, y: x + y, map(ord, st)) % 256
        int result = array[0];
        for (int i = 1; i < array.length; i++) {
            result += (int) array[i];
        }
        return result % 256;
    }

    // TODO: use constructor instead of parse method?
    public void parse(byte[] byteArray) {
        // [channel, speed, pitchTrim, rollTrim] = struct.unpack("<BBff", data[5:15])
        int offset = 5;
        ByteBuffer cf1ConfigBuffer = ByteBuffer.wrap(byteArray, offset, 10).order(ByteOrder.LITTLE_ENDIAN);
        this.mChannel = (int) cf1ConfigBuffer.get();
        this.mSpeed = (int) cf1ConfigBuffer.get();
        this.mPitchTrim = cf1ConfigBuffer.getFloat();
        this.mRollTrim = cf1ConfigBuffer.getFloat();
    }

    @Override
    public String toString() {
        return "CF1Config: Channel: " + mChannel + ", Speed: " + mSpeed + ", PitchTrim: " + mPitchTrim + ", RollTrim: " + mRollTrim;
    }
}