package se.bitcraze.crazyflie.lib.bootloader;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import se.bitcraze.crazyflie.lib.crtp.CrtpDriver;
import se.bitcraze.crazyflie.lib.crtp.CrtpPacket;

import static org.junit.Assert.*;

public class BootloaderTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private RecordingDriver driver;
    private Bootloader bootloader;

    @Before
    public void setUp() {
        driver = new RecordingDriver();
        bootloader = new Bootloader(driver);
        bootloader.getCloader().getTargets().put(Target.TargetTypes.STM32,
                target(Target.TargetTypes.STM32, 16, 1024));
        setNrfStartPage(108);
    }

    @Test
    public void modernManifestSelectsFirmwareAndSkipsDecksAndBootloader() throws Exception {
        List<Bootloader.FlashTarget> plan = bootloader.getFlashTargets(archive(modernManifest()));
        assertEquals(2, plan.size());
        for (Bootloader.FlashTarget image : plan) {
            assertEquals("fw", image.getType());
        }
    }

    @Test
    public void matchingSoftDeviceFlashesBothProcessorsAtTheirFirmwarePages() throws Exception {
        assertTrue(bootloader.flash(archive(modernManifest())));
        assertEquals(2, driver.writes.size());
        for (byte[] write : driver.writes) {
            int id = write[0] & 0xff;
            int page = ByteBuffer.wrap(write).order(ByteOrder.LITTLE_ENDIAN).getShort(4);
            assertEquals(id == Target.TargetTypes.NRF51 ? 108 : 16, page);
        }
    }

    @Test
    public void mismatchRejectsWholeUpdateBeforeAnyPacketIsSent() throws Exception {
        setNrfStartPage(88);
        assertRejected(modernManifest(), "sd-s110", "sd-s130");
    }

    @Test
    public void unknownInstalledSoftDeviceStopsBeforeAnyPacketIsSent() throws Exception {
        setNrfStartPage(100);
        assertRejected(modernManifest(), "unknown", "100");
    }

    @Test
    public void missingNrfInformationStopsBeforeAnyPacketIsSent() throws Exception {
        bootloader.getCloader().getTargets().remove(Target.TargetTypes.NRF51);
        assertRejected(modernManifest(), "nrf51");
    }

    @Test
    public void legacyFirmwareRequiresS110EvenWithoutRequiresMetadata() throws Exception {
        assertRejected(legacyManifest(), "sd-s130", "sd-s110");
    }

    @Test
    public void matchingLegacyFirmwareFlashesBothProcessors() throws Exception {
        setNrfStartPage(88);
        assertTrue(bootloader.flash(archive(legacyManifest())));
        assertEquals(2, driver.writes.size());
    }

    @Test
    public void explicitStm32OnlyUpdateDoesNotRequireMatchingNrfFirmware() throws Exception {
        setNrfStartPage(88);
        assertTrue(bootloader.flash(archive(modernManifest()), "stm32"));
        assertEquals(1, driver.writes.size());
        assertEquals(Target.TargetTypes.STM32, driver.writes.get(0)[0] & 0xff);
    }

    @Test
    public void explicitNrfUpdateDoesNotFlashBundledBootloader() throws Exception {
        assertTrue(bootloader.flash(archive(modernManifest()), "nrf51"));
        assertEquals(1, driver.writes.size());
    }

    @Test
    public void conflictingRequirementsStopBeforeAnyPacketIsSent() throws Exception {
        assertRejected(modernManifest().replace("[\"sd-s130\"]", "[\"sd-s110\", \"sd-s130\"]"),
                "sd-s110", "sd-s130");
    }

    @Test
    public void invalidManifestReportsParsingErrorWithoutSendingPackets() throws Exception {
        assertRejected("{invalid json}", "manifest");
    }

    @Test(timeout = 8000)
    public void missingFlashReplyReturnsFailureInsteadOfHanging() {
        driver.dropReply = true;
        assertFalse(bootloader.getCloader().writeFlash(Target.TargetTypes.NRF51, 0, 135, 1));
        assertTrue(bootloader.getCloader().getErrorMessage().contains("Timed out"));
        assertEquals("Do not repeat a flash command when its result is unknown", 1, driver.writes.size());
    }

    @Test
    public void cancelledFlashDoesNotDereferenceMissingReply() {
        bootloader.getCloader().cancel();
        assertFalse(bootloader.getCloader().writeFlash(Target.TargetTypes.NRF51, 0, 135, 1));
        assertTrue(driver.packets.isEmpty());
    }

    private void assertRejected(String manifest, String... messages) throws Exception {
        try {
            bootloader.flash(archive(manifest));
            fail("Expected update to be rejected");
        } catch (IOException error) {
            for (String message : messages) {
                assertTrue(error.getMessage(), error.getMessage().contains(message));
            }
        }
        assertTrue("Refused updates must not send any packets", driver.packets.isEmpty());
    }

    private void setNrfStartPage(int page) {
        bootloader.getCloader().getTargets().put(Target.TargetTypes.NRF51,
                target(Target.TargetTypes.NRF51, page, 256));
    }

    private Target target(int id, int startPage, int flashPages) {
        byte[] info = ByteBuffer.allocate(23).order(ByteOrder.LITTLE_ENDIAN)
                .put((byte) id).put((byte) Cloader.GET_INFO)
                .putShort((short) 1024).putShort((short) 1)
                .putShort((short) flashPages).putShort((short) startPage)
                .put(new byte[12]).put((byte) BootVersion.CF2_PROTO_VER).array();
        Target target = new Target(id);
        target.parseData(info);
        return target;
    }

    private String modernManifest() throws IOException {
        try (InputStream stream = getClass().getResourceAsStream("/bootloader/manifest-2026.04.json")) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String legacyManifest() throws IOException {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.node.ObjectNode root =
                (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(modernManifest());
        root.put("version", 1);
        com.fasterxml.jackson.databind.node.ObjectNode files =
                (com.fasterxml.jackson.databind.node.ObjectNode) root.get("files");
        ((com.fasterxml.jackson.databind.node.ObjectNode) files.get("cf2_nrf-2026.04.bin")).remove("requires");
        files.remove("sd130_bootloader-2024.10.bin");
        files.remove("color-led.bin");
        return root.toString();
    }

    private File archive(String manifest) throws IOException {
        File file = temporary.newFile("firmware.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(file.toPath()))) {
            zip.putNextEntry(new ZipEntry("manifest.json"));
            zip.write(manifest.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            for (String name : new String[]{"cf2-2026.04.bin", "cf2_nrf-2026.04.bin",
                    "sd130_bootloader-2024.10.bin", "lighthouse.bin", "aideck_esp.bin", "color-led.bin"}) {
                zip.putNextEntry(new ZipEntry(name));
                zip.write(new byte[]{1, 2, 3});
                zip.closeEntry();
            }
        }
        return file;
    }

    private static class RecordingDriver extends CrtpDriver {
        final List<byte[]> packets = new ArrayList<>();
        final List<byte[]> writes = new ArrayList<>();
        private CrtpPacket reply;
        boolean dropReply;
        public void connect() { }
        public void disconnect() { }
        public boolean isConnected() { return true; }
        public void sendPacket(CrtpPacket packet) {
            byte[] payload = packet.getPayload();
            packets.add(payload);
            if ((payload[1] & 0xff) == Cloader.WRITE_FLASH) {
                writes.add(payload);
                reply = new CrtpPacket((byte) 0xff,
                        new byte[]{payload[0], (byte) Cloader.WRITE_FLASH, 1, 0});
            }
        }
        public CrtpPacket receivePacket(int wait) {
            if (dropReply) {
                try { Thread.sleep(wait * 1000L); } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                }
                return null;
            }
            if (reply == null) {
                throw new AssertionError("Unexpected receive without a simulated flash acknowledgement");
            }
            CrtpPacket result = reply;
            reply = null;
            return result;
        }
    }
}
