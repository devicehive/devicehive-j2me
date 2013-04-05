package com.dataart.devicehive;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

/**
 * binary service - reads/writes frames from/to COM port.
 */
public class BinaryService implements Runnable {

    /**
     * Binary Service Listener interface.
     * Should handle received frames.
     */
    public static interface Listener {
        public void binaryFrameReceived(BinFrame frame);
    }

    private boolean isRun = false;
    private Thread thread = null;
    private final Vector framesToSend = new Vector();
    private Listener listener;
    private StreamConnection stream;

    public BinaryService(Listener listener) {
        this.listener = listener;
    }

    // TODO: add "FAKE RX frames" feature
    public void sendFrame(BinFrame frame) {
        synchronized (framesToSend) {
            framesToSend.addElement(frame);
        }
    }

    public void start() {
        isRun = true;
        thread = new Thread(this);
        thread.start();
    }

    public void stop() {
        isRun = false;
        // TODO: join thread?
        thread = null;
    }

    public void run() {
        while (isRun) {
            try {
                stream = (StreamConnection)Connector.open(Settings.SERIAL_PORT_NAME);
                OutputStream os = stream.openOutputStream();
                InputStream is = stream.openInputStream();
                byte[] rxParseBuf = new byte[0];

                while (isRun) {
                    BinFrame txFrame = null;
                    synchronized (framesToSend) {
                        if (!framesToSend.isEmpty()) {
                            txFrame = (BinFrame) framesToSend.firstElement();
                            framesToSend.removeElementAt(0);
                        }
                    }

                    byte[] txBuf = txFrame != null ? txFrame.format() : null;
                    if (txFrame != null && txBuf != null) {
                        DebugUtils.logSpi("sending binary frame: <" + txFrame
                            + ">: " + DataUtils.dumpHex(txBuf));
                        os.write(txBuf);
                    }

                    int rxLen = is.available();
                    if (rxLen > 0) {
                        byte[] rxBuf = new byte[rxLen];
                        rxLen = is.read(rxBuf, 0, rxBuf.length);

                        if (rxLen > 0) {
                            // append received data to the 'rxParsedBuf'
                            byte[] newBuf = new byte[rxParseBuf.length + rxLen];
                            System.arraycopy(rxParseBuf, 0, newBuf, 0, rxParseBuf.length);
                            System.arraycopy(rxBuf, 0, newBuf, rxParseBuf.length, rxLen);
                            rxParseBuf = newBuf;

                            DebugUtils.logSpi("binary data received: " + DataUtils.dumpHex(rxBuf, 0, rxLen));

                            // try to parse as many frames as possible
                            while (true) {
                                BinFrame.ParseResult res = BinFrame.parseFrame(rxParseBuf);
                                rxParseBuf = res.data;
                                // TODO: handle res.result

                                if (res.frame != null) {
                                    //DebugUtils.logSpi("binary frame received: <" + res.frame + ">");
                                    listener.binaryFrameReceived(res.frame);
                                } else {
                                    break;
                                }
                            }
                        }
                    }

                    // if no activity (TX or RX) then sleep a while
                    if (txFrame == null && rxLen == 0) {
                        try {
                            Thread.sleep(100); // TODO: latency constant?
                        } catch (Exception ex) {
                            //
                        }
                    }
                }
            } catch (Exception ex) {
                DebugUtils.logSpi("binary service error: " + ex.getMessage());
                DebugUtils.diagnosticLogError("binservice: ", ex);
            }
        }

        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ex) {
                //
            }
            stream = null;
        }
    }
}
