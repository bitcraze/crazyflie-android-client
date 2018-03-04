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
     * @see se.bitcraze.crazyflielib.ConnectionListener#connectionRequested()
     */
    @Override
    public void connectionRequested() {
    }

    /* (non-Javadoc)
     * @see se.bitcraze.crazyflielib.ConnectionListener#connected()
     */
    @Override
    public void connected() {
    }

    /* (non-Javadoc)
     * @see se.bitcraze.crazyflielib.ConnectionListener#setupFinished()
     */
    @Override
    public void setupFinished() {
    }

    /* (non-Javadoc)
     * @see se.bitcraze.crazyflielib.ConnectionListener#connectionFailed(java.lang.String)
     */
    @Override
    public void connectionFailed(String msg) {
    }

    /* (non-Javadoc)
     * @see se.bitcraze.crazyflielib.ConnectionListener#connectionLost(java.lang.String)
     */
    @Override
    public void connectionLost(String msg) {
    }

    /* (non-Javadoc)
     * @see se.bitcraze.crazyflielib.ConnectionListener#disconnected()
     */
    @Override
    public void disconnected() {
    }

    /* (non-Javadoc)
     * @see se.bitcraze.crazyflielib.ConnectionListener#linkQualityUpdated(int)
     */
    @Override
    public void linkQualityUpdated(int percent) {
    }

}
