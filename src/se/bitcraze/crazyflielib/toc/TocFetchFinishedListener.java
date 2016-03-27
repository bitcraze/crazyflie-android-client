package se.bitcraze.crazyflielib.toc;

import se.bitcraze.crazyflielib.crtp.CrtpPort;

public abstract class TocFetchFinishedListener {

    private CrtpPort mPort;

    public TocFetchFinishedListener(CrtpPort port) {
        mPort = port;
    }

    public CrtpPort getPort() {
        return mPort;
    }

    public abstract void tocFetchFinished();

}
