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

package se.bitcraze.crazyflie.lib.crtp;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Packet containing console text from the Crazyflie.
 */
public class ConsolePacket extends CrtpPacket {

    public static final Charset CHARSET = Charset.forName("US-ASCII");

    private final String mText;

    public ConsolePacket(String text) {
        super(0, CrtpPort.CONSOLE);
        this.mText = text;
    }

    /**
     * Get the text contained in the packet.
     *
     * @return the text
     */
    public String getText() {
        return mText;
    }

    /*
     * (non-Javadoc)
     * @see
     * se.bitcraze.crazyflielib.crtp.CRTPPacket#serializeData(java.nio.ByteBuffer
     * )
     */
    @Override
    protected void serializeData(ByteBuffer buffer) {
        buffer.put(mText.getBytes(CHARSET));
    }

    /*
     * (non-Javadoc)
     * @see se.bitcraze.crazyflielib.crtp.CRTPPacket#getDataByteCount()
     */
    @Override
    protected int getDataByteCount() {
        return mText.getBytes(CHARSET).length;
    }

    /**
     * Construct a console packet using given data.
     *
     * @param payload the data (must not include the CRTP header)
     * @return parsed console packet
     */
    public static ConsolePacket parse(byte[] payload) {
        return new ConsolePacket(new String(payload, CHARSET));
    }

    @Override
    public String toString() {
        return "ConsolePacket: " + getText();
    }

}
