/**
 *    ||          ____  _ __
 * +------+      / __ )(_) /_______________ _____  ___
 * | 0xBC |     / __  / / __/ ___/ ___/ __ `/_  / / _ \
 * +------+    / /_/ / / /_/ /__/ /  / /_/ / / /_/  __/
 *  ||  ||    /_____/_/\__/\___/_/   \__,_/ /___/\___/
 *
 * Copyright (C) 2013 Bitcraze AB
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

package se.bitcraze.crazyflie.lib.crtp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Packet of data which can be sent/received from/to the Crazyflie. All packet
 * implementations must be immutable to avoid issues with modifying packets via
 * references, e.g. in a send queue.
 */
public class CrtpPacket {

    /**
     * Byte order used when serializing/deserializing packets.
     */
    public static final ByteOrder BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;

    /**
     * NULL packet. Header is 0xFF without any data.
     */
    public static final CrtpPacket NULL_PACKET = new CrtpPacket((byte) 0xff, new byte[0]) {
        @Override
        protected void serializeData(ByteBuffer buffer) {
        }

        @Override
        protected int getDataByteCount() {
            return 0;
        }
    };

    public static class Header {

        private int mChannel;
        private CrtpPort mPort;
        private boolean isNullPacketHeader = false;

        public int getChannel() {
            return mChannel;
        }

        public CrtpPort getPort() {
            return mPort;
        }

        public Header(byte header) {
            if(header != -1){
                this.mPort = CrtpPort.getByNumber((byte) (header >> 4));
                this.mChannel = header & 0x03;
            }else{
                isNullPacketHeader = true;
                this.mPort = CrtpPort.UNKNOWN;
                this.mChannel = 0;
            }
        }

        //TODO: change order of parameters according to python cflib?
        public Header(int channel, CrtpPort port){
            this.mChannel = channel;
            this.mPort = port;
        }

        public byte getByte(){
            if(isNullPacketHeader) {
                return (byte) 0xFF;
            }
            return (byte) (((mPort.getNumber() & 0x0F) << 4) | (mChannel & 0x03));
        }

        public String toString() {
            return "Header - Channel: " + getChannel() + " Port: " + getPort();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (isNullPacketHeader ? 1231 : 1237);
            result = prime * result + mChannel;
            result = prime * result + ((mPort == null) ? 0 : mPort.hashCode());
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
            if (!(obj instanceof Header)) {
                return false;
            }
            Header other = (Header) obj;
            if (isNullPacketHeader != other.isNullPacketHeader) {
                return false;
            }
            if (mChannel != other.mChannel) {
                return false;
            }
            if (mPort != other.mPort) {
                return false;
            }
            return true;
        }
    }

    private final Header mPacketHeader;
    private final byte[] mPacketPayload;
    private byte[] mSerializedPacket;
    private byte[] mExpectedReply;

    public CrtpPacket() {
        mPacketHeader = null;
        mPacketPayload = null;
    }

    /**
     * Create a new packet.
     *
     * @param channel channel to set in the header.
     * @param port port to set in the header.
     */
    public CrtpPacket(int channel, CrtpPort port) {
        this.mPacketHeader = new Header(channel, port);
        this.mPacketPayload = new byte[0];
        this.mSerializedPacket = null;
    }

    /**
     * Create a new packet.
     *
     * @param packetHeader header of the packet.
     * @param packetPayload payload of the packet.
     */
    public CrtpPacket(byte packetHeader, byte[] packetPayload) {
        this.mPacketHeader = new Header(packetHeader);
        this.mPacketPayload = packetPayload;
        this.mSerializedPacket = null;
    }

    /**
     * Create a new packet.
     *
     * @param packetData
     */
    public CrtpPacket(byte[] packetData) {
        this.mPacketHeader = new Header(packetData[0]);
        this.mPacketPayload = Arrays.copyOfRange(packetData, 1, packetData.length);
        this.mSerializedPacket = null;
    }

    /**
     * Get the header of the packet.
     *
     * @return the header of the packet.
     */
    public byte getHeaderByte() {
        return mPacketHeader.getByte();
    }

    /**
     * Get the header of the packet.
     *
     * @return the header of the packet.
     */
    public Header getHeader() {
        return mPacketHeader;
    }

    /**
     * Get the payload of the packet.
     *
     * @return the payload of the packet.
     */
    public byte[] getPayload() {
        return mPacketPayload;
    }

    /**
     * Serialize the data of the packet. Must not include the header.
     *
     * @param buffer the target buffer for serialization.
     */
    protected void serializeData(ByteBuffer buffer){
        buffer.put(mPacketPayload);
    }

    /**
     * Get the number of bytes used when serializing the data.
     *
     * @return number of bytes required by the serialized data.
     */
    protected int getDataByteCount(){
        return mPacketPayload.length;
    }

    /**
     * Convert the packet to a byte array suitable for transmission.
     *
     * @return byte array containing the header and packet data.
     */
    public byte[] toByteArray() {
        // if it's the first call, serialize the packet and cache it
        if (mSerializedPacket == null) {
            ByteBuffer buffer = ByteBuffer.allocate(getDataByteCount() + 1).order(BYTE_ORDER);
            buffer.put(getHeaderByte());
            serializeData(buffer);
            mSerializedPacket = buffer.array();
        }
        return mSerializedPacket;
    }

    @Override
    public String toString() {
        return "CrtpPacket: port: " + this.getHeader().getPort() + " channel: " + this.getHeader().getChannel();
    }

    public byte[] getExpectedReply() {
        return mExpectedReply;
    }

    public void setExpectedReply(byte[] mExpectedReply) {
        this.mExpectedReply = mExpectedReply;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(mExpectedReply);
        result = prime * result + ((mPacketHeader == null) ? 0 : mPacketHeader.hashCode());
        result = prime * result + Arrays.hashCode(mPacketPayload);
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
        if (!(obj instanceof CrtpPacket)) {
            return false;
        }
        CrtpPacket other = (CrtpPacket) obj;
        if (!Arrays.equals(mExpectedReply, other.mExpectedReply)) {
            return false;
        }
        if (mPacketHeader == null) {
            if (other.mPacketHeader != null) {
                return false;
            }
        } else if (!mPacketHeader.equals(other.mPacketHeader)) {
            return false;
        }
        if (!Arrays.equals(mPacketPayload, other.mPacketPayload)) {
            return false;
        }
        return true;
    }

}
