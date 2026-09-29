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

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Needs to be in a separate class, otherwise more JSON serialization magic is required
 *
 */
public class FirmwareDetails {
    private String mPlatform;
    private List<String> mTarget = new ArrayList<String>();
    private String mType;
    private String mRelease;
    private String mRepository;
    private List<String> mRequires = new ArrayList<String>();

    public FirmwareDetails() {
    }

    public FirmwareDetails(String platform, String target, String type, String release, String repository) {
        this.mPlatform = platform;
        this.mTarget = Collections.singletonList(target);
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

    public List<String> getTarget() {
        return mTarget;
    }

    public void setTarget(JsonNode target) {
        List<String> targets = new ArrayList<String>();
        if (target.isTextual()) {
            targets.add(target.asText());
        } else if (target.isArray()) {
            for (JsonNode name : target) {
                if (!name.isTextual()) {
                    throw new IllegalArgumentException("Firmware targets must be strings");
                }
                targets.add(name.asText());
            }
        } else {
            throw new IllegalArgumentException("Firmware target must be a string or a list of strings");
        }
        this.mTarget = targets;
    }

    public List<String> getRequires() {
        return mRequires;
    }

    public void setRequires(List<String> requires) {
        this.mRequires = requires == null ? new ArrayList<String>() : requires;
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
