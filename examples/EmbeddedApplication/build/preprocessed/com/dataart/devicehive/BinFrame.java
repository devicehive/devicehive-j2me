package com.dataart.devicehive;

import com.dataart.io.bin.DataOutputStream;
import java.io.IOException;

/**
 * This is base class for all frames specified by binary protocol.
 *
 * The parseFrame() static method is used to parse custom frame from a bytes.
 */
public class BinFrame {

    protected static final int SIGNATURE1 = 0xC5;
    protected static final int SIGNATURE2 = 0xC3;
    protected static final int VERSION    = 0x01;

    public static final int INTENT_REGISTRATION_REQUEST         = 0;
    //public static final int INTENT_REGISTRATION_RESPONSE_BIN    = 1;
    public static final int INTENT_REGISTRATION_RESPONSE_JSON   = 3;
    public static final int INTENT_COMMAND_RESULT               = 2;
    public static final int INTENT_USER_DEFINED                 = 256;

    public int intent;
    public byte[] payload;
    public BinFrame(int intent, byte[] payload) {
        this.intent = intent;
        this.payload = payload;
    }

    /**
     * Format the frame as RAW bytes. This method should be overridden by derived classes.
     *
     * @return The RAW bytes.
     */
    public byte[] format() throws IOException {
        return formatFrame(intent, payload);
    }

    /**
     * Get string representation.
     */
    public String toString() {
        return "intent:" + intent
                + " payload:" + DataUtils.dumpHex(payload);
    }

    /**
     * This is the result of parse operation.
     */
    public static class ParseResult {
        public static final int FRAME_PARSED     = 0;
        public static final int NOT_ENOUGH_DATA  = 1;
        public static final int BAD_CHECKSUM     = 2;
        public static final int BAD_FRAME_FORMAT = 3;

        public BinFrame frame; // parsed frame, may be null
        public byte[] data; // remain, i.e. not parsed data
        public int result;  // parse result, see constants above


        /**
         * Main constructor initializes all members.
         *
         * @param frame The parsed frame, may be null.
         * @param data The remain data, may be empty.
         * @param result The parse result. May be FRAME_PARSED, NOT_ENOUGH_DATA or BAD_CHECKSUM.
         */
        ParseResult(BinFrame frame, byte[] data, int result) {
            this.frame  = frame;
            this.data   = data;
            this.result = result;
        }
    }


    /**
     * Try to parse one frame from an input bytes.
     *
     * @param data The input RAW bytes.
     * @return The parse result.
     */
    public static ParseResult parseFrame(byte[] data) {
        int result = ParseResult.NOT_ENOUGH_DATA;
        BinFrame frame = null;

        int offset = 0; // search for "SIGNATURE1" byte
        while (offset < data.length) {
            for(; offset < data.length; ++offset) {
                final int b = data[offset]&0xFF;
                if (SIGNATURE1 == b) {
                    break;
                }
            }

            if (offset+1 < data.length) {
                final int b = data[offset+1]&0xFF;
                if (SIGNATURE2 != b) {
                    ++offset;
                    continue;
                }
            }

            if (offset+2 < data.length) {
                final int b = data[offset+2]&0xFF;
                if (VERSION != b) {
                    ++offset;
                    continue;
                }
            }

            break; // got "C5C301"
        }

        if (data.length - offset >= (8+0+1)) {
            final int len_lsb = data[offset+4] & 0xFF;
            final int len_msb = data[offset+5] & 0xFF;
            final int int_lsb = data[offset+6] & 0xFF;
            final int int_msb = data[offset+7] & 0xFF;
            final int intent = (int_msb<<8) | int_lsb;
            final int len = (len_msb<<8) | len_lsb;

            if (data.length - offset >= (8+len+1)) {
                if ((checksum(data, offset, 8+len+1)&0xFF) == 0xFF) {
                    byte[] payload = new byte[len];
                    System.arraycopy(data, offset + 8,
                                     payload, 0, len);
                    try {
                        frame = createFrame(intent, payload);
                        result = ParseResult.FRAME_PARSED;
                        offset += 8 + len + 1;
                    } catch (IOException ex) {
                        result = ParseResult.BAD_FRAME_FORMAT;
                        offset += 1; // skip "frame start" byte!
                        // ex.printStackTrace();
                    }
                } else {
                    result = ParseResult.BAD_CHECKSUM;
                    offset += 1; // skip "frame start" byte!
                }
            }
        }

        if (offset > 0) {
            final int N = data.length - offset; // remain
            byte[] new_data = new byte[N];
            System.arraycopy(data, offset,
                             new_data, 0, N);
            data = new_data;
        }

        return new ParseResult(frame, data, result);
    }


    /**
     * Create custom frame. This is frame factory method.
     * Returns UnknownFrame instance for unknown intents.
     *
     * @param intent The frame intent.
     * @param payload The frame payload.
     * @return The frame instance.
     */
    public static BinFrame createFrame(int intent, byte[] payload) throws IOException {
        return new BinFrame(intent, payload);
    }


    /**
     * This is auxiliary method to format frame.
     *
     * @param intent The primary header.
     * @param payload The frame payload.
     * @return The RAW bytes.
     */
    public static byte[] formatFrame(int intent, byte[] payload) throws IOException {
        java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(os);

        try {
            dos.write(SIGNATURE1);
            dos.write(SIGNATURE2);
            dos.write(VERSION);
            dos.write(0x00);
            dos.writeUInt16LE(payload.length);
            dos.writeUInt16LE(intent);
            dos.write(payload);
            dos.write(0x00); // checksum placeholder

            byte[] buf = os.toByteArray();

            // checksum
            int cs = checksum(buf, 0, buf.length-1);
            buf[buf.length-1] = (byte)(0xFF - (cs&0xFF));
            return buf;
        } catch (IOException ex) {
            throw ex; // TODO: add detailed information
        }
    }

    /**
     * Calculate 1-byte checksum.
     *
     * @param data The RAW bytes.
     * @param offset The data offset.
     * @param length The data length.
     * @return The checksum.
     */
    protected static int checksum(byte[] data, int offset, int length) {
        int cs = 0;
        for (int i = 0; i < length; ++i) {
            cs += data[offset + i] & 0xFF;
        }
        return cs;
    }
}
