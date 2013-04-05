package com.dataart.devicehive;

import java.io.ByteArrayOutputStream;
import java.util.Random;

/**
 */
public class DataUtils {


    private static final char[] HEX = { '0', '1', '2', '3', '4', '5', '6', '7',
                                        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    /**
     * Returns the RAW bytes as hexadecimal string.
     * This method is used for debugging.
     *
     * @param data The RAW bytes.
     * @return The hexadecimal string.
     */
    public static String dumpHex(byte[] data, int offset, int length) {
        char[] hex = new char[2*length];
        for (int i = 0; i < length; ++i) {
            final int b = data[offset+i] & 0xFF;
            hex[2*i + 0] = HEX[b >>> 4];
            hex[2*i + 1] = HEX[b & 0x0F];
        }

        return new String(hex);
    }


    public static String dumpHex(byte[] data) {
        return data != null ? dumpHex(data, 0, data.length) : "null";
    }


    /**
     * Returns the integer as hexadecimal string.
     *
     * @param val The integer value.
     * @param width The number of characters to dump.
     * @return The hexadecimal string.
     */
    public static String dumpHex(long val, int width) {
        char[] hex = new char[width];
        for (int i = 0; i < width; ++i) {
            final long b = (val >>> ((width-1-i)*4));
            hex[i] = HEX[(int)b & 0x0F];
        }

        return new String(hex);
    }


    public static String randomUUID() {
        Random rnd = new Random();
        StringBuffer sb = new StringBuffer(2 * 16 + 4);

        sb.append(dumpHex(rnd.nextInt(), 8));
        sb.append('-');

        sb.append(dumpHex(rnd.nextInt(), 4));
        sb.append('-');

        sb.append(dumpHex(rnd.nextInt(), 4));
        sb.append('-');

        sb.append(dumpHex(rnd.nextInt(), 4));
        sb.append('-');

        sb.append(dumpHex(rnd.nextLong(), 12));
        return sb.toString().toLowerCase();
    }


    public static String formatUUID(byte[] data) throws Exception {
        if (data.length != 16) {
            throw new Exception("invalid UUID length");
        }
        StringBuffer sb = new StringBuffer(2 * 16 + 4);

        sb.append(dumpHex(data, 0, 4));
        sb.append('-');

        sb.append(dumpHex(data, 4, 2));
        sb.append('-');

        sb.append(dumpHex(data, 6, 2));
        sb.append('-');

        sb.append(dumpHex(data, 8, 2));
        sb.append('-');

        sb.append(dumpHex(data, 10, 6));
        return sb.toString().toLowerCase();
    }


    /// Parse the MAC address.
    public static long parseHexMAC(String val) throws Exception {
        long res = 0;
        int n = 0;

        for (int i = 0; i < val.length(); ++i) {
            final int d = Character.digit(val.charAt(i), 16);
            if (d >= 0) {
                res <<= 4;
                res |= d;
                n += 1;
            }
        }

        if (n != 16) {
            throw new IllegalArgumentException("invalid MAC address");
        }

        return res;
    }

    /// Parse hexadecimal string.
    public static byte[] parseHexString(String val) throws Exception {
        ByteArrayOutputStream bs = new ByteArrayOutputStream();

        boolean got_hi = false;
        int hi = 0;

        for (int i = 0; i < val.length(); ++i) {
            final int b = Character.digit(val.charAt(i), 16);
            if (b >= 0) {
                if (!got_hi) {
                    hi = b;
                    got_hi = true;
                } else {
                    bs.write((hi << 4) | b);
                    got_hi = false;
                }
            }
        }

        if (got_hi) {
            throw new IllegalArgumentException("invalid hexadecimal string");
        }

        return bs.toByteArray();
    }
}
