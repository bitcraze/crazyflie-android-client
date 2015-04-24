package se.bitcraze.crazyflielib.param;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import se.bitcraze.crazyflielib.toc.TocElement;
import se.bitcraze.crazyflielib.toc.VariableType;


/**
 * An element in the Param TOC
 *
 */
public class ParamTocElement extends TocElement {

    /*
    types = {0x08: ("uint8_t",  '<B'),
             0x09: ("uint16_t", '<H'),
             0x0A: ("uint32_t", '<L'),
             0x0B: ("uint64_t", '<Q'),
             0x00: ("int8_t",   '<b'),
             0x01: ("int16_t",  '<h'),
             0x02: ("int32_t",  '<i'),
             0x03: ("int64_t",  '<q'),
             0x05: ("FP16",     ''),
             0x06: ("float",    '<f'),
             0x07: ("double",   '<d')}
    */

    public static final Map<Integer, VariableType> VARIABLE_TYPE_MAP;

    static {
        VARIABLE_TYPE_MAP = new HashMap<Integer, VariableType>(10);
        VARIABLE_TYPE_MAP.put(0x08, VariableType.UINT8_T);
        VARIABLE_TYPE_MAP.put(0x09, VariableType.UINT16_T);
        VARIABLE_TYPE_MAP.put(0x0A, VariableType.UINT32_T);
        VARIABLE_TYPE_MAP.put(0x0B, VariableType.UINT64_T);
        VARIABLE_TYPE_MAP.put(0x00, VariableType.INT8_T);
        VARIABLE_TYPE_MAP.put(0x01, VariableType.INT16_T);
        VARIABLE_TYPE_MAP.put(0x02, VariableType.INT32_T);
        VARIABLE_TYPE_MAP.put(0x03, VariableType.INT64_T);
        VARIABLE_TYPE_MAP.put(0x06, VariableType.FLOAT);
        VARIABLE_TYPE_MAP.put(0x07, VariableType.DOUBLE);
    }

    // empty constructor is needed for (de)serialization
    public ParamTocElement() {
    }

    /**
     * TocElement creator. Data is the binary payload of the element.
     */
    public ParamTocElement(byte[] data) {
        if (data != null) {
            setGroupAndName(data);

            setIdent(data[0]);

            setCtype(VARIABLE_TYPE_MAP.get(data[1] & 0x0F));

            // setting pytype not needed in Java cf lib

            if ((data[1] & 0x40) != 0) {
                setAccess(RO_ACCESS);
            } else {
                setAccess(RW_ACCESS);
            }
        }
    }

    private void setGroupAndName(byte[] payload) {
        if(payload != null && payload.length > 0) {
            int offset = 2;
            byte[] trimmedPayload = new byte[payload.length-offset];
            System.arraycopy(payload, offset, trimmedPayload, 0, trimmedPayload.length);
            String temp = new String(trimmedPayload, Charset.forName("US-ASCII"));
            String[] split = temp.split("\0");
            setGroup(split[0]);
            setName(split[1]);
        }
    }

}
