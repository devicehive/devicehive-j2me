package devicehive.arduino;


import com.dataart.devicehive.DebugUtils;

import javax.microedition.midlet.*;

/**
 * Main application.
 */
public class IMlet extends MIDlet {

    private Gateway gateway = null;

    public void startApp() {

        //init logger
        DebugUtils.init();

        DebugUtils.log("startApp()");
        DebugUtils.diagnosticLog("startApp()");

        // c5c301000000000076
        if (gateway == null) {
            gateway = new Gateway();
            gateway.start();
        }
    }

    public void pauseApp() {
        DebugUtils.diagnosticLog("pauseApp()");
        DebugUtils.log("pauseApp()");
    }

    public void destroyApp(boolean unconditional) {
        DebugUtils.diagnosticLog("destroyApp()");
        DebugUtils.log("destroyApp(): " + unconditional);
        if (gateway != null) {
            gateway.stop();
            gateway = null;
        }
    }
}
