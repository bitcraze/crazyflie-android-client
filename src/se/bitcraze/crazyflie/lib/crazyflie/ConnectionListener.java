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

/**
 * Interface for receiving notifications about the connection status.
 */
public interface ConnectionListener {

    /**
     * Callback when the user requests a connection
     *
     * @param connectionInfo
     */
    public void connectionRequested(String connectionInfo);

    /**
     * Callback when the first packet in a new link is received
     *
     * @param connectionInfo
     */
    public void connected(String connectionInfo);

    /**
     * Callback when a Crazyflie has been connected and the TOCs have been downloaded.
     *
     * @param connectionInfo
     */
    public void setupFinished(String connectionInfo);

    /**
     * Callback when initial connection fails (i.e no Crazyflie at the specified address)
     *
     * @param connectionInfo
     * @param msg
     */
    public void connectionFailed(String connectionInfo, String msg);

    /**
     * Callback when disconnected after a connection has been made (i.e Crazyflie moves out of range)
     *
     * @param connectionInfo
     * @param msg
     */
    public void connectionLost(String connectionInfo, String msg);

    /**
     * Callback when the Crazyflie is disconnected (called in all cases)
     *
     * @param connectionInfo
     */
    public void disconnected(String connectionInfo);

    /**
     * Called when the link driver updates the link quality measurement
     *
     * @param percent
     */
    public void linkQualityUpdated(int percent);

}
