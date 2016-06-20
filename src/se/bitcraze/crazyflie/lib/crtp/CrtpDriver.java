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

package se.bitcraze.crazyflie.lib.crtp;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import se.bitcraze.crazyflie.lib.crazyflie.ConnectionListener;
import se.bitcraze.crazyflie.lib.crazyradio.ConnectionData;

/**
 * CTRP Driver main class
 * This class is inherited by all the CRTP link drivers.
 *
 */
public abstract class CrtpDriver {

    protected Set<ConnectionListener> mConnectionListeners = new CopyOnWriteArraySet<ConnectionListener>();

    protected ConnectionData mConnectionData;

    /**
     * Driver constructor. Throw an exception if the driver is unable to open the URI
     */
    public CrtpDriver() {
    }

    /**
     * Connect the driver
     *
     * @param connectionData
     * @throws IOException
     */
    public abstract void connect(ConnectionData connectionData) throws IOException;

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

    /* CONNECTION LISTENER */

    public void addConnectionListener(ConnectionListener listener) {
        this.mConnectionListeners.add(listener);
    }

    public void removeConnectionListener(ConnectionListener listener) {
        this.mConnectionListeners.remove(listener);
    }

    /**
     * Notify all registered listeners about a requested connection
     */
    protected void notifyConnectionRequested() {
        for (ConnectionListener cl : this.mConnectionListeners) {
            cl.connectionRequested(mConnectionData.toString());
        }
    }

    /**
     * Notify all registered listeners about a connect.
     */
    public void notifyConnected() {
        for (ConnectionListener cl : this.mConnectionListeners) {
            cl.connected(mConnectionData.toString());
        }
    }

    /**
     * Notify all registered listeners about a finished setup.
     */
    public void notifySetupFinished() {
        for (ConnectionListener cl : this.mConnectionListeners) {
            cl.setupFinished(mConnectionData.toString());
        }
    }

    /**
     * Notify all registered listeners about a failed connection attempt.
     *
     * @param msg
     */
    protected void notifyConnectionFailed(String msg) {
        for (ConnectionListener cl : this.mConnectionListeners) {
            cl.connectionFailed(mConnectionData.toString(), msg);
        }
    }

    /**
     * Notify all registered listeners about a lost connection.
     *
     * @param msg
     */
    protected void notifyConnectionLost(String msg) {
        for (ConnectionListener cl : this.mConnectionListeners) {
            cl.connectionLost(mConnectionData.toString(), msg);
        }
    }

    /**
     * Notify all registered listeners about a disconnect.
     */
    protected void notifyDisconnected() {
        for (ConnectionListener cl : this.mConnectionListeners) {
            cl.disconnected(mConnectionData.toString());
        }
    }

    /**
     * Notify all registered listeners about a link quality update.
     *
     * @param percent quality of the link (0 = connection lost, 100 = good)
     */
    protected void notifyLinkQualityUpdated(int percent) {
        for (ConnectionListener cl : this.mConnectionListeners) {
            cl.linkQualityUpdated(percent);
        }
    }

    public abstract void startSendReceiveThread();

    public abstract void stopSendReceiveThread();
}
