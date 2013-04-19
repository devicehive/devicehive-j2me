package com.dataart.devicehive;

import com.dataart.io.bin.DataInputStream;
import com.dataart.io.bin.DataOutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import org.json.me.JSONArray;
import org.json.me.JSONObject;

/**
 */
public class RemoteDevice {
    // fixed size types
    static final int DT_NULL    = 0;    // NULL type, zero length.
    static final int DT_UINT8   = 1;    // unsigned byte, 1 byte.
    static final int DT_UINT16  = 2;    // unsigned word, 2 bytes, little-endian.
    static final int DT_UINT32  = 3;    // unsigned double word, 4 bytes, little-endian.
    static final int DT_UINT64  = 4;    // unsigned quad word, 8 bytes, little-endian.
    static final int DT_INT8    = 5;    // signed byte, 1 byte.
    static final int DT_INT16   = 6;    // signed word, 2 bytes, little-endian.
    static final int DT_INT32   = 7;    // signed double word, 4 bytes, little-endian.
    static final int DT_INT64   = 8;    // signed quad word, 8 bytes, little-endian.
    static final int DT_SINGLE  = 9;    // float, 4 bytes.
    static final int DT_DOUBLE  = 10;   // double, 8 bytes.
    static final int DT_BOOL    = 11;   // boolean, 1 byte.
    static final int DT_UUID    = 12;   // UUID, 16 bytes, little-endian.

    // variable size types
    static final int DT_STRING  = 13;   // UTF string, variable length.
    static final int DT_BINARY  = 14;   // binary data, variable length.
    static final int DT_ARRAY   = 15;   // array, variable length.
    static final int DT_OBJECT  = 16;   // object, variable length.

    // data layout
    public static class Layout {
        private Vector content = new Vector();
        public String name;         // layout name
        public int intent;          // intent number

        // one element
        static class Element {
            String name;        // element name
            int dataType;       // see constants above
            Layout sublayout;   // for an array and an object

            Element(String name, int dataType) {
                this.name = name;
                this.dataType = dataType;
            }

            Element(String name, int dataType, Layout sublayout) {
                this.name = name;
                this.dataType = dataType;
                this.sublayout = sublayout;
            }
        }

        // get all elements
        Enumeration elements() {
            return content.elements();
        }

        // add an element
        void add(Element element) {
            content.addElement(element);
        }
    }

    // intent => Layout
    private Hashtable layoutByIntent = new Hashtable();
    private Hashtable intentByName = new Hashtable();

    // constructor
    public RemoteDevice() {
        Layout regReq = new Layout();
        regReq.add(new Layout.Element("data", DT_NULL));
        registerLayout(BinFrame.INTENT_REGISTRATION_REQUEST, "reg_req", regReq);

        Layout regJson = new Layout();
        regJson.add(new Layout.Element("json", DT_STRING));
        registerLayout(BinFrame.INTENT_REGISTRATION_RESPONSE_JSON, "reg_json", regJson);

        Layout cmdRes = new Layout();
        cmdRes.add(new Layout.Element("id", DT_UINT32));
        cmdRes.add(new Layout.Element("status", DT_STRING));
        cmdRes.add(new Layout.Element("result", DT_STRING));
        registerLayout(BinFrame.INTENT_COMMAND_RESULT, "cmd_res", cmdRes);
    }

    // handle register info
    public void handleRegisterResponse(JSONObject jval) throws Exception {
        if (true) { // update commands
            JSONArray jcommands = jval.getJSONArray("commands");
            for (int i = 0; i < jcommands.length(); ++i) {
                JSONObject jcommand = jcommands.getJSONObject(i);

                final int intent = jcommand.getInt("intent");
                final String name = jcommand.getString("name");

                Layout layout = new Layout();
                layout.add(new Layout.Element("id", DT_UINT32));
                layout.add(parseCommandParamsField("parameters", jcommand.opt("params")));

                registerLayout(intent, name, layout);
            }
        }

        if (true) { // update notifications
            JSONArray jnotifications = jval.getJSONArray("notifications");
            for (int i = 0; i < jnotifications.length(); ++i) {
                JSONObject jnotification = jnotifications.getJSONObject(i);

                final int intent = jnotification.getInt("intent");
                final String name = jnotification.getString("name");

                Layout layout = new Layout();
                layout.add(parseCommandParamsField("parameters", jnotification.opt("params")));

                registerLayout(intent, name, layout);
            }
        }
    }


