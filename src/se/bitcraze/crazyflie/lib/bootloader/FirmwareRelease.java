/**
 *    ||          ____  _ __
 * +------+      / __ )(_) /_______________ _____  ___
 * | 0xBC |     / __  / / __/ ___/ ___/ __ `/_  / / _ \
 * +------+    / /_/ / / /_/ /__/ /  / /_/ / / /_/  __/
 *  ||  ||    /_____/_/\__/\___/_/   \__,_/ /___/\___/
 *
 * Copyright (C) 2017 Bitcraze AB
 *
 * Crazyflie Nano Quadcopter Client
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */

package se.bitcraze.crazyflie.lib.bootloader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FirmwareRelease implements Comparable<FirmwareRelease> {

    private String mTagName;
    private String mName;
    private String mCreatedAt;

    private String mAssetName;
    private int mSize;
    private String mBrowserDownloadUrl;
    private String mReleaseNotes;

    private final SimpleDateFormat inputFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
    private final SimpleDateFormat outputFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    public FirmwareRelease() {
    }

    public FirmwareRelease(String tagName, String name, String createdAt) {
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

    public void setReleaseNotes(String releaseNotes) {
        this.mReleaseNotes = releaseNotes;
    }

    public String getReleaseNotes() {
        return mReleaseNotes;
    }

    public String getType() {
        // TODO: make this more reliable
        String lcAssetName = mAssetName.toLowerCase(Locale.US);
        if (lcAssetName.startsWith("cf1") || lcAssetName.startsWith("crazyflie1")) {
            return "CF1";
        } else if (lcAssetName.startsWith("cf2") || lcAssetName.startsWith("crazyflie2") || lcAssetName.startsWith("cflie2")) {
            return "CF2";
        } else if (lcAssetName.startsWith("crazyflie-")) {
            return "CF1 & CF2";
        } else {
            return "Unknown";
        }
    }

    @Override
    public String toString() {
        return "Firmware [mTagName=" + mTagName + ", mName=" + mName + ", mCreatedAt=" + mCreatedAt + "]";
    }

    @Override
    public int compareTo(FirmwareRelease another) {
        return this.mTagName.compareTo(another.getTagName());
    }

}
