package se.bitcraze.crazyflielib.toc;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.bitcraze.crazyflielib.crtp.CrtpPacket;

public enum VariableType {
    UINT8_T,
    UINT16_T,
    UINT32_T,
    UINT64_T,
    INT8_T,
    INT16_T,
    INT32_T,
    INT64_T,
    FLOAT,
    DOUBLE;

    final Logger mLogger = LoggerFactory.getLogger(this.getClass().getSimpleName());

    /**
     * Parse one variable of the given type.
     *
     * @param buffer the buffer to read raw data from
     * @return the parsed variable
     */
    public Number parse(ByteBuffer buffer) {
        ByteBuffer tempBuffer = ByteBuffer.allocate(8).order(CrtpPacket.BYTE_ORDER);
        //TODO: simplify
        tempBuffer.put(buffer.get());
        tempBuffer.put(buffer.get());
        tempBuffer.put(buffer.get());
        tempBuffer.put(buffer.get());
        if(this == INT64_T || this == DOUBLE) {
            tempBuffer.put(buffer.get());
            tempBuffer.put(buffer.get());
            tempBuffer.put(buffer.get());
            tempBuffer.put(buffer.get());
        }
        tempBuffer.rewind();
        switch (this) {
            case UINT8_T:
                return ((short) tempBuffer.get()) & 0xff;
            case UINT16_T:
                return ((int) tempBuffer.getShort()) & 0xffff;
            case UINT32_T:
                return ((long) tempBuffer.getInt()) & 0xffffffffL;
            case UINT64_T:
                mLogger.warn("UINT64_T not yet implemented");
                return -1;
            case INT8_T:
                return tempBuffer.get();
            case INT16_T:
                return tempBuffer.getShort();
            case INT32_T:
                return tempBuffer.getInt();
            case INT64_T:
                return tempBuffer.getLong();
            case FLOAT:
                return tempBuffer.getFloat();
            case DOUBLE:
                return tempBuffer.getDouble();
            default:
                mLogger.warn("Parsing " + this.name() + " is not yet implemented");
                break;
        }
        return -1;
    }

    /**
     * Parse one variable of the given type.
     *
     * @param value
     * @return
     */
    public byte[] parse(Number value) {
        ByteBuffer tempBuffer4 = ByteBuffer.allocate(4).order(CrtpPacket.BYTE_ORDER);

        //Use ByteBuffer with 8 bytes for INT64_T and DOUBLE
        ByteBuffer tempBuffer8 = ByteBuffer.allocate(8).order(CrtpPacket.BYTE_ORDER);

        switch (this) {
            case UINT8_T:
                tempBuffer4.putShort((short) (value.byteValue() & 0xff));
                break;
            case UINT16_T:
                tempBuffer4.putInt((int) (value.shortValue() & 0xffff));
                break;
            case UINT32_T:
                tempBuffer4.putInt((int) (((long) value.intValue()) & 0xffffffffL));
                //tempBuffer.putLong((long) (value.intValue() & 0xffffffffL)); //only works if ByteBuffer is 8 bytes long
                break;
            case UINT64_T:
                mLogger.warn("UINT64_T not yet implemented");
                break;
            case INT8_T:
                tempBuffer4.put(value.byteValue());
                break;
            case INT16_T:
                tempBuffer4.putShort(value.shortValue());
                break;
            case INT32_T:
                tempBuffer4.putInt(value.intValue());
                break;
            case INT64_T:
                tempBuffer8.putLong(value.longValue());
                tempBuffer8.rewind();
                return tempBuffer8.array();
            case FLOAT:
                tempBuffer4.putFloat(value.floatValue());
                break;
            case DOUBLE:
                tempBuffer8.putDouble(value.doubleValue());
                tempBuffer8.rewind();
                return tempBuffer8.array();
            default:
                mLogger.warn("Parsing " + this.name() + " is not yet implemented");
                break;
        }
        tempBuffer4.rewind();
        return tempBuffer4.array();
    }

}