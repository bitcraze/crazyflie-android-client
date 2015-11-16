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

    public class FirmwareDetails {
        private String mPlatform;
        private String mTarget;
        private String mType;

        public FirmwareDetails() {
        }

        public FirmwareDetails(String platform, String target, String type) {
            this.mPlatform = platform;
            this.mTarget = target;
            this.mType = type;
        }

        public String getPlatform() {
            return mPlatform;
        }

        public void setPlatform(String platform) {
            this.mPlatform = platform;
        }

        public String getTarget() {
            return mTarget;
        }

        public void setTarget(String target) {
            this.mTarget = target;
        }

        public String getType() {
            return mType;
        }

        public void setType(String type) {
            this.mType = type;
        }

        @Override
        public String toString() {
            return "FirmwareDetails [Platform=" + mPlatform + ", Target=" + mTarget + ", Type=" + mType + "]";
        }
    }
}