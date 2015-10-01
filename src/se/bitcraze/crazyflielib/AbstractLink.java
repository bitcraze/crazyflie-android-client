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

package se.bitcraze.crazyflielib;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import se.bitcraze.crazyflielib.crazyradio.ConnectionData;
import se.bitcraze.crazyflielib.crtp.CrtpDriver;
import se.bitcraze.crazyflielib.crtp.CrtpPacket;

/**
 * This class provides a skeletal implementation of the {@link Link} interface
 * to minimize the effort required to implement the interface.
 */
public abstract class AbstractLink extends CrtpDriver {

    private List<ConnectionListener> mConnectionListeners;
    private List<DataListener> mDataListeners;
    private ConnectionData mConnectionData;

    /**
     * Create a new abstract link.
     */
    public AbstractLink() {
        this.mConnectionListeners = Collections.synchronizedList(new LinkedList<ConnectionListener>());
        this.mDataListeners = Collections.synchronizedList(new LinkedList<DataListener>());
        this.mConnectionData = new ConnectionData(0, 0); // TODO: only placeholder value
    }

    public void addConnectionListener(ConnectionListener l) {
        this.mConnectionListeners.add(l);
    }

    public void removeConnectionListener(ConnectionListener l) {
        this.mConnectionListeners.remove(l);
    }

    public void addDataListener(DataListener l) {
        this.mDataListeners.add(l);
    }

    public void removeDataListener(DataListener l) {
        this.mDataListeners.remove(l);
    }

    /**
     * Handle the response from the Crazyflie.
     *
     * @param packet
     */
    protected void notifyDataListeners(CrtpPacket packet) {
        for (DataListener dataListener : mDataListeners) {
            if (dataListener.getPort() == packet.getHeader().getPort()) {
                dataListener.dataReceived(packet);
            }
        }
    }

    /**
     * Notify all registered listeners about a requested connection.
     */
    protected void notifyConnectionRequested() {
        synchronized (this.mConnectionListeners) {
            for (ConnectionListener cl : this.mConnectionListeners) {
                cl.connectionRequested(mConnectionData.toString());
            }
        }
    }

    /**
     * Notify all registered listeners about an initiated connection.
     */
    protected void notifyConnected() {
        synchronized (this.mConnectionListeners) {
            for (ConnectionListener cl : this.mConnectionListeners) {
                cl.connected(mConnectionData.toString());
            }
        }
    }

    /**
     * Notify all registered listeners about a setup connection.
     */
    protected void notifySetupFinished() {
        synchronized (this.mConnectionListeners) {
            for (ConnectionListener cl : this.mConnectionListeners) {
                cl.setupFinished(mConnectionData.toString());
            }
        }
    }

    /**
     * Notify all registered listeners about a lost connection.
     *
     * @param msg
     */
    protected void notifyConnectionLost(String msg) {
        synchronized (this.mConnectionListeners) {
            for (ConnectionListener cl : this.mConnectionListeners) {
                cl.connectionLost(mConnectionData.toString(), msg);
            }
        }
    }

    /**
     * Notify all registered listeners about a failed connection attempt.
     *
     * @param msg
     */
    protected void notifyConnectionFailed(String msg) {
        synchronized (this.mConnectionListeners) {
            for (ConnectionListener cl : this.mConnectionListeners) {
                cl.connectionFailed(mConnectionData.toString(), msg);
            }
        }
    }

    /**
     * Notify all registered listeners about a disconnect.
     */
    protected void notifyDisconnected() {
        synchronized (this.mConnectionListeners) {
            for (ConnectionListener cl : this.mConnectionListeners) {
                cl.disconnected(mConnectionData.toString());
            }
        }
    }

    /**
     * Notify all registered listeners about the link status.
     *
     * @param percent quality of the link (0 = connection lost, 100 = good)
     * @see ConnectionListener#linkQualityUpdate(Link, int)
     */
    protected void notifyLinkQualityUpdated(int percent) {
        synchronized (this.mConnectionListeners) {
            for (ConnectionListener cl : this.mConnectionListeners) {
                cl.linkQualityUpdated(percent);
            }
        }
    }
}
