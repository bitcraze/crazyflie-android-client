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

package se.bitcraze.crazyflie.lib.param;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.bitcraze.crazyflie.lib.crazyflie.ConnectionAdapter;
import se.bitcraze.crazyflie.lib.crazyflie.Crazyflie;
import se.bitcraze.crazyflie.lib.crazyflie.DataListener;
import se.bitcraze.crazyflie.lib.crtp.CrtpPacket;
import se.bitcraze.crazyflie.lib.crtp.CrtpPort;
import se.bitcraze.crazyflie.lib.crtp.CrtpPacket.Header;
import se.bitcraze.crazyflie.lib.toc.Toc;
import se.bitcraze.crazyflie.lib.toc.TocCache;
import se.bitcraze.crazyflie.lib.toc.TocElement;
import se.bitcraze.crazyflie.lib.toc.TocFetchFinishedListener;
import se.bitcraze.crazyflie.lib.toc.TocFetcher;

/**
 * Enables reading/writing of parameter values to/from the Crazyflie.
 * When a Crazyflie is connected it's possible to download a TableOfContent of all
 * the parameters that can be written/read.
 *
 */
public class Param {

    final static Logger mLogger = LoggerFactory.getLogger("Param");

    private Toc mToc;
    private Crazyflie mCrazyflie;

    private Thread mParamUpdaterThread;
    private ParamUpdaterThread mPut;
    private Map<String, Map<String, Number>> mValues = new HashMap<String, Map<String, Number>>();
    private boolean mHaveUpdated = false;

    // TODO: use only one map for both
    // TODO: ParamListener already contains group/completeName
    private Map<String, ParamListener> mUpdateListeners = new HashMap<String, ParamListener>();         // completeName
    private Map<String, ParamListener> mGroupUpdateListeners = new HashMap<String, ParamListener>();    // group

    // Possible states
    private int IDLE = 0;
    private int WAIT_TOC = 1;
    private int WAIT_READ = 2;
    private int WAIT_WRITE = 3;

    private int TOC_CHANNEL = 0;
    private int READ_CHANNEL = 1;
    private int WRITE_CHANNEL = 2;

    // TOC access command
    private int TOC_RESET = 0;
    private int TOC_GETNEXT = 1;
    private int TOC_GETCRC32 = 2;


    public Param(Crazyflie crazyflie) {
        this.mCrazyflie = crazyflie;

        this.mToc = new Toc(); // Avoid NPE

        // self.param_updater = None
        // self.param_updater = _ParamUpdater(self.cf, self._param_updated)
        // self.param_updater.start()
        mParamUpdaterThread = null;
        if (mParamUpdaterThread == null) {
            mPut = new ParamUpdaterThread();
            mParamUpdaterThread = new Thread(mPut);
            mParamUpdaterThread.start();
        }

        // self.cf.disconnected.add_callback(self.param_updater.close)
        mCrazyflie.getDriver().addConnectionListener(new ConnectionAdapter() {
            @Override
            public void disconnected(String connectionInfo) {
                mPut.close();
                if (mParamUpdaterThread != null) {
                    mParamUpdaterThread.interrupt();
                }
            }
        });

        // self.all_updated = Caller()
    }

    /**
     * Request an update of all the parameters in the TOC
     */
    public void requestUpdateOfAllParams() {
        for (TocElement tocElement : mToc.getElements()) {
            requestParamUpdate(tocElement.getCompleteName());
        }
    }

