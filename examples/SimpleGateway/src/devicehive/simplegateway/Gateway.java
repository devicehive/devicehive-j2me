package devicehive.simplegateway;

//import com.dataart.devicehive.BinFrame;
//import com.dataart.devicehive.BinaryService;
import com.dataart.devicehive.Command;
import com.dataart.devicehive.DebugUtils;
import com.dataart.devicehive.DeviceData;
import com.dataart.devicehive.PollService;
import com.dataart.devicehive.PostService;
import com.dataart.devicehive.RestClient;
import com.dataart.devicehive.Settings;
import java.util.Vector;
import org.json.me.JSONException;
import org.json.me.JSONObject;

/**
 *
 */
public class Gateway implements Runnable, PollService.Listener {

    private PollService pollService = null;
    private PostService postService = null;
    //private BinaryService binaryService = null;
    private Vector receivedGatewayCommands = new Vector();
    
    private DeviceData gatewayDevice;
    
    private String serverTimestamp;

    private void initDevicehive() {

        Settings.SERVER_URL = GatewaySettings.serverUrl;
        // get the server information (at least timestamp)
        serverTimestamp = null;
        final String apiInfo = RestClient.getApiInfo(); // TODO: in separate thread!!!
        DebugUtils.diagnosticLog("init devicehive");
        DebugUtils.log("got server API info response: " + apiInfo);
        try {
            JSONObject json = new JSONObject(apiInfo);
            serverTimestamp = json.optString("serverTimestamp");
            // TODO: check other server API properties...
        } catch (JSONException ex) {
            DebugUtils.diagnosticLogError("init devicehive exception: ", ex);
        }
        
        DebugUtils.log("registering device");
        
        gatewayDevice = new DeviceData();
        gatewayDevice.id = GatewaySettings.DEVICE_ID;
        gatewayDevice.name = GatewaySettings.DEVICE_NAME;
        gatewayDevice.key = GatewaySettings.DEVICE_KEY;
        gatewayDevice.status = "Online";
        gatewayDevice.network.name = GatewaySettings.networkName;
        gatewayDevice.network.description = GatewaySettings.networkDesc;
        gatewayDevice.deviceClass.name = GatewaySettings.DEVICE_CLASS_NAME;
        gatewayDevice.deviceClass.version = GatewaySettings.DEVICE_CLASS_VERSION;
        String rresp = RestClient.registerDevice(gatewayDevice);

        if (postService == null) {
            postService = new PostService(gatewayDevice);
            postService.start();
        }

        if (pollService == null) {
            pollService = new PollService(gatewayDevice, this);
            pollService.lastCommandTimestamp = serverTimestamp;
            pollService.start();
        }
    }
    boolean isRun;
    Thread thread;

    public void run() {

        try {
            initDevicehive();

            while (isRun) {
                try {

            

                    Command rxCmd = null;
                    synchronized (this) {
                        if (!receivedGatewayCommands.isEmpty()) {
                            rxCmd = (Command) receivedGatewayCommands.firstElement();
                            receivedGatewayCommands.removeElementAt(0);
                        }
                    }

                
                    if (rxCmd != null) {
                        try {
                            handleGatewayCommand(rxCmd);
                        } catch (Exception ex) {
                            DebugUtils.diagnosticLogError("IOEGateway habdleGatewayCommand  error: ", ex);
                        }
                    }

                    // if no activities then sleep a while
                    if (rxCmd == null) {
                        Thread.sleep(100);
                    }
                } catch (Throwable ex) {
                    DebugUtils.diagnosticLogError("IOEGateway run cycle error: ", ex);
                }
            }
        } catch (Throwable ex) {
            DebugUtils.diagnosticLogError("IOEGateway run error: ", ex);
        }
    }

    public void start() {
        
        isRun = true;
        thread = new Thread(this);
        thread.start();
    }

    public void stop() {
        
        if (pollService != null) {
            pollService.stop();
            pollService = null;
        }

        isRun = false;
        // TODO: join the thread?
        thread = null;
    }

    private void handleGatewayCommand(Command cmd) throws Exception {
        DebugUtils.log("handle gateway command: <" + cmd + ">");

        if(GatewaySettings.COMMAND_ECHO.equalsIgnoreCase(cmd.name)) {
            postService.updateCommand(cmd.id, "done", null);
            
        }
        else {
            postService.insertNotification(serverTimestamp, "unkown command received");
        }
       
        //throw new Exception("unknown command, ignored");
        
    }
    

    /**
     * Note: this method is called from another thread!
     *
     * @param cmd The received DeviceHive command.
     */
    public void deviceHiveCommandReceived(DeviceData dev, Command cmd) {
        if (dev == gatewayDevice) {
            synchronized (this) {
                receivedGatewayCommands.addElement(cmd);
            }
        }
    }

   
}
