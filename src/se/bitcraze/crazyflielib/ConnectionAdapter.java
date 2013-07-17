/**
 * 
 */

package se.bitcraze.crazyflielib;

/**
 * An abstract adapter class for receiving connection events. The methods in
 * this class are empty. This class exists as convenience for creating listener
 * objects.
 */
public abstract class ConnectionAdapter implements ConnectionListener {

    /*
     * (non-Javadoc)
     * @see
     * se.bitcraze.crazyflielib.ConnectionListener#connectionInitiated(se.bitcraze
     * .crazyflielib.Link)
     */
    @Override
    public void connectionInitiated(Link l) {
    }

    /*
     * (non-Javadoc)
     * @see
     * se.bitcraze.crazyflielib.ConnectionListener#connectionSetupFinished(se
     * .bitcraze.crazyflielib.Link)
     */
    @Override
    public void connectionSetupFinished(Link l) {
    }

    /*
     * (non-Javadoc)
     * @see
     * se.bitcraze.crazyflielib.ConnectionListener#disconnected(se.bitcraze.
     * crazyflielib.Link)
     */
    @Override
    public void disconnected(Link l) {
    }

    /*
     * (non-Javadoc)
     * @see
     * se.bitcraze.crazyflielib.ConnectionListener#connectionLost(se.bitcraze
     * .crazyflielib.Link)
     */
    @Override
    public void connectionLost(Link l) {
    }

    /*
     * (non-Javadoc)
     * @see
     * se.bitcraze.crazyflielib.ConnectionListener#connectionFailed(se.bitcraze
     * .crazyflielib.Link)
     */
    @Override
    public void connectionFailed(Link l) {
    }

    @Override
    public void linkQualityUpdate(Link l, int quality) {
    }

}
