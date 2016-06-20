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

package se.bitcraze.crazyflie.lib.toc;


import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.bitcraze.crazyflie.lib.crazyflie.Crazyflie;
import se.bitcraze.crazyflie.lib.crazyflie.DataListener;
import se.bitcraze.crazyflie.lib.crtp.CrtpPacket;
import se.bitcraze.crazyflie.lib.crtp.CrtpPort;
import se.bitcraze.crazyflie.lib.crtp.CrtpPacket.Header;
import se.bitcraze.crazyflie.lib.log.LogTocElement;
import se.bitcraze.crazyflie.lib.param.ParamTocElement;

/**
 * Fetches TOC entries from the Crazyflie
 *
 */
public class TocFetcher {

    final Logger mLogger = LoggerFactory.getLogger("TocFetcher");

    private Crazyflie mCrazyflie;
    private CrtpPort mPort;
    private int mCrc = 0;
    private TocState mState = null;
    private TocCache mTocCache;
    private Toc mToc;

    public static final int TOC_CHANNEL = 0;
    public static final int CMD_TOC_ELEMENT = 0;
    public static final int CMD_TOC_INFO= 1;

    private int mRequestedIndex = -1;
    private int mNoOfItems = -1;

    private Set<TocFetchFinishedListener> mTocFetchFinishedListeners = new CopyOnWriteArraySet<TocFetchFinishedListener>();

    private DataListener mDataListener;
    private long tocFetchStartTime;

    public enum TocState {
        IDLE, GET_TOC_INFO, GET_TOC_ELEMENT, TOC_FETCH_FINISHED;
    }

    public TocFetcher(Crazyflie crazyFlie, CrtpPort port, Toc tocHolder, TocCache tocCache) {
        this.mCrazyflie = crazyFlie;
        this.mPort = port;
        this.mToc = tocHolder;
        this.mTocCache = tocCache;
    }

    /**
     * Initiate fetching of the TOC
     *
     */
    public void start() {
        mLogger.debug("Starting to fetch TOC (Port: " + this.mPort + ")...");

        mDataListener = new DataListener(this.mPort) {
            @Override
            public void dataReceived(CrtpPacket packet) {
                newPacketReceived(packet);
            }
        };
        this.mCrazyflie.addDataListener(mDataListener);
        tocFetchStartTime = System.currentTimeMillis();
        requestTocInfo();
    }


    /**
     * Callback for when the TOC fetching is finished
     */
    public void tocFetchFinished() {
        this.mCrazyflie.removeDataListener(mDataListener);
        long tocFetchDuration = System.currentTimeMillis() - tocFetchStartTime;
        mLogger.debug("Fetching TOC (Port: " + this.mPort + ") done in " + tocFetchDuration + "ms.");
        this.mState = TocState.TOC_FETCH_FINISHED;
        // finishedCallback();
        notifyTocFetchFinished(this.mPort);
    }

    public TocState getState() {
        return this.mState;
    }

    // TODO: only for testing, try to remove
    public void setState(TocState state) {
        this.mState = state;
    }

