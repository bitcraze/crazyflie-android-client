package se.bitcraze.crazyflielib.bootloader;

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