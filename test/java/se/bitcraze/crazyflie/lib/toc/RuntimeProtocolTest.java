package se.bitcraze.crazyflie.lib.toc;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import se.bitcraze.crazyflie.lib.crazyflie.*;
import se.bitcraze.crazyflie.lib.crtp.*;
import se.bitcraze.crazyflie.lib.log.*;
import se.bitcraze.crazyflie.lib.param.Param;
import static org.junit.Assert.*;

public class RuntimeProtocolTest {
    @Test public void versionProbeRecognizesModernAndLegacyFirmware() {
        CapturingCf cf = new CapturingCf(4);
        PlatformService platform = new PlatformService(cf);
        int[] version = {-2};
        platform.fetchProtocolVersion(value -> version[0] = value);
        assertEquals((byte) 0xf1, cf.sent.poll().getHeaderByte());
        cf.emit(new CrtpPacket((byte) 0xf1, "Bitcraze Crazyflie".getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
        assertEquals((byte) 0xd1, cf.sent.poll().getHeaderByte());
        cf.emit(new CrtpPacket((byte) 0xd1, new byte[]{0, 11}));
        assertEquals(11, version[0]);
        assertTrue(cf.listeners.isEmpty());
        platform.fetchProtocolVersion(value -> version[0] = value);
        cf.emit(new CrtpPacket((byte) 0xf1, new byte[]{0}));
        assertEquals(-1, version[0]);
    }

    @Test public void legacyTocStillUsesOneByteIds() {
        CapturingCf cf = new CapturingCf(3);
        Toc toc = new Toc();
        TocFetcher fetcher = new TocFetcher(cf, CrtpPort.LOGGING, toc, null);
        fetcher.start();
        assertArrayEquals(new byte[]{1}, cf.sent.poll().getPayload());
        fetcher.newPacketReceived(new CrtpPacket((byte) 0x50, new byte[]{1, 1, 0x78, 0x56, 0x34, 0x12}));
        assertArrayEquals(new byte[]{0, 0}, cf.sent.poll().getPayload());
        fetcher.newPacketReceived(new CrtpPacket((byte) 0x50, new byte[]{0, 0, 7, 'g', 0, 'n', 0}));
        assertEquals(0x12345678, toc.getCrc());
        assertEquals(TocFetcher.TocState.TOC_FETCH_FINISHED, fetcher.getState());
    }

    @Test public void logConfigUsesV2CommandAndFullVariableId() {
        CapturingCf cf = new CapturingCf(4);
        Logg log = new Logg(cf);
        Toc toc = new Toc(); toc.addElement(element(513)); log.setToc(toc);
        LogConfig config = new LogConfig("test"); config.addVariable("g.n", VariableType.FLOAT);
        log.addConfig(config); log.create(config);
        assertArrayEquals(new byte[]{6, (byte) config.getId(), 7, 1, 2}, cf.sent.poll().getPayload());
    }

    @Test public void modernParamReadAndWriteKeepFullIdAndReadStatus() throws Exception {
        CapturingCf cf = new CapturingCf(4);
        Param param = new Param(cf);
        try {
            param.getToc().addElement(element(513));
            param.requestParamUpdate("g.n");
            CrtpPacket request = cf.sent.poll(1, TimeUnit.SECONDS);
            assertNotNull(request); assertArrayEquals(new byte[]{1, 2}, request.getPayload());
            param.paramUpdated(new CrtpPacket((byte) 0x21, new byte[]{1, 2, 0, 0, 0, (byte) 0x80, 0x3f}));
            assertEquals(1.0f, param.getValue("g.n").floatValue(), 0.0f);
            param.setValue("g.n", 2.0f);
            CrtpPacket write = cf.sent.poll(1, TimeUnit.SECONDS);
            assertNotNull(write);
            assertArrayEquals(new byte[]{1, 2, 0, 0, 0, 0x40}, write.getPayload());
            assertArrayEquals(new byte[]{1, 2}, write.getExpectedReply());
            param.paramUpdated(new CrtpPacket((byte) 0x22, write.getPayload()));
            assertEquals(2.0f, param.getValue("g.n").floatValue(), 0.0f);
            param.paramUpdated(new CrtpPacket((byte) 0x21, new byte[]{1, 2, 2}));
            assertEquals("Failed reads must not overwrite the value", 2.0f,
                    param.getValue("g.n").floatValue(), 0.0f);
        } finally { cf.driver.disconnect(); }
    }

    @Test public void legacyLogConfigStillUsesItsOriginalFormat() {
        CapturingCf cf = new CapturingCf(3);
        Logg log = new Logg(cf);
        Toc toc = new Toc(); toc.addElement(element(200)); log.setToc(toc);
        LogConfig config = new LogConfig("test"); config.addVariable("g.n", VariableType.FLOAT);
        log.addConfig(config); log.create(config);
        assertArrayEquals(new byte[]{0, (byte) config.getId(), 7, (byte) 200}, cf.sent.poll().getPayload());
    }

    @Test public void repliesMustMatchThePortChannelAndCompletePrefix() throws Exception {
        Crazyflie cf = new Crazyflie(null, null);
        java.lang.reflect.Method matcher = Crazyflie.class.getDeclaredMethod(
                "isPacketMatchingExpectedReply", CrtpPacket.class, CrtpPacket.class);
        matcher.setAccessible(true);
        CrtpPacket request = new CrtpPacket((byte) 0x50, new byte[]{2, 1, 2});
        request.setExpectedReply(new byte[]{2, 1, 2});
        assertEquals(true, matcher.invoke(cf, request, new CrtpPacket((byte) 0x50, new byte[]{2, 1, 2, 7})));
        assertEquals(false, matcher.invoke(cf, request, new CrtpPacket((byte) 0x20, new byte[]{2, 1, 2, 7})));
        assertEquals(false, matcher.invoke(cf, request, new CrtpPacket((byte) 0x51, new byte[]{2, 1, 2, 7})));
        assertEquals(false, matcher.invoke(cf, request, new CrtpPacket((byte) 0x50, new byte[]{2})));
    }

    static TocElement element(int id) {
        TocElement element = new TocElement(); element.setIdent(id); element.setGroup("g");
        element.setName("n"); element.setCtype(VariableType.FLOAT); element.setAccess(TocElement.RW_ACCESS);
        return element;
    }
    static class CapturingCf extends Crazyflie {
        final LinkedBlockingQueue<CrtpPacket> sent = new LinkedBlockingQueue<>();
        final List<DataListener> listeners = new ArrayList<>();
        final DummyDriver driver = new DummyDriver();
        final int version;
        CapturingCf(int version) { super(null, null); this.version = version; }
        @Override public int getProtocolVersion() { return version; }
        @Override public CrtpDriver getDriver() { return driver; }
        @Override public void sendPacket(CrtpPacket packet) { sent.add(packet); }
        @Override public void addDataListener(DataListener listener) { listeners.add(listener); }
        @Override public void removeDataListener(DataListener listener) { listeners.remove(listener); }
        void emit(CrtpPacket packet) {
            for (DataListener listener : new ArrayList<>(listeners)) {
                if (listener.getPort() == packet.getHeader().getPort()) { listener.dataReceived(packet); }
            }
        }
    }
    static class DummyDriver extends CrtpDriver {
        public void connect() {} public void disconnect() { notifyDisconnected(); }
        public boolean isConnected() { return true; } public void sendPacket(CrtpPacket packet) {}
        public CrtpPacket receivePacket(int wait) { return null; }
    }
}
