/**
 *    ||          ____  _ __
 * +------+      / __ )(_) /_______________ _____  ___
 * | 0xBC |     / __  / / __/ ___/ ___/ __ `/_  / / _ \
 * +------+    / /_/ / / /_/ /__/ /  / /_/ / / /_/  __/
 *  ||  ||    /_____/_/\__/\___/_/   \__,_/ /___/\___/
 *
 * Copyright (C) 2015 Bitcraze AB
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

/**
 * Needs to be in a separate class, otherwise more JSON serialization magic is required
 *
 */
public class FirmwareDetails {
    private String mPlatform;
    private String mTarget;
    private String mType;
    private String mRelease;
    private String mRepository;

    public FirmwareDetails() {
    }

    public FirmwareDetails(String platform, String target, String type, String release, String repository) {
        this.mPlatform = platform;
        this.mTarget = target;
        this.mType = type;
        this.mRelease = release;
        this.mRepository = repository;
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

    public String getRelease() {
        return mRelease;
    }

    public void setRelease(String mRelease) {
        this.mRelease = mRelease;
    }

    public String getRepository() {
        return mRepository;
    }

    public void setRepository(String mRepository) {
        this.mRepository = mRepository;
    }

    @Override
    public String toString() {
        return "FirmwareDetails [Platform=" + mPlatform + ", Target=" + mTarget + ", Type=" + mType + ", Release=" + mRelease + ", Repository=" + mRepository + "]";
    }

}