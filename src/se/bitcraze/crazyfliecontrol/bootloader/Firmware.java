package se.bitcraze.crazyfliecontrol.bootloader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Firmware {

    private String mTagName;
    private String mName;
    private String mCreatedAt;

    private String mAssetName;
    private int mSize;
    private String mBrowserDownloadUrl;

    private final SimpleDateFormat inputFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
    private final SimpleDateFormat outputFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    public Firmware() {
    }

    public Firmware(String tagName, String name, String createdAt) {
        this.mTagName = tagName;
        this.mName = name;

        try {
            Date date = inputFormatter.parse(createdAt);
            this.mCreatedAt = outputFormatter.format(date);
        } catch (ParseException e) {
            this.mCreatedAt = createdAt;
        }
    }

    public String getTagName() {
        return mTagName;
    }

    public String getName() {
        return mName;
    }

    public String getCreatedAt() {
        return mCreatedAt;
    }

    public void setAsset(String assetName, int assetSize, String URL) {
        this.mAssetName = assetName;
        this.mSize = assetSize;
        this.mBrowserDownloadUrl = URL;
    }

    public String getAssetName() {
        return mAssetName;
    }

    public int getAssetSize() {
        return mSize;
    }

    public String getBrowserDownloadUrl() {
        return mBrowserDownloadUrl;
    }

    public String getType() {
        // TODO: make this more reliable
        String lcAssetName = mAssetName.toLowerCase(Locale.US);
        if (lcAssetName.startsWith("cf1") || lcAssetName.startsWith("crazyflie1")) {
            return "CF1";
        } else if (lcAssetName.startsWith("cf2") || lcAssetName.startsWith("crazyflie2") || lcAssetName.startsWith("cflie2")) {
            return "CF2";
        } else {
            return "Unknown";
        }
    }

    @Override
    public String toString() {
        return "Firmware [mTagName=" + mTagName + ", mName=" + mName + ", mCreatedAt=" + mCreatedAt + "]";
    }

}
