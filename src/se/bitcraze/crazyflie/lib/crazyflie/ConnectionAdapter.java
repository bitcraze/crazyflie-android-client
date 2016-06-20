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
 * An abstract adapter class for receiving connection events. The methods in
 * this class are empty. This class exists as convenience for creating listener objects.
 */
public abstract class ConnectionAdapter implements ConnectionListener {

    /* (non-Javadoc)
     * @see se.bitcraze.crazyflielib.ConnectionListener#connectionRequested(java.lang.String)
     */
    @Override
    public void connectionRequested(String connectionInfo) {
    }

    /* (non-Javadoc)
     * @see se.bitcraze.crazyflielib.ConnectionListener#connected(java.lang.String)
     */
    @Override
    public void connected(String connectionInfo) {
    }

    /* (non-Javadoc)
     * @see se.bitcraze.crazyflielib.ConnectionListener#setupFinished(java.lang.String)
     */
    @Override
    public void setupFinished(String connectionInfo) {
    }

    /* (non-Javadoc)
     * @see se.bitcraze.crazyflielib.ConnectionListener#connectionFailed(java.lang.String, java.lang.String)
     */
    @Override
    public void connectionFailed(String connectionInfo, String msg) {
    }

    /* (non-Javadoc)
     * @see se.bitcraze.crazyflielib.ConnectionListener#connectionLost(java.lang.String, java.lang.String)
     */
    @Override
    public void connectionLost(String connectionInfo, String msg) {
    }

    /* (non-Javadoc)
     * @see se.bitcraze.crazyflielib.ConnectionListener#disconnected(java.lang.String)
     */
    @Override
    public void disconnected(String connectionInfo) {
    }

    /* (non-Javadoc)
     * @see se.bitcraze.crazyflielib.ConnectionListener#linkQualityUpdated(int)
     */
    @Override
    public void linkQualityUpdated(int percent) {
    }

}
