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

package se.bitcraze.crazyflielib.crtp;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import se.bitcraze.crazyflielib.ConnectionListener;
import se.bitcraze.crazyflielib.crazyflie.LinkListener;
import se.bitcraze.crazyflielib.crazyradio.ConnectionData;

/**
 * CTRP Driver main class
 * This class is inherited by all the CRTP link drivers.
 *
 */
public abstract class CrtpDriver {

    protected Set<LinkListener> mLinkListeners = new CopyOnWriteArraySet<LinkListener>();

    /**
     * Driver constructor. Throw an exception if the driver is unable to open the URI
     */
    public CrtpDriver() {
    }

    /**
     * Connect the driver
     *
     * @param connectionData
     */
    public abstract void connect(ConnectionData connectionData);

    /**
     * Close the link
     */
    public abstract void disconnect();

    /**
     * Check whether the link is connected.
     *
     * @return <code>true</code> if the link is connected.
     */
    public abstract boolean isConnected();

    /**
     * Send a CRTP packet
     *
     * @param packet
     */
    public abstract void sendPacket(CrtpPacket packet);

    /**
     * Receive a CRTP packet.
     *
     * @param wait The time to wait for a packet in milliseconds. -1 means forever
     * @return One CRTP packet or None if no packet has been received.
     */
    public abstract CrtpPacket receivePacket(int wait);


    public abstract boolean scanSelected(int channel, int datarate, byte[] packet);

    /* LINK LISTENER */

    /**
     * Add a link listener
     *
     * @param linkListener
     */
    public void addLinkListener(LinkListener listener) {
        this.mLinkListeners.add(listener);
    }

    /**
     * Remove a link listener
     *
     * @param linkListener
     */
    public void removeLinkListener(LinkListener listener) {
        this.mLinkListeners.remove(listener);
    }

    protected void notifyLinkQualityUpdated(int percent) {
        for (LinkListener pl : this.mLinkListeners) {
            pl.linkQualityUpdated(percent);
        }
    }

    protected void notifyLinkError(String msg) {
        for (LinkListener pl : this.mLinkListeners) {
            pl.linkError(msg);
        }
    }

    /**
     * Add a connection listener
     *
     * @param linkListener
     */
    public abstract void addConnectionListener(ConnectionListener listener);

    public abstract void startSendReceiveThread();

    public abstract void stopSendReceiveThread();
}
