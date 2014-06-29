/**
 * 
 */

package se.bitcraze.crazyflielib;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import se.bitcraze.crazyflielib.crtp.ConsolePacket;
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
            switch (data[0] >> 4) {
                case ConsolePacket.PORT:
                    final ConsolePacket p = ConsolePacket.parse(Arrays.copyOfRange(data, 1, data.length));
                    Log.i(AbstractLink.class.getName(), "received console packet: " + p.getText());
                    break;
                    // TODO implement other types
                default:
                    Log.w(AbstractLink.class.getName(), "packet contains unknown port");
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