    public void newPacketReceived(CrtpPacket packet) {
        if (packet.getHeader().getChannel() != TOC_CHANNEL) {
            return;
        }
        // payload = struct.pack("B" * (len(packet.datal) - 1), *packet.datal[1:])

        int offset = 1;
        byte[] payload = new byte[packet.getPayload().length-offset];
        System.arraycopy(packet.getPayload(), offset, payload, 0, payload.length);
        ByteBuffer payloadBuffer = ByteBuffer.wrap(payload);

        if (mState == TocState.GET_TOC_INFO) {
            if (packet.getPayload()[0] == CMD_TOC_INFO) {
                // [self.nbr_of_items, self._crc] = struct.unpack("<BI", payload[:5])
                this.mNoOfItems = payloadBuffer.get();
                this.mCrc = payloadBuffer.getInt();
                mToc.setCrc(mCrc);

                mLogger.debug("[" + this.mPort + "]: Got TOC CRC, " + this.mNoOfItems + " items and CRC=" + String.format("0x%08X", this.mCrc));

                //Try to find toc in cache
                Toc cacheData = (mTocCache != null) ? mTocCache.fetch(mCrc, mPort) : null;
                if (cacheData != null) {
                    // self.toc.toc = cache_data
                    // assigning a toc to another toc directly does not work
                    mToc.setTocElementMap(cacheData.getTocElementMap());
                    mLogger.info("TOC for port " + mPort + " found in cache.");
                    tocFetchFinished();
                } else {
                    this.mState = TocState.GET_TOC_ELEMENT;
                    this.mRequestedIndex = 0;
                    requestTocElement(this.mRequestedIndex);
                }
            }
        } else if (mState == TocState.GET_TOC_ELEMENT) {

            if (packet.getPayload()[0] == CMD_TOC_ELEMENT) {

                // Always add new element, but only request new if it's not the last one.

                // if self.requested_index != ord(payload[0]):
                if (this.mRequestedIndex != payloadBuffer.get(0)) {
                    /*
                        # TODO: There might be a timing issue here with resending old
                        #       packets while loosing new ones. Then if 7 is requested
                        #       but 6 is send back due to timing issues with the resend
                        #       while 7 is lost then we will never resend for 7.
                        #       This is pretty hard to reproduce but happens...
                     */
                    mLogger.warn("[" + this.mPort + "]: Was expecting " + this.mRequestedIndex + " but got " + payloadBuffer.get(0));
                    return;
                }

                TocElement tocElement;
                if (mPort == CrtpPort.LOGGING) {
                    tocElement = new LogTocElement(payloadBuffer.array());
                } else {
                    tocElement = new ParamTocElement(payloadBuffer.array());
                }
                mToc.addElement(tocElement);

                mLogger.debug("Added "+ tocElement.getClass().getSimpleName() + " [" + tocElement.getIdent() + "] to TOC");

                if(mRequestedIndex < (mNoOfItems - 1)) {
                    mLogger.debug("[" + this.mPort + "]: More variables, requesting index " + (this.mRequestedIndex + 1));
                    this.mRequestedIndex++;
                    requestTocElement(this.mRequestedIndex);
                } else {
                    // No more variables in TOC
                    mLogger.info("No more variables in TOC.");
                    mTocCache.insert(mCrc, mPort, mToc);
                    tocFetchFinished();
                }
            }
        }
    }

    private void requestTocInfo() {
        //# Request the TOC CRC
        this.mState = TocState.GET_TOC_INFO;

        mLogger.debug("Requesting TOC info on port " + this.mPort);
        Header header = new Header(TOC_CHANNEL, mPort);
        CrtpPacket packet = new CrtpPacket(header.getByte(), new byte[]{CMD_TOC_INFO});
        packet.setExpectedReply(new byte[]{CMD_TOC_INFO});
        this.mCrazyflie.sendPacket(packet);
    }

    private void requestTocElement(int index) {
        mLogger.debug("Requesting index " + index + " on port " + this.mPort);
        Header header = new Header(TOC_CHANNEL, this.mPort);
        CrtpPacket packet = new CrtpPacket(header.getByte(), new byte[]{CMD_TOC_ELEMENT, (byte) index});
        packet.setExpectedReply(new byte[]{CMD_TOC_ELEMENT, (byte) index});
        this.mCrazyflie.sendPacket(packet);
    }

    /* TOC FETCH FINISHED LISTENER */

    public void addTocFetchFinishedListener(TocFetchFinishedListener listener) {
        this.mTocFetchFinishedListeners.add(listener);
    }

    // TODO: never used?
    public void removeTocFetchFinishedListener(TocFetchFinishedListener listener) {
        this.mTocFetchFinishedListeners.remove(listener);
    }

    private void notifyTocFetchFinished(CrtpPort port) {
        for (TocFetchFinishedListener tffl : this.mTocFetchFinishedListeners) {
            if (tffl.getPort() == port) {
                tffl.tocFetchFinished();
            }
        }
    }

}
