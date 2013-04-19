package devicehive.arduino;

import com.dataart.devicehive.BinFrame;
import com.dataart.devicehive.BinaryService;
import com.dataart.devicehive.Command;
import com.dataart.devicehive.DebugUtils;
import com.dataart.devicehive.DeviceData;
import com.dataart.devicehive.ExJSONParser;
import com.dataart.devicehive.PollService;
import com.dataart.devicehive.PostService;
import com.dataart.devicehive.RemoteDevice;
import com.dataart.devicehive.RestClient;
import com.dataart.devicehive.Settings;
import com.dataart.io.bin.DataInputStream;
import com.dataart.io.bin.DataOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Vector;
import org.json.me.JSONException;
import org.json.me.JSONObject;

/**
 *
 */
public class Gateway implements Runnable, PollService.Listener, BinaryService.Listener {

    private PollService pollService = null;
    private PostService postService = null;
    private BinaryService binaryService = null;
    private Vector receivedGatewayCommands = new Vector();
    private Vector receivedBinaryFrames = new Vector();
    private DeviceData gatewayDevice;
    private RemoteDevice remoteDevice = new RemoteDevice();
    private String serverTimestamp;

    private void initDevicehive() {
        //setup playground
        Settings.SERVER_URL = GatewaySettings.serverUrl;
        //SERIAL_PORT_NAME
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
    }
    boolean isRun;
    Thread thread;

    public void run() {

        try {
            initDevicehive();

            // send registration request
            binaryService.sendFrame(new BinFrame(BinFrame.INTENT_REGISTRATION_REQUEST, new byte[0]));

            while (isRun) {
                try {

                    BinFrame rxFrame = null;
                    synchronized (this) {
                        if (!receivedBinaryFrames.isEmpty()) {
                            rxFrame = (BinFrame) receivedBinaryFrames.firstElement();
                            receivedBinaryFrames.removeElementAt(0);
                        }
                    }

                    Command rxCmd = null;
                    synchronized (this) {
                        if (!receivedGatewayCommands.isEmpty()) {
                            rxCmd = (Command) receivedGatewayCommands.firstElement();
                            receivedGatewayCommands.removeElementAt(0);
                        }
                    }

                    if (rxFrame != null) {
                        try {
                            handleBinaryFrame(rxFrame);
                        } catch (Exception ex) {
                            DebugUtils.diagnosticLogError("IOEGateway handleRapidSEFrame error: ", ex);
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
                    if (rxFrame == null && rxCmd == null) {
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
        if (binaryService == null) {
            binaryService = new BinaryService(this);
            binaryService.start();
        }

        isRun = true;
        thread = new Thread(this);
        thread.start();
    }

    public void stop() {
        if (binaryService != null) {
            binaryService.stop();
            binaryService = null;
        }
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

        final int intent = remoteDevice.findIntentByName(cmd.name);
        if (0 <= intent) {
            JSONObject data = new JSONObject();
            data.put("id", new Long(cmd.id));
            data.put("parameters", cmd.params);

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(os);
            RemoteDevice.Layout layout = remoteDevice.findLayoutByIntent(intent);
            RemoteDevice.json2bin(data, dos, layout);

            BinFrame frame = new BinFrame(intent, os.toByteArray());
            binaryService.sendFrame(frame);
        } else {
            throw new Exception("unknown command, ignored");
        }
    }

    private void handleBinaryFrame(BinFrame frame) throws Exception {
        DebugUtils.log("handle binary frame: <" + frame + ">");

        RemoteDevice.Layout layout = remoteDevice.findLayoutByIntent(frame.intent);
        if (layout != null) {
            ByteArrayInputStream is = new ByteArrayInputStream(frame.payload);
            DataInputStream dis = new DataInputStream(is);
            Object jval = RemoteDevice.bin2json(dis, layout);

            switch (frame.intent) {
                case BinFrame.INTENT_REGISTRATION_RESPONSE_JSON:
                    handleRegistrationResponse(((JSONObject) jval).getString("json"));
                    break;

                case BinFrame.INTENT_COMMAND_RESULT: {
                    JSONObject jj = (JSONObject) jval;
                    final long cmdId = jj.getLong("id");
                    final String status = jj.getString("status");
                    final String result = jj.getString("result");
                    postService.updateCommand(cmdId, status, result);
                    //RestClient.updateCommand(gatewayDevice, cmdId, status, result);
                }
                break;

                default: {
                    postService.insertNotification(layout.name, ((JSONObject) jval).get("parameters"));
                    //RestClient.insertNotification(gatewayDevice, layout.name, ((JSONObject)jval).get("parameters"));
                }
                break;
            }
        } else {
            DebugUtils.log("unknown intent:" + frame.intent);
        }
    }

    private void handleRegistrationResponse(String info) throws Exception {
        DebugUtils.log("registration data: " + info);
        JSONObject jval = (JSONObject) (new ExJSONParser(info).parse());
        remoteDevice.handleRegisterResponse(jval);

        gatewayDevice = new DeviceData();
        gatewayDevice.id = jval.getString("id");
        gatewayDevice.name = jval.getString("name");
        gatewayDevice.key = jval.getString("key");
        gatewayDevice.status = "Online";
        gatewayDevice.network.name = GatewaySettings.networkName;
        gatewayDevice.network.description = GatewaySettings.networkDesc;
        gatewayDevice.deviceClass.name = jval.getJSONObject("deviceClass").getString("name");
        gatewayDevice.deviceClass.version = jval.getJSONObject("deviceClass").getString("version");
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

    /**
     * Note: this method is called from another thread!
     *
     * @param frame The received RapidSE frame.
     */
    public void binaryFrameReceived(BinFrame frame) {
        synchronized (this) {
            receivedBinaryFrames.addElement(frame);
        }
    }
}