    /**
     * Parse the command or notification parameters (Structure in JSON format).
     */
    static Layout parseCommandParamsStruct(JSONObject jval) throws Exception {
        Layout layout = new Layout();
        for (Enumeration e = jval.orderedKeys(); e.hasMoreElements(); ) {
            final String name = (String)e.nextElement();
            Object field = jval.get(name);

            layout.add(parseCommandParamsField(name, field));
        }

        return layout;
    }


    /**
     * Parse the command or notification parameters (Field in JSON format).
     */
    static Layout.Element parseCommandParamsField(String name, Object jval) throws Exception {
        if (jval == null) { // NULL as primitive type
            return new Layout.Element(name, DT_NULL);
        } else if (jval instanceof String) { // a primitive type
            return new Layout.Element(name,
                parsePrimitiveDataType((String)jval));
        } else if (jval instanceof JSONObject) { // object
            return new Layout.Element(name, DT_OBJECT,
                parseCommandParamsStruct((JSONObject)jval));
        } else if (jval instanceof JSONArray) { // array
            JSONArray jarr = (JSONArray)jval;
            if (jarr.length() != 1) {
                throw new Exception("invalid array field [1 element expected]");
            }

            Layout sublayout = new Layout();
            sublayout.add(parseCommandParamsField(null, jarr.get(0)));

            return new Layout.Element(name, DT_ARRAY, sublayout);
        } else {
            throw new Exception("unknown field type");
        }
    }

    // Parse primitive data type.
    static int parsePrimitiveDataType(String type) throws Exception {
        if ("bool".equalsIgnoreCase(type)) {
            return DT_BOOL;
        } else if ("u8".equalsIgnoreCase(type) || "uint8".equalsIgnoreCase(type)) {
            return DT_UINT8;
        } else if ("i8".equalsIgnoreCase(type) || "int8".equalsIgnoreCase(type)) {
            return DT_INT8;
        } else if ("u16".equalsIgnoreCase(type) || "uint16".equalsIgnoreCase(type)) {
            return DT_UINT16;
        } else if ("i16".equalsIgnoreCase(type) || "int16".equalsIgnoreCase(type)) {
            return DT_INT16;
        } else if ("u32".equalsIgnoreCase(type) || "uint32".equalsIgnoreCase(type)) {
            return DT_UINT32;
        } else if ("i32".equalsIgnoreCase(type) || "int32".equalsIgnoreCase(type)) {
            return DT_INT32;
        } else if ("u64".equalsIgnoreCase(type) || "uint64".equalsIgnoreCase(type)) {
            return DT_UINT64;
        } else if ("i64".equalsIgnoreCase(type) || "int64".equalsIgnoreCase(type)) {
            return DT_INT64;
        } else if ("f".equalsIgnoreCase(type) || "single".equalsIgnoreCase(type)) {
            return DT_SINGLE;
        } else if ("ff".equalsIgnoreCase(type) || "double".equalsIgnoreCase(type)) {
            return DT_DOUBLE;
        } else if ("uuid".equalsIgnoreCase(type) || "guid".equalsIgnoreCase(type)) {
            return DT_UUID;
        } else if ("s".equalsIgnoreCase(type) || "str".equalsIgnoreCase(type) || "string".equalsIgnoreCase(type)) {
            return DT_STRING;
        } else if ("b".equalsIgnoreCase(type) || "bin".equalsIgnoreCase(type) || "binary".equalsIgnoreCase(type)) {
            return DT_BINARY;
        } else {
            throw new Exception("unknown primitive type");
        }
    }


    // find layout by intent
    public Layout findLayoutByIntent(int intent) {
        return (Layout)layoutByIntent.get(new Integer(intent));
    }

    // find intent by name
    public int findIntentByName(String name) {
        Object val = intentByName.get(name);
        if (val != null) {
            return ((Integer)val).intValue();
        }
        return -1; // not found
    }

    public void registerLayout(int intent, String name, Layout layout) {
        Integer key = new Integer(intent);
        layoutByIntent.put(key, layout);
        layout.name = name;
        layout.intent = intent;
        if (name != null) {
            intentByName.put(name, key);
        }
    }


    /**
     * Convert binary data to JSON value.
     */
    public static Object bin2json(DataInputStream bs, Layout layout) throws Exception {
        Object jval = null;

        for (Enumeration e = layout.elements(); e.hasMoreElements(); ) {
            Layout.Element elem = (Layout.Element)e.nextElement();

            if (elem.name != null) {
                if (jval == null) {
                    jval = new JSONObject();
                }
                ((JSONObject)jval).put(elem.name, bin2json(bs, elem));
            } else {
                jval = bin2json(bs, elem);
            }
        }

        return jval;
    }


