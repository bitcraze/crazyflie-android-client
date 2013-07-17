
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
