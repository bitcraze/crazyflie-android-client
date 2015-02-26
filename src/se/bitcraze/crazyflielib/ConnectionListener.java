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

/**
 * Interface for receiving notifications about the connection status of a
 * {@link Link}.
 */
public interface ConnectionListener {

    /**
     * Called when a request has been made to the library to establish a
     * connection.
     *
     * @param l the link where the request has been made
     */
    public void connectionInitiated(Link l);

    /**
     * Called when the connection has been established and the log/param TOC has
     * been downloaded.
     *
     * @param l the link where the connection has been established
     */
    public void connectionSetupFinished(Link l);

    /**
     * Called when the connection has been closed (both when requested and not
     * requested to close).
     *
     * @param l the link which was disconnected
     */
    public void disconnected(Link l);

    /**
     * Called when the connection has been closed (without being requested to be
     * closed).
     *
     * @param l the link where the connection has been closed
     */
    public void connectionLost(Link l);

    /**
     * Called if the connection fails when it is being established (between the
     * request and connection setup finished).
     *
     * @param l the link where the connection has failed
     */
    public void connectionFailed(Link l);

    /**
     * Called periodically to report link status. The quantity of updates is at
     * the discretion of the link implementation.
     *
     * @param l the link which reports the link status.
     * @param quality the quality in range from 0-100. 0 means bad quality, 100
     *            is best quality.
     */
    public void linkQualityUpdate(Link l, int quality);
}
