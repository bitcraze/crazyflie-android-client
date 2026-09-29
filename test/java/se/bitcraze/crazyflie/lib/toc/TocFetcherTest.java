package se.bitcraze.crazyflie.lib.toc;

import org.junit.Test;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import se.bitcraze.crazyflie.lib.crazyflie.Crazyflie;
import se.bitcraze.crazyflie.lib.crtp.CrtpPacket;
import se.bitcraze.crazyflie.lib.crtp.CrtpPort;
import static org.junit.Assert.*;

public class TocFetcherTest {
    @Test
    public void currentFirmwareTocFetchesIdsAbove255() {
        CapturingCrazyflie cf = new CapturingCrazyflie(4);
        Toc toc = new Toc();
        TocFetcher fetcher = new TocFetcher(cf, CrtpPort.LOGGING, toc, null);
        fetcher.start();
        assertArrayEquals(new byte[]{3}, cf.sent.get(0).getPayload());
        fetcher.newPacketReceived(packet(new byte[]{3, 2, 1, 0x78, 0x56, 0x34, 0x12, 16, 128 - 256}));
        assertEquals(258, fetcher.getNoOfItems());
        assertEquals(0x12345678, toc.getCrc());
        for (int id = 0; id < 258; id++) {
            byte[] requested = cf.sent.get(cf.sent.size() - 1).getPayload();
            assertArrayEquals(new byte[]{2, (byte) id, (byte) (id >> 8)}, requested);
            byte[] names = ("test\0value" + id + "\0").getBytes(StandardCharsets.US_ASCII);
            ByteBuffer reply = ByteBuffer.allocate(4 + names.length).order(ByteOrder.LITTLE_ENDIAN);
            reply.put((byte) 2).putShort((short) id).put((byte) 7).put(names);
            fetcher.newPacketReceived(packet(reply.array()));
        }
        assertEquals(257, toc.getElementId("test.value257"));
        assertEquals(TocFetcher.TocState.TOC_FETCH_FINISHED, fetcher.getState());
    }

    @Test
    public void emptyCurrentFirmwareTocFinishesWithoutRequestingAnItem() {
        CapturingCrazyflie cf = new CapturingCrazyflie(4);
        TocFetcher fetcher = new TocFetcher(cf, CrtpPort.PARAMETERS, new Toc(), null);
        fetcher.start();
        fetcher.newPacketReceived(new CrtpPacket((byte) 0x20, new byte[]{3, 0, 0, 1, 0, 0, 0}));
        assertEquals(TocFetcher.TocState.TOC_FETCH_FINISHED, fetcher.getState());
        assertEquals(1, cf.sent.size());
    }

    private CrtpPacket packet(byte[] payload) { return new CrtpPacket((byte) 0x50, payload); }
    static class CapturingCrazyflie extends Crazyflie {
        final List<CrtpPacket> sent = new ArrayList<>();
        private final int version;
        CapturingCrazyflie(int version) { super(null, null); this.version = version; }
        public int getProtocolVersion() { return version; }
        @Override public void sendPacket(CrtpPacket packet) { sent.add(packet); }
    }
}
