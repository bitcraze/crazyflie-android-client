package se.bitcraze.crazyflielib.bootloader;

import java.util.HashMap;
import java.util.Map;


public class Manifest {
    private int mVersion;
    private Map<String, FirmwareDetails> mFiles = new HashMap<String, FirmwareDetails>();

    public Manifest() {

    }

    public Manifest(int version, Map<String, FirmwareDetails> files) {
        this.mVersion = version;
        this.mFiles = files;
    }

    public int getVersion() {
        return mVersion;
    }

    public void setVersion(int version) {
        this.mVersion = version;
    }

    public Map<String, FirmwareDetails> getFiles() {
        return mFiles;
    }

    public void setFiles(Map<String, FirmwareDetails> files) {
        this.mFiles = files;
    }
}