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

package se.bitcraze.crazyflie.lib.toc;

import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import se.bitcraze.crazyflie.lib.crtp.CrtpPort;

/**
 * An element in the TOC
 *
 */
public class TocElement implements Comparable<TocElement> {

    final Logger mLogger = LoggerFactory.getLogger("TocElement");

    public static final int RW_ACCESS = 1;
    public static final int RO_ACCESS = 0;

    private int mIdent = 0;
    private String mGroup = "";
    private String mName = "";
    private VariableType mCtype;
    private int mAccess = RO_ACCESS;

    public TocElement() {
    }

    /**
     * TocElement creator
     *
     * @param data the binary payload of the element
     */
    public TocElement(CrtpPort port, byte[] data) {
        this();
        if (data != null) {
            setGroupAndName(data);
            setIdent(data[0] & 0x00ff);
            if (port == CrtpPort.LOGGING) {
                setCtype(new Toc().getVariableTypeMapLog().get(data[1] & 0x0F)); 
            } else {
                setCtype(new Toc().getVariableTypeMapParam().get(data[1] & 0x0F));
            }

            // setting pytype not needed in Java cf lib

            //TODO: self.access = ord(data[1]) & 0x10 ?!
            if ((data[1] & 0x40) != 0) {
                setAccess(RO_ACCESS);
            } else {
                setAccess(RW_ACCESS);
            }
        }
    }

    protected void fillVariableTypeMap() {
        //empty on purpose, implemented in LogTocElement and ParamTocElement
    }

    public int getIdent() {
        return mIdent;
    }

    public void setIdent(int ident) {
        this.mIdent = ident;
    }

    public String getGroup() {
        return mGroup;
    }

    public void setGroup(String group) {
        this.mGroup = group;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    @JsonIgnore
    public String getCompleteName() {
        return mGroup + "." + mName;
    }

    public VariableType getCtype() {
        return mCtype;
    }

    public void setCtype(VariableType ctype) {
        this.mCtype = ctype;
    }

    public int getAccess() {
        return mAccess;
    }

    public void setAccess(int access) {
        this.mAccess = access;
    }

    @JsonIgnore
    public String getReadableAccess() {
        return (getAccess() == RO_ACCESS) ? "RO" : "RW";
    }

    protected void setGroupAndName(byte[] payload) {
        int offset = 2;
        byte[] trimmedPayload = new byte[payload.length-offset];
        System.arraycopy(payload, offset, trimmedPayload, 0, trimmedPayload.length);
        String temp = new String(trimmedPayload, Charset.forName("US-ASCII"));
        String[] split = temp.split("\0");
        if (split.length != 2) {
            mLogger.debug("Group and Name could not be assigned: " + temp);
            return;
        }
        setGroup(split[0]);
        setName(split[1]);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + ": " + this.getGroup() + "." + this.getName() + " (" + this.getIdent() + ", " + this.getCtype() + ", " + this.getReadableAccess() + ")";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + mAccess;
        result = prime * result + ((mCtype == null) ? 0 : mCtype.hashCode());
        result = prime * result + ((mGroup == null) ? 0 : mGroup.hashCode());
        result = prime * result + mIdent;
        result = prime * result + ((mName == null) ? 0 : mName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof TocElement)) {
            return false;
        }
        TocElement other = (TocElement) obj;
        if (mAccess != other.mAccess) {
            return false;
        }
        if (mCtype != other.mCtype) {
            return false;
        }
        if (mGroup == null) {
            if (other.mGroup != null) {
                return false;
            }
        } else if (!mGroup.equals(other.mGroup)) {
            return false;
        }
        if (mIdent != other.mIdent) {
            return false;
        }
        if (mName == null) {
            if (other.mName != null) {
                return false;
            }
        } else if (!mName.equals(other.mName)) {
            return false;
        }
        return true;
    }

    public int compareTo(TocElement te) {
        // int identCmp = Integer.compare(this.getIdent(), te.getIdent()); // only supported in API level 19+
        int identCmp = Integer.valueOf(this.getIdent()).compareTo(Integer.valueOf(te.getIdent()));
        return (identCmp != 0 ? identCmp : this.getCompleteName().compareTo(te.getCompleteName()));
    }

}
