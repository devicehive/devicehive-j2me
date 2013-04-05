package com.dataart.io.bin;

import java.io.IOException;

public class DataInputStream extends java.io.DataInputStream {

    public DataInputStream(java.io.InputStream stream) {
        super(stream);
    }

    public String readBinString() throws IOException {
        final int len = readUInt16LE();
        byte[] buf = new byte[len];
        if (len > 0) {
            this.readFully(buf);
        }
        return new String(buf);
    }

    public long readUInt64LE() throws IOException {
        final int b0 = this.read() & 0xff;
        final int b1 = this.read() & 0xff;
        final int b2 = this.read() & 0xff;
        final long b3 = this.read() & 0xff;
        final int b4 = this.read() & 0xff;
        final int b5 = this.read() & 0xff;
        final int b6 = this.read() & 0xff;
        final long b7 = this.read() & 0xff;

        final long hi = (b7<<24) | ((b6<<16) | (b5<<8) | b4);
        final long lo = (b3<<24) | ((b2<<16) | (b1<<8) | b0);
        return (hi << 32) | lo;
    }

    public long readUInt32LE() throws IOException {
        final int b0 = this.read() & 0xff;
        final int b1 = this.read() & 0xff;
        final int b2 = this.read() & 0xff;
        final long b3 = this.read() & 0xff;

        return (b3 << 24) | ((b2 << 16) | (b1 << 8) | b0);
    }

    public int readUInt16LE() throws IOException {
        final int b0 = this.read() & 0xff;
        final int b1 = this.read() & 0xff;

        return (b1 << 8) | b0;
    }

    public int readUInt8() throws IOException {
        return this.readUnsignedByte();
    }

    public long readInt64LE() throws IOException {
        return readUInt64LE();
    }

    public long readInt32LE() throws IOException {
        return (int)readUInt32LE();
    }

    public int readInt16LE() throws IOException {
        return (short)readUInt16LE();
    }

    public int readInt8() throws IOException {
        return this.readByte();
    }
}
