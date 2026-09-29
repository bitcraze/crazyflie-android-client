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

package se.bitcraze.crazyflie.lib.crazyflie;

import java.nio.charset.StandardCharsets;
import java.util.function.IntConsumer;
import se.bitcraze.crazyflie.lib.crtp.CrtpPacket;
import se.bitcraze.crazyflie.lib.crtp.CrtpPort;

/** Detects firmware protocol support before selecting the TOC and parameter formats. */
public class PlatformService implements AutoCloseable {
    private final Crazyflie crazyflie;
    private IntConsumer callback;
    private final DataListener sourceListener = new DataListener(CrtpPort.LINKCTRL) {
        @Override public void dataReceived(CrtpPacket packet) {
            if (packet.getHeader().getChannel() != 1 || callback == null) { return; }
            String source = new String(packet.getPayload(), StandardCharsets.US_ASCII);
            if (source.startsWith("Bitcraze Crazyflie")) {
                CrtpPacket request = new CrtpPacket((byte) 0xd1, new byte[]{0});
                request.setExpectedReply(new byte[]{0});
                crazyflie.sendPacket(request);
            } else {
                finish(-1);
            }
        }
    };
    private final DataListener versionListener = new DataListener(CrtpPort.PLATFORM) {
        @Override public void dataReceived(CrtpPacket packet) {
            byte[] data = packet.getPayload();
            if (packet.getHeader().getChannel() == 1 && data.length >= 2 && data[0] == 0 && callback != null) {
                finish(data[1] & 0xff);
            }
        }
    };

    public PlatformService(Crazyflie crazyflie) { this.crazyflie = crazyflie; }

    public void fetchProtocolVersion(IntConsumer callback) {
        this.callback = callback;
        crazyflie.addDataListener(sourceListener);
        crazyflie.addDataListener(versionListener);
        // Legacy firmware answers this source request too, but without the versioning marker.
        crazyflie.sendPacket(new CrtpPacket((byte) 0xf1, new byte[]{0}));
    }

    private void finish(int version) {
        IntConsumer completed = callback;
        close();
        completed.accept(version);
    }

    @Override public void close() {
        callback = null;
        crazyflie.removeDataListener(sourceListener);
        crazyflie.removeDataListener(versionListener);
    }
}
