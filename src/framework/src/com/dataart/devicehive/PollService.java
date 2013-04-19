package com.dataart.devicehive;

import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;

/**
 * Polls DeviceHive commands in separate thread.
 */
public class PollService implements Runnable {

    /**
     * Poll Command Listener interface.
     * Should handle received commands.
     */
    public static interface Listener {
        public void deviceHiveCommandReceived(DeviceData device, Command cmd);
    }

    DeviceData device;
    public String lastCommandTimestamp = null;
    Listener listener;
    boolean isRun = false;
    Thread thread;

    public PollService(DeviceData device, Listener listener) {
        this.device = device;
        this.listener = listener;
    }

    public void run() {
        while (isRun) {
            try {
                String msg = RestClient.pollCommands(device, lastCommandTimestamp);
                DebugUtils.log("got poll response: " + msg);
                Command[] commands = fromPollResponse(msg);

                if (listener != null && commands != null) {
                    for (int i = 0; i < commands.length; ++i) {
                        listener.deviceHiveCommandReceived(device, commands[i]);
                        lastCommandTimestamp = commands[i].timestamp;
                    }
                }
            } catch (Exception ex) {

                DebugUtils.diagnosticLog("PollService run cycle  error: " + ex.getMessage());
            }
        }
    }

    public void stop() {
        isRun = false;
        // TODO: join the thread?
    }

    public void start() {
        isRun = true;
        thread = new Thread(this);
        thread.start();
    }

    /**
     * Parse commands from a "poll" response.
     *
     * @param str The "poll" response as a string.
     * @return Array of parsed commands or null.
     */
    static Command[] fromPollResponse(String str) {
        Command[] commands = null;
        try {
            JSONArray json = new JSONArray(str);
            commands = new Command[json.length()];

            for (int i = 0; i < json.length(); ++i) {
                JSONObject jcmd = json.getJSONObject(i);
                commands[i] = new Command(jcmd);
            }
        } catch (JSONException ex) {

            //DebugUtils.diagnosticLogError("PollService fromPollResponse error: ", ex);
        }

        return commands;
    }
}
