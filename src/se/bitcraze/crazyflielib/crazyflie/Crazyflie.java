package se.bitcraze.crazyflielib.crazyflie;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.bitcraze.crazyflielib.ConnectionAdapter;
import se.bitcraze.crazyflielib.ConnectionListener;
import se.bitcraze.crazyflielib.CrazyradioLink.ConnectionData;
import se.bitcraze.crazyflielib.DataListener;
import se.bitcraze.crazyflielib.Link;
import se.bitcraze.crazyflielib.crtp.CommanderPacket;
import se.bitcraze.crazyflielib.crtp.CrtpPacket;
import se.bitcraze.crazyflielib.toc.TocFetcher.TocFetchFinishedListener;

public class Crazyflie {

    final Logger mLogger = LoggerFactory.getLogger("Crazyflie");

    private Link mLink;

    private Set<DataListener> mDataListeners = new CopyOnWriteArraySet<DataListener>();
    private Set<ConnectionListener> mConnectionListeners = new CopyOnWriteArraySet<ConnectionListener>();

    private State mState = State.DISCONNECTED;

    private ConnectionData mConnectionData;

    private ConnectionListener mConnectionListener;

    /**
     * State of the connection procedure
     */
    public enum State {
        DISCONNECTED,
        INITIALIZED,
        CONNECTED,
        SETUP_FINISHED;
    }


    public Crazyflie(Link link) {
        this.mLink = link;
    }

    public void connect(int channel, int datarate) {
        connect(new ConnectionData(channel, datarate));
    }

    public void connect(ConnectionData connectionData) {
        mLogger.debug("Connect");
        mConnectionData = connectionData;
        notifyConnectionRequested();
        mState = State.INITIALIZED;

        //TODO: can this be done more elegantly?
        mConnectionListener = new ConnectionAdapter(){

            public void linkQualityUpdated(ConnectionData connectionData, int percent) {
                notifyLinkQualityUpdated(percent);
            }

        };
        mLink.addConnectionListener(mConnectionListener);

        // try to connect
        mLink.connect(mConnectionData);

        //TODO: better solution to wait for connected state?
        //Timeout: 10x50ms = 500ms
        int i = 0;
        while(i < 10) {
            if (mState == State.CONNECTED) {
                break;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            i++;
        }

        if (mState == State.CONNECTED) {
            startConnectionSetup();

        } else {
            notifyConnectionFailed("Connection failed");
            disconnect();
        }

    }

    public void disconnect() {
        if (mState != State.DISCONNECTED) {
            mLogger.debug("Disconnect");

            if (mLink != null) {
                mLink.removeConnectionListener(mConnectionListener);
                //Send commander packet with all values set to 0 before closing the connection
                sendPacket(new CommanderPacket(0, 0, 0, (char) 0));
                mLink.disconnect();
                mLink = null;
            }
            notifyDisconnected();
            mState = State.DISCONNECTED;
        }
    }

    // TODO: should this be public?
    public State getState() {
        return mState;
    }

    /**
     * Send a packet through the driver interface
     *
     * @param packet
     */
    // def send_packet(self, pk, expected_reply=(), resend=False):
    public void sendPacket(CrtpPacket packet){
        if (mLink != null) {
            mLink.sendPacket(packet);
        }
    }

    public Link getLink(){
        return mLink;
    }


    /**
     * Start the connection setup by refreshing the TOCs
     */
    public void startConnectionSetup() {
        mLogger.info("We are connected [" + mConnectionData.toString() + "], requesting connection setup...");
        //FIXME
        //Skipping log and param setup for now
        //this.mLog.refreshToc(self._log_toc_updated_cb, self._toc_cache);

        TocFetchFinishedListener paramTocFetchFinishedListener = new TocFetchFinishedListener() {

            public void tocFetchFinished() {
                //_param_toc_updated_cb(self):
                mLogger.info("Param TOC finished updating.");
                //mParam.requestUpdateOfAllParams();
                //TODO: should be set only after log, param, mems are all updated
                mState = State.SETUP_FINISHED;
                notifySetupFinished();

            }
        };
    }

    /** DATA LISTENER **/

    /**
     * Add a data listener for data that comes on a specific port
     *
     * @param dataListener
     */
    public void addDataListener(DataListener dataListener) {
        mLogger.debug("Adding data listener for port [" + dataListener.getPort() + "]");
        this.mDataListeners.add(dataListener);
    }

    /**
     * Remove a data listener for data that comes on a specific port
     *
     * @param dataListener
     */
    public void removeDataListener(DataListener dataListener) {
        mLogger.debug("Removing data listener for port [" + dataListener.getPort() + "]");
        this.mDataListeners.remove(dataListener);
    }

    //public void removeDataListener(CrtpPort); ?

    /**
     * @param inPacket
     */
    private void notifyDataReceived(CrtpPacket inPacket) {
        for (DataListener dataListener : mDataListeners) {
            dataListener.dataReceived(inPacket);
//            if (dataListener.getPort() == packet.getHeader().getPort()) {
//                dataListener.dataReceived(packet);
//            }
        }
    }

    /* CONNECTION LISTENER */

    public void addConnectionListener(ConnectionListener listener) {
        this.mConnectionListeners.add(listener);
    }

    public void removeConnectionListener(ConnectionListener listener) {
        this.mConnectionListeners.remove(listener);
    }

    /**
     * Notify all registered listeners about a requested connection
     */
    private void notifyConnectionRequested() {
        for (ConnectionListener cl : this.mConnectionListeners) {
            cl.connectionRequested(mConnectionData.toString());
        }
    }

    /**
     * Notify all registered listeners about a connect.
     */
    private void notifyConnected() {
        for (ConnectionListener cl : this.mConnectionListeners) {
            cl.connected(mConnectionData.toString());
        }
    }

    /**
     * Notify all registered listeners about a finished setup.
     */
    private void notifySetupFinished() {
        for (ConnectionListener cl : this.mConnectionListeners) {
            cl.setupFinished(mConnectionData.toString());
        }
    }

    /**
     * Notify all registered listeners about a failed connection attempt.
     * 
     * @param msg
     */
    private void notifyConnectionFailed(String msg) {
        for (ConnectionListener cl : this.mConnectionListeners) {
            cl.connectionFailed(mConnectionData.toString(), msg);
        }
    }

    /**
     * Notify all registered listeners about a lost connection.
     * 
     * @param msg
     */
    private void notifyConnectionLost(String msg) {
        for (ConnectionListener cl : this.mConnectionListeners) {
            cl.connectionLost(mConnectionData.toString(), msg);
        }
    }

    /**
     * Notify all registered listeners about a disconnect.
     */
    private void notifyDisconnected() {
        for (ConnectionListener cl : this.mConnectionListeners) {
            cl.disconnected(mConnectionData.toString());
        }
    }

    /**
     * Notify all registered listeners about a link quality update.
     * 
     * @param percent quality of the link (0 = connection lost, 100 = good)
     */
    private void notifyLinkQualityUpdated(int percent) {
        for (ConnectionListener cl : this.mConnectionListeners) {
            cl.linkQualityUpdated(percent);
        }
    }

}
