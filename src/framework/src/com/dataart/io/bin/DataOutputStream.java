package com.dataart.io.bin;

import java.io.IOException;

public class DataOutputStream extends java.io.DataOutputStream {

    public DataOutputStream(java.io.OutputStream stream) {
        super(stream);
    }

    public void writeBinString(String val) throws IOException {
        byte[] buf = val.getBytes();
        writeUInt16LE(buf.length);
        this.write(buf);
    }

    public void writeUInt64LE(long val) throws IOException {
        this.write(0xff & (int)val);
        this.write(0xff & (int)(val >> 8));
        this.write(0xff & (int)(val >> 16));
        this.write(0xff & (int)(val >> 24));
        this.write(0xff & (int)(val >> 32));
        this.write(0xff & (int)(val >> 40));
        this.write(0xff & (int)(val >> 48));
        this.write(0xff & (int)(val >> 56));
    }

    public void writeUInt32LE(long val) throws IOException {
        this.write(0xff & (int)val);
        this.write(0xff & (int)(val >> 8));
        this.write(0xff & (int)(val >> 16));
        this.write(0xff & (int)(val >> 24));

    }

    public void writeUInt16LE(int val) throws IOException {
        this.write(0xff & val);
        this.write(0xff & (val >> 8));
    }

    public void writeUInt8(int val) throws IOException {
        this.writeByte(val);
    }

    public void writeInt64LE(long val) throws IOException {
        this.writeUInt64LE(val);
    }

    public void writeInt32LE(long val) throws IOException {
        this.writeUInt32LE(val);
    }

    public void writeInt16LE(int val) throws IOException {
        this.writeUInt16LE(val);
    }

    public void writeInt8(int val) throws IOException {
        this.writeByte(val);
    }
}