    /**
     * Check if all parameters from the TOC have at least been fetched once
     */
    public boolean checkIfAllUpdated() {
        /*
        for g in self.toc.toc:
            if not g in self.values:
                return False
            for n in self.toc.toc[g]:
                if not n in self.values[g]:
                    return False

        return True
        */
        for (TocElement tocElement : mToc.getElements()) {
            if (mValues.get(tocElement.getGroup()) == null) {
                return false;
            } else {
                if (mValues.get(tocElement.getGroup()).get(tocElement.getName()) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Callback with data for an updated parameter
     */
    public void paramUpdated(CrtpPacket packet) {
        int varId = packet.getPayload()[0];
        TocElement tocElement = mToc.getElementById(varId);
        if (tocElement != null) {
            //s = struct.unpack(element.pytype, pk.data[1:])[0]
            //s = s.__str__()
            // TODO: probably does not work as intended, use System.arrayCopy instead
            ByteBuffer payload = ByteBuffer.wrap(packet.getPayload(), 1, packet.getPayload().length-1);
            Number number = tocElement.getCtype().parse(payload);

            String completeName = tocElement.getCompleteName();

            // Save the value for synchronous access
            if (!mValues.containsKey(tocElement.getGroup())) {
                mValues.put(tocElement.getGroup(), new HashMap<String, Number>());
            }
            mValues.get(tocElement.getGroup()).put(tocElement.getName(), number);

            // This will only be called once
            if (checkIfAllUpdated() && !mHaveUpdated) {
                mHaveUpdated = true;
                // self.all_updated.call()
            }
//                mLogger.debug("Updated parameter " + completeName);

            if (mUpdateListeners.containsKey(completeName)) {
                mUpdateListeners.get(completeName).updated(completeName, number);
            }
            if (mGroupUpdateListeners.containsKey(tocElement.getGroup())) {
                mGroupUpdateListeners.get(tocElement.getGroup()).updated(completeName, number);
            }
        } else {
            mLogger.debug("Variable id " + varId + " not found in TOC");
        }
    }

    public Map<String, Map<String, Number>> getValuesMap() {
        return mValues;
    }

    /**
     * Remove the listener for a group or a complete name (group.name)
     */
    public void removeParamListeners(String group, String name) {
        if (name == null || name.isEmpty()) {
            if (mGroupUpdateListeners.containsKey(group)) {
                mGroupUpdateListeners.remove(group);
            }
        } else {
            String completeName = group + "." + name;
            if (mUpdateListeners.containsKey(completeName)) {
                mUpdateListeners.remove(completeName);
            }
        }
    }

    /**
     * Add a listener for a specific parameter name. This callback will be
     * executed when a new value is read from the Crazyflie.
     */
    public void addParamListener(ParamListener paramListener) {
        if (paramListener.getName() == null || paramListener.getName().isEmpty()) {
            if (!mGroupUpdateListeners.containsKey(paramListener.getGroup())) {
                mGroupUpdateListeners.put(paramListener.getGroup(), paramListener);
            }
        } else {
            if (!mUpdateListeners.containsKey(paramListener.getCompleteName())) {
                mUpdateListeners.put(paramListener.getCompleteName(), paramListener);
            }
        }
    }

    /**
     * Initiate a refresh of the parameter TOC.
     */
    // def refresh_toc(self, refresh_done_callback, toc_cache):
    public void refreshToc(TocFetchFinishedListener listener, TocCache tocCache) {
       this.mToc = new Toc();
       // toc_fetcher = TocFetcher(self.cf, ParamTocElement, CRTPPort.PARAM, self.toc, refresh_done_callback, toc_cache)
       TocFetcher tocFetcher = new TocFetcher(mCrazyflie, CrtpPort.PARAMETERS, mToc, tocCache);
       tocFetcher.addTocFetchFinishedListener(listener);
       tocFetcher.start();
    }

    //TODO: only for debugging
    public Toc getToc() {
        return this.mToc;
    }

    /*
    def disconnected(self, uri):
        """Disconnected callback from Crazyflie API"""
        self.param_updater.close()
        self._have_updated = False

     */

    /**
     * Request an update of the value for the supplied parameter.
     *
     * @param completeName
     */
    //TODO: public?
    public void requestParamUpdate(String completeName) {
        // self.param_updater.request_param_update(self.toc.get_element_id(complete_name))
        int elementId = mToc.getElementId(completeName);
        Header header = new Header(READ_CHANNEL, CrtpPort.PARAMETERS);
        CrtpPacket requestPacket = new CrtpPacket(header.getByte(), new byte[]{(byte) elementId});
        mPut.addParamRequest(requestPacket);
    }

    /**
     * Set the value for the supplied parameter.
     *
     * @param completeName
     * @param value
     */
    //TODO: is Number the right data type for value?
    public void setValue(String completeName, Number value) {
        TocElement tocElement = mToc.getElementByCompleteName(completeName);
        if (tocElement == null) {
            mLogger.warn("Cannot set value for " + completeName + ", it's not in the TOC!");
        } else if (tocElement.getAccess() == TocElement.RO_ACCESS) {
            mLogger.debug(completeName + " is read only, not trying to set value");
        } else {
            //TODO: extract into method
            Header header = new Header(WRITE_CHANNEL, CrtpPort.PARAMETERS);
            //pk.data = struct.pack('<B', varid)
            //pk.data += struct.pack(element.pytype, eval(value))
            //TODO: value.byteValue() might not be the right method to use, because it can involve rounding or truncation!
            byte[] parse = tocElement.getCtype().parse(value);
            ByteBuffer bb = ByteBuffer.allocate(parse.length+1);
            bb.put((byte) tocElement.getIdent());
            bb.put(parse);
            CrtpPacket packet = new CrtpPacket(header.getByte(), bb.array());
            //self.param_updater.request_param_setvalue(pk)
            mPut.addParamRequest(packet);
        }
    }

    /**
     * Get the value for the supplied parameter
     *
     * @param completeName
     * @return
     */
    public Number getValue(String completeName) {
        TocElement tocElement = mToc.getElementByCompleteName(completeName);
        if (tocElement == null) {
            mLogger.warn("Cannot get value for " + completeName + ", it's not in the TOC!");
            return -1;
        }
        if (getValuesMap().size() > 0) {
            return getValuesMap().get(tocElement.getGroup()).get(tocElement.getName());
        } else {
            mLogger.warn("Parameters values map is empty!");
            return -2;
        }
    }

    /**
     * This thread will update params through a queue to make sure that we get back values
     *
     */
    public class ParamUpdaterThread implements Runnable {

        final Logger mLogger = LoggerFactory.getLogger(this.getClass().getSimpleName());

        private final BlockingQueue<CrtpPacket> mRequestQueue = new LinkedBlockingQueue<CrtpPacket>();
        private int mReqParam = -1;

        /**
         * Initialize the thread
         */
        public ParamUpdaterThread() {
            /*
            Thread.__init__(self)
            self.setDaemon(True)
            self.wait_lock = Lock()
            self.updated_callback = updated_callback
            */

            // self.cf.add_port_callback(CRTPPort.PARAM, self._new_packet_cb)
            mCrazyflie.addDataListener(new DataListener(CrtpPort.PARAMETERS) {
                @Override
                public void dataReceived(CrtpPacket packet) {
                    newPacketReceived(packet);
                }
            });
        }

        // def close(self, uri):
        public void close () {
            /*
            # First empty the queue from all packets
            while not self.request_queue.empty():
                self.request_queue.get()
            # Then force an unlock of the mutex if we are waiting for a packet
            # we didn't get back due to a disconnect for example.
            try:
                self.wait_lock.release()
            except:
                pass
             */
            mRequestQueue.clear();
        }

        /**
         * Place a param request (update request or set value) on the queue.
         *
         * @param packet
         */
        public void addParamRequest(CrtpPacket packet) {
            try {
                //TODO: is put() the right method?
                mRequestQueue.put(packet);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        /**
         * Callback for newly arrived packets
         *
         * @param packet
         */
        private void newPacketReceived(CrtpPacket packet) {
            int channel = packet.getHeader().getChannel();
            if (channel == READ_CHANNEL || channel == WRITE_CHANNEL) {
                int varId = packet.getPayload()[0];
                //if (pk.channel != TOC_CHANNEL and self._req_param == var_id and pk is not None):

                // TODO: is this even a problem!?
//                if (channel != TOC_CHANNEL && mReqParam != varId) {
//                    mLogger.warn("mReqParam != varId: " + mReqParam + " : " + varId);
//                }

                /* if mReqParam == varId is actually checked, resend or late packets are dismissed,
                 * which makes the param update fragile
                 */
                if (channel != TOC_CHANNEL /* && mReqParam == varId */) {
                    //self.updated_callback(pk)
                    paramUpdated(packet);
                    //self._req_param = -1
                    mReqParam = -1;
                    /*
                    try:
                        self.wait_lock.release()
                    except:
                        pass
                    */
                }
            }
        }

        public void run() {
            CrtpPacket packet = null;
            while (mCrazyflie.getDriver() != null && !Thread.currentThread().isInterrupted()) {
                try {
                    packet = null;
                    // pk = self.request_queue.get() # Wait for request update
                    packet = mRequestQueue.poll((long) 100, TimeUnit.MILLISECONDS);

                    // self.wait_lock.acquire()
                    // if self.cf.link:
                    if (packet != null && packet.getPayload().length > 0) {
                        mReqParam = packet.getPayload()[0];
                        // self.cf.send_packet(pk, expected_reply=(pk.datat[0:2]))
                        packet.setExpectedReply(new byte[] {packet.getPayload()[0]});
                        if (packet.getHeader().getChannel() == READ_CHANNEL) {
                            mLogger.debug("Requesting updated for param with ID " + mReqParam);
                        } else {
                            mLogger.debug("Setting param with ID " + mReqParam);
                        }
                        mCrazyflie.sendPacket(packet);
                    } else {
                        // self.wait_lock.release()
                    }
                } catch (InterruptedException e) {
                    mLogger.debug("ParamUpdaterThread was interrupted.");
                    break;
                }
            }
        }
    }

}
