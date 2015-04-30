package se.bitcraze.crazyflielib.toc;


import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.bitcraze.crazyflielib.DataListener;
import se.bitcraze.crazyflielib.crazyflie.Crazyflie;
import se.bitcraze.crazyflielib.crtp.CrtpPacket;
import se.bitcraze.crazyflielib.crtp.CrtpPacket.Header;
import se.bitcraze.crazyflielib.crtp.CrtpPort;
import se.bitcraze.crazyflielib.param.ParamTocElement;

/**
 * Fetches TOC entries from the Crazyflie
 *
 */
public class TocFetcher {

    final Logger mLogger = LoggerFactory.getLogger("TocFetcher");

    private Crazyflie mCrazyflie;
    private CrtpPort mPort;
    private int mCrc = 0;

    public static final int TOC_CHANNEL = 0;
    public static final int CMD_TOC_ELEMENT = 0;
    public static final int CMD_TOC_INFO= 1;

    private int mNoOfItems = -1;

    private Set<TocFetchFinishedListener> mTocFetchFinishedListeners = new CopyOnWriteArraySet<TocFetchFinishedListener>();

    private DataListener mDataListener;

    private Toc mToc;

    private int mNoOfLoops;

    public TocFetcher(Crazyflie crazyflie, CrtpPort port, Toc toc) {
        this.mCrazyflie = crazyflie;
        this.mPort = port;
        this.mToc = toc;
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

        requestTocInfo();
    }

    public void newPacketReceived(CrtpPacket packet) {
        if (packet.getHeader().getChannel() != TOC_CHANNEL) {
            return;
        }
        int offset = 1;
        byte[] payload = new byte[packet.getPayload().length-offset];
        System.arraycopy(packet.getPayload(), offset, payload, 0, payload.length);
        ByteBuffer payloadBuffer = ByteBuffer.wrap(payload);

        if (packet.getPayload()[0] == CMD_TOC_INFO) {
            this.mNoOfItems = payloadBuffer.get();
            this.mCrc = payloadBuffer.getInt();

            mLogger.debug("[" + this.mPort + "]: Got TOC CRC, " + this.mNoOfItems + " items and CRC=" + String.format("0x%08X", this.mCrc));
            requestTocElements();

        } else if (packet.getPayload()[0] == CMD_TOC_ELEMENT) {
            if (mPort == CrtpPort.LOGGING) {
                //TODO: mToc.addElement(new LogTocElement(payload));
            } else {
                ParamTocElement paramTocElement = new ParamTocElement(payloadBuffer.array());
                mToc.addElement(paramTocElement);
                mLogger.debug("ParamTocElement added to Toc: " + paramTocElement);
            }
        }

    }

    private void requestTocElements() {
        Thread requestThread = new Thread(new Runnable() {

            @Override
            public void run() {
                int timeout = 30000;
                mNoOfLoops = 0;
                Long startTime = System.currentTimeMillis();
                while((mToc.getTocSize() < mNoOfItems) && !((System.currentTimeMillis() - startTime) > timeout)) {
                    for(int i = 0; i < mNoOfItems; i++) {
                        // skip over items that have been fetched already
                        if(mToc.getTocSize() > 0 && mToc.getElementById(i) != null) {
                            continue;
                        }
                        requestTocElement(i);
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    mNoOfLoops++;
                }
                if(mToc.getTocSize() != mNoOfItems) {
                    mLogger.debug("Fetching TOC elements timed out after " + timeout  + ".");
                    return;
                }
                notifyTocFetchFinished();
                mLogger.debug("Fetched all TOC elements in " + (System.currentTimeMillis() - startTime) + "ms (noOfLoops: " + mNoOfLoops + ").");
            }

        });
        requestThread.start();
    }

    private void requestTocInfo() {
        //# Request the TOC CRC
        mLogger.debug("Requesting TOC info on port " + this.mPort);
        Header header = new Header(TOC_CHANNEL, mPort);
        CrtpPacket packet = new CrtpPacket(header.getByte(), new byte[]{CMD_TOC_INFO});
        this.mCrazyflie.sendPacket(packet);
    }

    private void requestTocElement(int index) {
        mLogger.debug("Requesting index " + index + " on port " + this.mPort);
        Header header = new Header(TOC_CHANNEL, this.mPort);
        CrtpPacket packet = new CrtpPacket(header.getByte(), new byte[]{CMD_TOC_ELEMENT, (byte) index});
        this.mCrazyflie.sendPacket(packet);
    }

    /* TOC FETCH FINISHED LISTENER */

    public void addTocFetchFinishedListener(TocFetchFinishedListener listener) {
        this.mTocFetchFinishedListeners.add(listener);
    }

    public void removeTocFetchFinishedListener(TocFetchFinishedListener listener) {
        this.mTocFetchFinishedListeners.remove(listener);
    }

    private void notifyTocFetchFinished() {
        for (TocFetchFinishedListener tffl : this.mTocFetchFinishedListeners) {
            tffl.tocFetchFinished();
        }
    }

    public interface TocFetchFinishedListener {

        public void tocFetchFinished();

    }
}
