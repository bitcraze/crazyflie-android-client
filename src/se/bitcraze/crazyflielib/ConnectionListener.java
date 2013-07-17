
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