    /**
     * Convert binary data to JSON value.
     */
    static Object bin2json(DataInputStream bs, Layout.Element layoutElement) throws Exception {
        switch (layoutElement.dataType) {
            case DT_NULL:   return JSONObject.NULL;
            case DT_UINT8:  return new Integer(bs.readUInt8());
            case DT_UINT16: return new Integer(bs.readUInt16LE());
            case DT_UINT32: return new Long(bs.readUInt32LE());
            case DT_UINT64: return new Long(bs.readUInt64LE());
            case DT_INT8:   return new Integer(bs.readInt8());
            case DT_INT16:  return new Integer(bs.readInt16LE());
            case DT_INT32:  return new Long(bs.readInt32LE());
            case DT_INT64:  return new Long(bs.readInt64LE());
            case DT_BOOL:   return new Boolean(bs.readUInt8() != 0);
            case DT_SINGLE: return new Float(bs.readFloat());
            case DT_DOUBLE: return new Double(bs.readDouble());
            case DT_UUID: {
                byte[] buf = new byte[16];
                bs.readFully(buf);
                return DataUtils.formatUUID(buf);
            }

            case DT_STRING:
                return bs.readBinString();

            case DT_BINARY: {
                final int len = bs.readUInt16LE();
                byte[] buf = new byte[len];
                bs.readFully(buf);
                return DataUtils.dumpHex(buf);
            }

            case DT_ARRAY: {
                final int N = bs.readUInt16LE();
                JSONArray jarr = new JSONArray();
                for (int i = 0; i < N; ++i) {
                    jarr.put(bin2json(bs, layoutElement.sublayout));
                }
                return jarr;
            }

            case DT_OBJECT:
                return bin2json(bs, layoutElement.sublayout);
        }

        throw new Exception("unknown data type");
    }


    /**
     * Convert JSON value to binary data.
     */
    public static void json2bin(Object jval, DataOutputStream bs, Layout layout) throws Exception {
        for (Enumeration e = layout.elements(); e.hasMoreElements(); ) {
            Layout.Element elem = (Layout.Element)e.nextElement();

            if (elem.name != null) {
                json2bin(((JSONObject)jval).get(elem.name), bs, elem);
            } else {
                json2bin(jval, bs, elem);
            }
        }
    }


