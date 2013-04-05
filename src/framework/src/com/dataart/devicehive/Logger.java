package com.dataart.devicehive;

import java.io.OutputStream;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

public class Logger implements Runnable {

    private Thread thread;
    private boolean isRun;
    private final String url;
    private Vector msgBuf;

    private boolean newConnectionPerMessage = true;
    private StreamConnection stream;
    private OutputStream output;

    public Logger(String url, boolean newConnectionPerMessage) {
        this.newConnectionPerMessage = newConnectionPerMessage;
        this.msgBuf = new Vector();
        this.url = url;
    }


    public void log(String message) {
        synchronized (this) {
            msgBuf.addElement(message);
        }
    }


    public void run() {
        while (isRun) {
            try {
                String msg = null;
                synchronized (this) {
                    if (!msgBuf.isEmpty()) {
                        msg = (String)msgBuf.elementAt(0);
                        msgBuf.removeElementAt(0);
                    }
                }

                if (msg != null) {
                    try {
                        if (stream == null || output == null) {
                            stream = (StreamConnection)Connector.open(this.url, Connector.WRITE);
                            output = stream.openOutputStream();
                        }

                        output.write((msg + "\n").getBytes());
                        //? output.flush();

                        if (newConnectionPerMessage) {
                            output.close();
                            stream.close();
                            output = null;
                            stream = null;
                        }
                    } catch (Exception ex) {
                        System.err.println(msg); // use stderr as fallback
                        
                        output = null;
                        stream = null;
                    }
                } else {
                    // nothing to do
                    Thread.sleep(250);
                }
            } catch (Exception ex) {
               
            }
        }
    }


    public void start() {
        isRun = true;
        thread = new Thread(this);
        thread.start();
    }


    public void stop() {
        isRun = false;
        // TODO: join the thread?
    }
}
