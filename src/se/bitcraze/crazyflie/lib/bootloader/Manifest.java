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

import java.util.HashMap;
import java.util.Map;


public class Manifest {
    private int mVersion;
    private int mSubversion;
    private String mRelease;
    private Map<String, FirmwareDetails> mFiles = new HashMap<String, FirmwareDetails>();

    public Manifest() {

    }

    public Manifest(int version, int subversion, String release, Map<String, FirmwareDetails> files) {
        this.mVersion = version;
        this.mSubversion = subversion;
        this.mRelease = release;
        this.mFiles = files;
    }

    public int getVersion() {
        return mVersion;
    }

    public void setVersion(int version) {
        this.mVersion = version;
    }

    public int getSubversion() {
        return mSubversion;
    }

    public void setSubversion(int mSubVersion) {
        this.mSubversion = mSubVersion;
    }

    public String getRelease() {
        return mRelease;
    }

    public void setRelease(String mRelease) {
        this.mRelease = mRelease;
    }

    public Map<String, FirmwareDetails> getFiles() {
        return mFiles;
    }

    public void setFiles(Map<String, FirmwareDetails> files) {
        this.mFiles = files;
    }

}