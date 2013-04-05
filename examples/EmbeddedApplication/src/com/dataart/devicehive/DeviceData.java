package com.dataart.devicehive;

import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;

/**
 *
 */
public class DeviceData {
    String id;
    String name;
    String key;
    String status;
    JSONObject data;

    DeviceClass deviceClass = new DeviceClass();
    Network network = new Network();
    Equipment[] equipment;

    static class DeviceClass {
        String name;
        String version;
        boolean isPermanent;
        int offlineTimeout;

        JSONObject json() throws JSONException {
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

    static class Network {
        String name;
        String key;
        String description;

        JSONObject json() throws JSONException {
            JSONObject jnet = new JSONObject();
            jnet.put("name", name);
            if (key != null && key.length() > 0) {
                jnet.put("key", key);
            }
            jnet.put("description", description);
            return jnet;
        }
    }

    static class Equipment {
        String code;
        String name;
        String type;

        JSONObject json() throws JSONException {
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
    JSONObject json() throws JSONException {
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