    /**
     * Convert JSON value to binary data.
     */
    static void json2bin(Object jval, DataOutputStream bs, Layout.Element layoutElement) throws Exception {
        // TODO: more checks on data types!!!
        switch (layoutElement.dataType) {
            case DT_NULL:
                break;

            case DT_UINT8:
                if (jval instanceof Boolean) {
                    bs.writeUInt8(((Boolean)jval).booleanValue() ? 1:0);
                } else if (jval instanceof Integer) {
                    bs.writeUInt8(((Integer)jval).intValue());
                } else if (jval instanceof Long) {
                    bs.writeUInt8((int)((Long)jval).longValue());
                } else {
                    throw new Exception("integer expected");
                }
                break;

            case DT_UINT16:
                if (jval instanceof Boolean) {
                    bs.writeUInt16LE(((Boolean)jval).booleanValue() ? 1:0);
                } else if (jval instanceof Integer) {
                    bs.writeUInt16LE(((Integer)jval).intValue());
                } else if (jval instanceof Long) {
                    bs.writeUInt16LE((int)((Long)jval).longValue());
                } else {
                    throw new Exception("integer expected");
                }
                break;

            case DT_UINT32:
                if (jval instanceof Boolean) {
                    bs.writeUInt32LE(((Boolean)jval).booleanValue() ? 1:0);
                } else if (jval instanceof Integer) {
                    bs.writeUInt32LE(((Integer)jval).intValue());
                } else if (jval instanceof Long) {
                    bs.writeUInt32LE(((Long)jval).longValue());
                } else {
                    throw new Exception("integer expected");
                }
                break;

            case DT_UINT64:
                if (jval instanceof Boolean) {
                    bs.writeUInt64LE(((Boolean)jval).booleanValue() ? 1:0);
                } else if (jval instanceof Integer) {
                    bs.writeUInt64LE(((Integer)jval).intValue());
                } else if (jval instanceof Long) {
                    bs.writeUInt64LE(((Long)jval).longValue());
                } else {
                    throw new Exception("integer expected");
                }
                break;

            case DT_INT8:
                if (jval instanceof Boolean) {
                    bs.writeInt8(((Boolean)jval).booleanValue() ? 1:0);
                } else if (jval instanceof Integer) {
                    bs.writeInt8(((Integer)jval).intValue());
                } else if (jval instanceof Long) {
                    bs.writeInt8((int)((Long)jval).longValue());
                } else {
                    throw new Exception("integer expected");
                }
                break;

            case DT_INT16:
                if (jval instanceof Boolean) {
                    bs.writeInt16LE(((Boolean)jval).booleanValue() ? 1:0);
                } else if (jval instanceof Integer) {
                    bs.writeInt16LE(((Integer)jval).intValue());
                } else if (jval instanceof Long) {
                    bs.writeInt16LE((int)((Long)jval).longValue());
                } else {
                    throw new Exception("integer expected");
                }
                break;

            case DT_INT32:
                if (jval instanceof Boolean) {
                    bs.writeInt32LE(((Boolean)jval).booleanValue() ? 1:0);
                } else if (jval instanceof Integer) {
                    bs.writeInt32LE(((Integer)jval).intValue());
                } else if (jval instanceof Long) {
                    bs.writeInt32LE(((Long)jval).longValue());
                } else {
                    throw new Exception("integer expected");
                }
                break;

            case DT_INT64:
                if (jval instanceof Boolean) {
                    bs.writeInt64LE(((Boolean)jval).booleanValue() ? 1:0);
                } else if (jval instanceof Integer) {
                    bs.writeInt64LE(((Integer)jval).intValue());
                } else if (jval instanceof Long) {
                    bs.writeInt64LE(((Long)jval).longValue());
                } else {
                    throw new Exception("integer expected");
                }
                break;

            case DT_BOOL:
                if (jval instanceof Boolean) {
                    bs.writeUInt8(((Boolean)jval).booleanValue() ? 1:0);
                } else if (jval instanceof Integer) {
                    bs.writeUInt8(((Integer)jval).intValue());
                } else if (jval instanceof Long) {
                    bs.writeUInt8((int)((Long)jval).longValue());
                } else {
                    throw new Exception("integer expected");
                }
                break;

            case DT_SINGLE:
                if (jval instanceof Boolean) {
                    bs.writeFloat(((Boolean)jval).booleanValue() ? 1:0);
                } else if (jval instanceof Integer) {
                    bs.writeFloat(((Integer)jval).intValue());
                } else if (jval instanceof Long) {
                    bs.writeFloat(((Long)jval).longValue());
                } else if (jval instanceof Float) {
                    bs.writeFloat(((Float)jval).floatValue());
                } else if (jval instanceof Double) {
                    bs.writeFloat(((Double)jval).floatValue());
                } else {
                    throw new Exception("float expected");
                }
                break;

            case DT_DOUBLE:
                if (jval instanceof Boolean) {
                    bs.writeDouble(((Boolean)jval).booleanValue() ? 1:0);
                } else if (jval instanceof Integer) {
                    bs.writeDouble(((Integer)jval).intValue());
                } else if (jval instanceof Long) {
                    bs.writeDouble(((Long)jval).longValue());
                } else if (jval instanceof Float) {
                    bs.writeDouble(((Float)jval).floatValue());
                } else if (jval instanceof Double) {
                    bs.writeDouble(((Double)jval).floatValue());
                } else {
                    throw new Exception("float expected");
                }
                break;

            case DT_UUID: {
                String str = (String)jval;
                byte[] buf = DataUtils.parseHexString(str);
                if (buf.length != 16) {
                    throw new Exception("invalid UUID format");
                }
                bs.writeUInt16LE(buf.length);
                bs.write(buf);
            } break;

            case DT_STRING: {
                String str = (String)jval;
                bs.writeBinString(str);
            } break;

            case DT_BINARY: {
                String str = (String)jval;
                byte[] buf = DataUtils.parseHexString(str);
                bs.writeUInt16LE(buf.length);
                bs.write(buf);
            } break;

            case DT_ARRAY: {
                if (jval instanceof JSONArray) {
                    JSONArray jarr = (JSONArray)jval;
                    bs.writeUInt16LE(jarr.length());
                    for (int i = 0; i < jarr.length(); ++i) {
                        json2bin(jarr.get(i), bs, layoutElement.sublayout);
                    }
                } else {
                    throw new Exception("is not an array");
                }
            } break;

            case DT_OBJECT: {
                json2bin(jval, bs, layoutElement.sublayout);
            } break;

            default:
                throw new Exception("unknown data type");
        }
    }
}
