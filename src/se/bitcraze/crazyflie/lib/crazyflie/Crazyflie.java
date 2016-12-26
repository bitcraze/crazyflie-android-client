/**
 *    ||          ____  _ __
 * +------+      / __ )(_) /_______________ _____  ___
 * | 0xBC |     / __  / / __/ ___/ ___/ __ `/_  / / _ \
 * +------+    / /_/ / / /_/ /__/ /  / /_/ / / /_/  __/
 *  ||  ||    /_____/_/\__/\___/_/   \__,_/ /___/\___/
 *
 * Copyright (C) 2015 Bitcraze AB
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

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.bitcraze.crazyflie.lib.BleLink;
import se.bitcraze.crazyflie.lib.crazyradio.ConnectionData;
import se.bitcraze.crazyflie.lib.crazyradio.RadioDriver;
import se.bitcraze.crazyflie.lib.crtp.CommanderPacket;
import se.bitcraze.crazyflie.lib.crtp.CrtpDriver;
import se.bitcraze.crazyflie.lib.crtp.CrtpPacket;
import se.bitcraze.crazyflie.lib.crtp.CrtpPort;
import se.bitcraze.crazyflie.lib.log.Logg;
import se.bitcraze.crazyflie.lib.param.Param;
import se.bitcraze.crazyflie.lib.toc.TocCache;
import se.bitcraze.crazyflie.lib.toc.TocFetchFinishedListener;

public class Crazyflie {

    final Logger mLogger = LoggerFactory.getLogger("Crazyflie");

    private CrtpDriver mDriver;
    private Thread mIncomingPacketHandlerThread;

    private LinkedBlockingDeque<CrtpPacket> mResendQueue = new LinkedBlockingDeque<CrtpPacket>();
    private Thread mResendQueueHandlerThread;

    private Set<DataListener> mDataListeners = new CopyOnWriteArraySet<DataListener>();
    private Set<PacketListener> mPacketListeners = new CopyOnWriteArraySet<PacketListener>();

    private State mState = State.DISCONNECTED;

    private ConnectionData mConnectionData;

    private Param mParam;
    private Logg mLogg;
    private TocCache mTocCache;

    /**
     * State of the connection procedure
     */
    public enum State {
        DISCONNECTED,
        INITIALIZED,
        CONNECTED,
        SETUP_FINISHED;
    }

    /**
     * Crazyflie constructor
     *
     * @param driver driver to use (e.g. RadioDriver or BleLink)
     */
    public Crazyflie(CrtpDriver driver) {
        this(driver, null);
    }

    /**
     * Crazyflie constructor
     *
     * @param driver driver to use (e.g. RadioDriver or BleLink)
     * @param tocCacheDir TOC cache files dir
     */
    public Crazyflie(CrtpDriver driver, File tocCacheDir) {
        this.mDriver = driver;
        this.mTocCache = new TocCache(tocCacheDir);
    }

    private PacketListener mPacketListener = new PacketListener() {

        public void packetReceived(CrtpPacket packet) {
            checkReceivedPackets(packet);
        }

        public void packetSent() {
        }

    };

    public void connect(int channel, int datarate) {
        connect(new ConnectionData(channel, datarate));
    }

    public void connect(ConnectionData connectionData) {
        mLogger.debug("Connect");
        mConnectionData = connectionData;
        mState = State.INITIALIZED;

        addPacketListener(mPacketListener);

        // try to connect
        try {
            mDriver.connect(mConnectionData);
        } catch (IOException ioe) {
            mLogger.debug(ioe.getMessage());
//            notifyConnectionFailed("Connection failed: " + ioe.getMessage());
            disconnect();
        } catch (IllegalArgumentException iae) {
            mLogger.debug(iae.getMessage());
//            notifyConnectionFailed("Connection failed: " + iae.getMessage());
            disconnect();
        }

        if (mIncomingPacketHandlerThread == null) {
            IncomingPacketHandler iph = new IncomingPacketHandler();
            mIncomingPacketHandlerThread = new Thread(iph);
            mIncomingPacketHandlerThread.start();
        }

        if (mResendQueueHandlerThread == null) {
            ResendQueueHandler rqh = new ResendQueueHandler();
            mResendQueueHandlerThread = new Thread(rqh);
            mResendQueueHandlerThread.start();
        }

    }

    public void disconnect() {
        if (mState != State.DISCONNECTED) {
            mLogger.debug("disconnect()");

            if (mDriver.isConnected()) {
                //Send commander packet with all values set to 0 before closing the connection
                sendPacket(new CommanderPacket(0, 0, 0, (char) 0));
                mDriver.disconnect();
            } else {
                mDriver.disconnect();
            }
            if(mIncomingPacketHandlerThread != null) {
                mIncomingPacketHandlerThread.interrupt();
            }
            if(mResendQueueHandlerThread != null) {
                mResendQueueHandlerThread.interrupt();
            }
            removePacketListener(mPacketListener);
            mState = State.DISCONNECTED;
        }
    }

    //TODO: is this good enough?
    public boolean isConnected() {
        // workaround for BleLink because it does not support Param and Logg subsystems yet
        // TODO: this should be fixed somewhere else, because it adds a dependency to BLeLink
        if (mDriver instanceof BleLink) {
            return mState == State.CONNECTED;
        }
        return mState == State.SETUP_FINISHED;
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
        if (mDriver.isConnected()) {
            mDriver.sendPacket(packet);

            if (packet.getExpectedReply() != null && packet.getExpectedReply().length > 0) {
                //add packet to resend queue
                if(!mResendQueue.contains(packet)) {
                    mResendQueue.add(packet);
                } else {
                    mLogger.warn("Packet already exists in Queue.");
                }
            }
        }
    }

    /**
     * Callback called for every packet received to check if we are
     * waiting for an packet like this. If so, then remove it from the queue.
     *
     * @param packet
     */
    private void checkReceivedPackets(CrtpPacket packet) {
        // compare received packet with expectedReplies in resend queue
        for(CrtpPacket resendQueuePacket : mResendQueue) {
            if(isPacketMatchingExpectedReply(resendQueuePacket, packet)) {
                mResendQueue.remove(resendQueuePacket);
                // mLogger.debug("QUEUE REMOVE: " + resendQueuePacket);
                break;
            }
        }
    }

    private boolean isPacketMatchingExpectedReply(CrtpPacket resendQueuePacket, CrtpPacket packet) {
        //Only check equality for the amount of bytes in expected reply
        byte[] expectedReply = resendQueuePacket.getExpectedReply();
        for(int i = 0; i < expectedReply.length;i++) {
            if(expectedReply[i] != packet.getPayload()[i]) {
                return false;
            }
        }
        return true;
    }

    private class ResendQueueHandler implements Runnable {

        public void run() {
            while(true) {
                if (!mResendQueue.isEmpty()) {
                    CrtpPacket resendPacket = mResendQueue.poll();
                    mLogger.debug("RESEND: " + resendPacket + " ID: " + resendPacket.getPayload()[0]);
                    sendPacket(resendPacket);
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    mLogger.debug("ResendQueueHandlerThread was interrupted.");
                    break;
                }
            }
        }

    }


    /**
     * Called when first packet arrives from Crazyflie.
     * This is used to determine if we are connected to something that is answering.
     *
     * @param packet
     */
    public void checkForInitialPacketCallback(CrtpPacket packet) {
        //TODO: should be made more reliable
        if (this.mState == State.INITIALIZED) {
            mLogger.info("Initial packet has been received! => State.CONNECTED");
            this.mState = State.CONNECTED;
            //self.link_established.call(self.link_uri)
            //TODO: fix hacky-di-hack
            if (this.mDriver instanceof RadioDriver) {
                this.mDriver.notifyConnected();
                startConnectionSetup();
            }
        }
        //self.packet_received.remove_callback(self._check_for_initial_packet_cb)
        // => IncomingPacketHandler
    }

    public CrtpDriver getDriver(){
        return mDriver;
    }


    /**
     * Start the connection setup by refreshing the TOCs
     */
    private void startConnectionSetup() {
        mLogger.info("We are connected [" + mConnectionData.toString() + "], requesting connection setup...");

        mParam = new Param(this);
        //must be defined first to be usable in Log TocFetchFinishedListener
        final TocFetchFinishedListener paramTocFetchFinishedListener = new TocFetchFinishedListener(CrtpPort.PARAMETERS) {
            public void tocFetchFinished() {
                //_param_toc_updated_cb(self):
                mLogger.info("Param TOC finished updating.");
                //mParam.requestUpdateOfAllParams();
                //TODO: should be set only after log, param, mems are all updated
                mState = State.SETUP_FINISHED;
                //TODO: fix hacky-di-hack
                mDriver.notifySetupFinished();
            }
        };

        mLogg = new Logg(this);
        TocFetchFinishedListener loggTocFetchFinishedListener = new TocFetchFinishedListener(CrtpPort.LOGGING) {
            public void tocFetchFinished() {
                mLogger.info("Logg TOC finished updating.");
                //after log toc has been fetched, fetch param toc
                mParam.refreshToc(paramTocFetchFinishedListener, mTocCache);
            }
        };
        //mLog.refreshToc(self._log_toc_updated_cb, self._toc_cache);
        if (mDriver instanceof RadioDriver) {
            mLogg.refreshToc(loggTocFetchFinishedListener, mTocCache);
        } else {
            //TODO: shortcut for BLELink
            mState = State.SETUP_FINISHED; //important, otherwise BLE keeps trying to reconnect
            mDriver.notifySetupFinished();
        }

        //TODO: self.mem.refresh(self._mems_updated_cb)
    }

    public Param getParam() {
        return mParam;
    }

    public Logg getLogg() {
        return mLogg;
    }

    public void clearTocCache() {
        mTocCache.clear();
    }

    public void setParamValue(String completeName, Number value) {
        if (mParam != null) {
            mParam.setValue(completeName, value);
        }
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
     * @param packet
     */
    private void notifyDataReceived(CrtpPacket packet) {
        boolean found = false;
        for (DataListener dataListener : mDataListeners) {
            if (dataListener.getPort() == packet.getHeader().getPort()) {
                dataListener.dataReceived(packet);
                found = true;
            }
        }
        if (!found) {
            //mLogger.warn("Got packet on port [" + packet.getHeader().getPort() + "] but found no data listener to handle it.");
        }
    }

    /* PACKET LISTENER */

    //TODO: should PacketListener methods be public?

    public void addPacketListener(PacketListener listener) {
        mLogger.debug("Adding packet listener...");
        this.mPacketListeners.add(listener);
    }

    public void removePacketListener(PacketListener listener) {
        mLogger.debug("Removing packet listener...");
        this.mPacketListeners.remove(listener);
    }

    private void notifyPacketReceived(CrtpPacket inPacket) {
        checkForInitialPacketCallback(inPacket);
        for (PacketListener pl : this.mPacketListeners) {
            pl.packetReceived(inPacket);
        }
    }

    /**
     * Handles incoming packets and sends the data to the correct listeners
     */
    public class IncomingPacketHandler implements Runnable {

        final Logger mLogger = LoggerFactory.getLogger("IncomingPacketHandler");

        public void run() {
            while(!Thread.currentThread().isInterrupted()) {
                CrtpPacket packet = getDriver().receivePacket(1);
                if(packet == null) {
                    continue;
                }

                //All-packet callbacks
                //self.cf.packet_received.call(pk)
                notifyPacketReceived(packet);

                notifyDataReceived(packet);
            }
            mLogger.debug("IncomingPacketHandlerThread was interrupted.");
        }

    }

}
