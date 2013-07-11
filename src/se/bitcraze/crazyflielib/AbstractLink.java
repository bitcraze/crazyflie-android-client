/**
 * 
 */
package se.bitcraze.crazyflielib;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * This class provides a skeletal implementation of the {@link Link} interface to minimize
 * the effort required to implement the interface. 
 */
public abstract class AbstractLink implements Link {

	private List<ConnectionListener> connectionListeners;
	private List<DataListener> dataListeners;
	
	public AbstractLink() {
		this.connectionListeners = Collections.synchronizedList(new LinkedList<ConnectionListener>());
		this.dataListeners = Collections.synchronizedList(new LinkedList<DataListener>());
	}
	
	/* (non-Javadoc)
	 * @see se.bitcraze.crazyflielib.Link#addConnectionListener(se.bitcraze.crazyflielib.ConnectionListener)
	 */
	@Override
	public void addConnectionListener(ConnectionListener l) {
		this.connectionListeners.add(l);
	}

	/* (non-Javadoc)
	 * @see se.bitcraze.crazyflielib.Link#removeConnectionListener(se.bitcraze.crazyflielib.ConnectionListener)
	 */
	@Override
	public void removeConnectionListener(ConnectionListener l) {
		this.connectionListeners.remove(l);
	}

	/* (non-Javadoc)
	 * @see se.bitcraze.crazyflielib.Link#addDataListener(se.bitcraze.crazyflielib.DataListener)
	 */
	@Override
	public void addDataListener(DataListener l) {
		this.dataListeners.add(l);
	}

	/* (non-Javadoc)
	 * @see se.bitcraze.crazyflielib.Link#removeDataListener(se.bitcraze.crazyflielib.DataListener)
	 */
	@Override
	public void removeDataListener(DataListener l) {
		this.dataListeners.remove(l);
	}
	
	/**
	 * Notify all registered listeners about an initiated connection.
	 */
	protected void notifyConnectionInitiated() {
		synchronized (this.connectionListeners) {
			for(ConnectionListener cl : this.connectionListeners) {
				cl.connectionInitiated(this);
			}
		}
	}
	
	/**
	 * Notify all registered listeners about a setup connection.
	 */
	protected void notifyConnectionSetupFinished() {
		synchronized (this.connectionListeners) {
			for(ConnectionListener cl : this.connectionListeners) {
				cl.connectionSetupFinished(this);
			}
		}
	}
	
	/**
	 * Notify all registered listeners about a disconnect.
	 */
	protected void notifyDisconnected() {
		synchronized (this.connectionListeners) {
			for(ConnectionListener cl : this.connectionListeners) {
				cl.disconnected(this);
			}
		}
	}
	
	/**
	 * Notify all registered listeners about a lost connection.
	 */
	protected void notifyConnectionLost() {
		synchronized (this.connectionListeners) {
			for(ConnectionListener cl : this.connectionListeners) {
				cl.connectionLost(this);
			}
		}
	}

	/**
	 * Notify all registered listeners about a failed connection attempt.
	 */
	protected void notifyConnectionFailed() {
		synchronized (this.connectionListeners) {
			for(ConnectionListener cl : this.connectionListeners) {
				cl.connectionFailed(this);
			}
		}
	}
}
