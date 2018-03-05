/**
 *    ||          ____  _ __
 * +------+      / __ )(_) /_______________ _____  ___
 * | 0xBC |     / __  / / __/ ___/ ___/ __ `/_  / / _ \
 * +------+    / /_/ / / /_/ /__/ /  / /_/ / / /_/  __/
 *  ||  ||    /_____/_/\__/\___/_/   \__,_/ /___/\___/
 *
 * Copyright (C) 2018 Bitcraze AB
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

package se.bitcraze.crazyfliecontrol.console;

import android.util.Log;

import se.bitcraze.crazyflie.lib.crazyflie.DataListener;
import se.bitcraze.crazyflie.lib.crtp.CrtpPacket;
import se.bitcraze.crazyflie.lib.crtp.CrtpPort;
import se.bitcraze.crazyfliecontrol2.MainActivity;

public class ConsoleListener extends DataListener {

    private static final String LOG_TAG = "ConsoleListener";
    private MainActivity mMainActivity;
    private StringBuffer consoleBuffer = new StringBuffer();

    public ConsoleListener() {
        super(CrtpPort.CONSOLE);
    }

    public void setMainActivity(MainActivity mainActivity) {
        this.mMainActivity = mainActivity;
    }

    @Override
    public void dataReceived(CrtpPacket packet) {
        //skip packet when it only contains zeros
        if (containsOnly00(packet.getPayload())) {
            return;
        }
        String parsedText = parseConsoleText(packet);
        Log.d(LOG_TAG, "Received console packet: " + parsedText);
        mMainActivity.appendToConsole(parsedText);
    }

    private String parseConsoleText(CrtpPacket packet) {
        byte[] payload = packet.getPayload();
        String result = "";
        String trimmedText = new String(payload).trim();
        if (contains0A(payload)) {
            consoleBuffer.append(trimmedText);
            result = consoleBuffer.toString();
            consoleBuffer = new StringBuffer();
        } else {
            consoleBuffer.append(trimmedText);
        }
        return result;
    }

    private boolean contains0A(byte[] payload) {
        for (byte b : payload) {
            if (b == 10) {
                return true;
            }
        }
        return false;
    }

    private boolean containsOnly00(byte[] payload) {
        for (byte b : payload) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }
}
