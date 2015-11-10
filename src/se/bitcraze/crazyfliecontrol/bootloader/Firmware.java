package se.bitcraze.crazyfliecontrol.bootloader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Firmware {

    private String mTagName;
    private String mName;
    private String mCreatedAt;

    private List<Asset> mAssets = new ArrayList<Asset>();

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

    public List<Asset> getAssets() {
        return mAssets;
    }

    public void setAssets(List<Asset> mAssets) {
        this.mAssets = mAssets;
    }

    public void addAsset(Asset asset) {
        this.mAssets.add(asset);
    }

    @Override
    public String toString() {
        return "Firmware [mTagName=" + mTagName + ", mName=" + mName + ", mCreatedAt=" + mCreatedAt + "]";
    }


    public class Asset {

        private String mAssetName;
        private int mSize;
        private String mBrowserDownloadUrl;

        public Asset(String name, int size, String browserDownloadUrl) {
            this.mAssetName = name;
            this.mSize = size;
            this.mBrowserDownloadUrl = browserDownloadUrl;
        }

        public String getName() {
            return mAssetName;
        }

        public int getSize() {
            return mSize;
        }

        public String getBrowserDownloadUrl() {
            return mBrowserDownloadUrl;
        }

        public String getType() {
            // TODO: make this more reliable
            if (mAssetName.startsWith("cf1") || mAssetName.startsWith("Crazyflie1")) {
                return "CF1";
            } else if (mAssetName.startsWith("cf2") || mAssetName.startsWith("Crazyflie2") || mAssetName.startsWith("cflie2")) {
                return "CF2";
            } else {
                return "Unknown";
            }
        }
    }

}
