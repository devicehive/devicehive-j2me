package com.dataart.devicehive;

import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;

/**
 *
 */
public class DeviceData {
    public String id;
    public String name;
    public String key;
    public String status;
    public JSONObject data;

    public DeviceClass deviceClass = new DeviceClass();
    public Network network = new Network();
    public Equipment[] equipment;

    public static class DeviceClass {
        public String name;
        public String version;
        public boolean isPermanent;
        public int offlineTimeout;

        public JSONObject json() throws JSONException {
            JSONObject jclass = new JSONObject();
            jclass.put("name", name);
            jclass.put("version", version);
            jclass.put("isPermanent", isPermanent);
            if (offlineTimeout > 0) {
                jclass.put("offlineTimeout", offlineTimeout);
            }
            return jclass;
        }
    }

    public static class Network {
        public String name;
        public String key;
        public String description;

        public JSONObject json() throws JSONException {
            JSONObject jnet = new JSONObject();
            jnet.put("name", name);
            if (key != null && key.length() > 0) {
                jnet.put("key", key);
            }
            jnet.put("description", description);
            return jnet;
        }
    }

    public  static class Equipment {
        public String code;
        public String name;
        public String type;

        public JSONObject json() throws JSONException {
            JSONObject jeq = new JSONObject();
            jeq.put("code", code);
            jeq.put("name", name);
            jeq.put("type", type);
            return jeq;
        }
    }

    /**
     * Convert to JSON
     */
    public JSONObject json() throws JSONException {
        JSONObject jdev = new JSONObject();

        if (name != null && name.length() > 0) {
            jdev.put("name", name);
        }

        jdev.put("key", key);
        jdev.put("status", status);

        if (data != null) {
            jdev.put("data", data);
        }

        if (network != null) {
            jdev.put("network", network.json());
        }

        if (deviceClass != null) {
            jdev.put("deviceClass", deviceClass.json());
        }

        if (equipment != null) {
            JSONArray jeq = new JSONArray();
            for (int i = 0; i < equipment.length; ++i) {
                jeq.put(equipment[i].json());
            }
            jdev.put("equipment", jeq);
        }

        return jdev;
    }

}

