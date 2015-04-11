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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import se.bitcraze.crazyflielib.crtp.ConsolePacket;
import se.bitcraze.crazyflielib.crtp.CrtpPort;
import android.util.Log;

/**
 * This class provides a skeletal implementation of the {@link Link} interface
 * to minimize the effort required to implement the interface.
 */
public abstract class AbstractLink implements Link {

    private List<ConnectionListener> mConnectionListeners;
    private List<DataListener> mDataListeners;

    /**
     * Create a new abstract link.
     */
    public AbstractLink() {
        this.mConnectionListeners = Collections.synchronizedList(new LinkedList<ConnectionListener>());
        this.mDataListeners = Collections.synchronizedList(new LinkedList<DataListener>());
    }

    /*
     * (non-Javadoc)
     * @see
     * se.bitcraze.crazyflielib.Link#addConnectionListener(se.bitcraze.crazyflielib
     * .ConnectionListener)
     */
    @Override
    public void addConnectionListener(ConnectionListener l) {
        this.mConnectionListeners.add(l);
    }

    /*
     * (non-Javadoc)
     * @see se.bitcraze.crazyflielib.Link#removeConnectionListener(se.bitcraze.
     * crazyflielib.ConnectionListener)
     */
    @Override
    public void removeConnectionListener(ConnectionListener l) {
        this.mConnectionListeners.remove(l);
    }

    /*
     * (non-Javadoc)
     * @see
     * se.bitcraze.crazyflielib.Link#addDataListener(se.bitcraze.crazyflielib
     * .DataListener)
     */
    @Override
    public void addDataListener(DataListener l) {
        this.mDataListeners.add(l);
    }

    /*
     * (non-Javadoc)
     * @see
     * se.bitcraze.crazyflielib.Link#removeDataListener(se.bitcraze.crazyflielib
     * .DataListener)
     */
    @Override
    public void removeDataListener(DataListener l) {
        this.mDataListeners.remove(l);
    }

    /**
     * Handle the response from the Crazyflie. Parses the CRPT packet and inform
     * registered listeners.
     *
     * @param data the data received from the Crazyflie. Must not include any
     *            headers or other attachments added by the link.
     */
    protected void handleResponse(byte[] data) {
        if (data.length >= 1) {
            switch (CrtpPort.getByNumber((byte) (data[0] >> 4))) {
                case CONSOLE:
                    final ConsolePacket p = ConsolePacket.parse(Arrays.copyOfRange(data, 1, data.length));
                    Log.i(AbstractLink.class.getName(), "received console packet: " + p.getText());
                    break;
                    // TODO implement other types
                default:
                    Log.d(AbstractLink.class.getName(), "packet contains unknown port");
                    break;
            }
        }
    }

    /**
     * Notify all registered listeners about an initiated connection.
     */
    protected void notifyConnectionInitiated() {
        synchronized (this.mConnectionListeners) {
            for (ConnectionListener cl : this.mConnectionListeners) {
                cl.connectionInitiated(this);
            }
        }
    }

    /**
     * Notify all registered listeners about a setup connection.
     */
    protected void notifyConnectionSetupFinished() {
        synchronized (this.mConnectionListeners) {
            for (ConnectionListener cl : this.mConnectionListeners) {
                cl.connectionSetupFinished(this);
            }
        }
    }

    /**
     * Notify all registered listeners about a disconnect.
     */
    protected void notifyDisconnected() {
        synchronized (this.mConnectionListeners) {
            for (ConnectionListener cl : this.mConnectionListeners) {
                cl.disconnected(this);
            }
        }
    }

    /**
     * Notify all registered listeners about a lost connection.
     */
    protected void notifyConnectionLost() {
        synchronized (this.mConnectionListeners) {
            for (ConnectionListener cl : this.mConnectionListeners) {
                cl.connectionLost(this);
            }
        }
    }

    /**
     * Notify all registered listeners about a failed connection attempt.
     */
    protected void notifyConnectionFailed() {
        synchronized (this.mConnectionListeners) {
            for (ConnectionListener cl : this.mConnectionListeners) {
                cl.connectionFailed(this);
            }
        }
    }

    /**
     * Notify all registered listeners about the link status.
     *
     * @param quality quality of the link (0 = connection lost, 100 = good)
     * @see ConnectionListener#linkQualityUpdate(Link, int)
     */
    protected void notifyLinkQuality(int quality) {
        synchronized (this.mConnectionListeners) {
            for (ConnectionListener cl : this.mConnectionListeners) {
                cl.linkQualityUpdate(this, quality);
            }
        }
    }
}
