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

import se.bitcraze.crazyflielib.crtp.CrtpPacket;

/**
 * Representation of a link to the Crazyflie.
 */
public interface Link {
    /**
     * Connect to the Crazyflie.
     */
    public void connect();

    /**
     * Disconnect from the Crazyflie.
     */
    public void disconnect();

    /**
     * Check whether the link is connected.
     *
     * @return <code>true</code> if the link is connected.
     */
    public boolean isConnected();

    /**
     * Send data to the Crazyflie.
     *
     * @param p the packet of data to send.
     */
    public void send(CrtpPacket p);

    /**
     * Add a listener to receive notifications about the connection status.
     *
     * @param l the listener to add.
     */
    public void addConnectionListener(ConnectionListener l);

    /**
     * Remote a previously registered connection listener. If the listener has
     * not been registered before, nothing is done.
     *
     * @param l the listener to remove.
     */
    public void removeConnectionListener(ConnectionListener l);

    /**
     * Add a listener to receive notifications about incoming data.
     *
     * @param l the listener to add.
     */
    public void addDataListener(DataListener l);

    /**
     * Remote a previously registered data listener. If the listener has not
     * been registered before, nothing is done.
     *
     * @param l the listener to remove.
     */
    public void removeDataListener(DataListener l);
}
