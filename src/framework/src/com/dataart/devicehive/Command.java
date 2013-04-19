package com.dataart.devicehive;

import org.json.me.JSONException;
import org.json.me.JSONObject;

/**
 * DeviceHive command wrapper class.
 */
public class Command {
    public long id;            // command identifier
    public String name;        // command name
    public String timestamp;   // command timestamp
    public Object params;      // custom parameters, may be null


    /**
     * Parse the command from an JSON object.
     *
     * @param jcmd The JSON object representing a command.
     * @throws JSONException in case of invalid JSON object format.
     */
    Command(JSONObject jcmd) throws JSONException {
        this.id         = jcmd.getLong("id");
        this.name       = jcmd.getString("command");
        this.timestamp  = jcmd.getString("timestamp");
        this.params     = jcmd.opt("parameters");
    }


    /**
     * Dump the command to string.
     * @return The string dump.
     */
    public String toString() {
        return "command id:" + this.id
             + " name:\"" + this.name + "\""
             + " timestamp:\"" + this.timestamp + "\""
             + " params:" + this.params;
    }
}
