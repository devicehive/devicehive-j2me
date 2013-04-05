package com.dataart.devicehive;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;
/**
 * This class based on the work Christoph Hartman
*/
public class RestClient {

    String userid;
    String password;
    // User Agent
    private String ua;

    public RestClient() {
    }

    public RestClient(String userid, String password) {
        this.userid = userid;
        this.password = password;
        ua = "Profile/" + System.getProperty("microedition.profiles")
                + " Configuration/"
                + System.getProperty("microedition.configuration");
    }

    private void configureConnection(HttpConnection conn, DeviceData dev) throws IOException {
        conn.setRequestProperty("User-Agent", ua);
        String locale = System.getProperty("microedition.locale");
        if (locale == null) {
            locale = "en-US";
        }
        conn.setRequestProperty("Accept-Language", locale);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");
        if (dev != null) {
            conn.setRequestProperty("Auth-DeviceID", dev.id);
            conn.setRequestProperty("Auth-DeviceKey", dev.key);
        }

        // set HTTP basic authentification
        if (true && userid != null && password != null) {
            conn.setRequestProperty("Authorization", "Basic " + BasicAuth.encode(userid, password));
        }
    }

    public HttpConnection getConnection(String url, DeviceData dev) throws IOException {
        HttpConnection conn = (HttpConnection) Connector.open(url);
        configureConnection(conn, dev);
        return conn;
    }

    public HttpConnection getConnection(String url, int access, DeviceData dev) throws IOException {
        HttpConnection conn = (HttpConnection) Connector.open(url, access);
        configureConnection(conn, dev);
        return conn;
    }

    /**
     * READ
     *
     * @param url
     * @return
     * @throws IOException
     */
    public String get(String url, DeviceData dev) throws IOException {

        DebugUtils.log("get: " + url);
        HttpConnection hcon = null;
        DataInputStream dis = null;
        StringBuffer responseMessage = new StringBuffer();

        try {
            int redirectTimes = 0;
            boolean redirect;

            do {
                redirect = false;

                // a standard HttpConnection with READ access
                hcon = getConnection(url, dev);
                // obtain a DataInputStream from the HttpConnection
                dis = new DataInputStream(hcon.openInputStream());
                // retrieve the response from the server
                int ch;
                while ((ch = dis.read()) != -1) {
                    responseMessage.append((char) ch);
                }// end while ( ( ch = dis.read() ) != -1 )
                // check status code
                int status = hcon.getResponseCode();

                switch (status) {
                    case HttpConnection.HTTP_OK: // Success!
                        break;
                    case HttpConnection.HTTP_TEMP_REDIRECT:
                    case HttpConnection.HTTP_MOVED_TEMP:
                    case HttpConnection.HTTP_MOVED_PERM:
                        // Redirect: get the new location
                        url = hcon.getHeaderField("location");
                        System.out.println("Redirect: " + url);

                        if (dis != null) {
                            dis.close();
                        }
                        if (hcon != null) {
                            hcon.close();
                        }

                        hcon = null;
                        redirectTimes++;
                        redirect = true;
                        break;
                    default:
                        // Error: throw exception
                        hcon.close();
                        throw new IOException("Response status not OK:" + status);
                }

                // max 5 redirects
            } while (redirect == true && redirectTimes < 5);

            if (redirectTimes == 5) {
                throw new IOException("Too much redirects");
            }

        } catch (Exception e) {

            // TODO bad style
            responseMessage.append("ERROR: ");
        } finally {
            try {
                if (hcon != null) {
                    hcon.close();
                }
                if (dis != null) {
                    dis.close();
                }
            } catch (IOException ioe) {
            }// end try/catch
        }// end try/catch/finally
        return responseMessage.toString();
    }// end sendGetRequest( String )
    /**
     * UPDATE
     *
     * @param url
     * @param data the request body
     * @throws IOException
     */
    final int METHOD_POST = 0;
    final int METHOD_PUT = 1;
    final int METHOD_DELETE = 2;

    public String post(String url, String data, DeviceData dev) throws IOException {
        return post(url, data, METHOD_POST, dev);
    }

    public String post(String url, String data, int method, DeviceData dev) throws IOException {

        HttpConnection hcon = null;
        DataInputStream dis = null;
        DataOutputStream dos = null;
        StringBuffer responseMessage = new StringBuffer();

        try {
            int redirectTimes = 0;
            boolean redirect;

            do {
                redirect = false;
                // an HttpConnection with both read and write access
                hcon = getConnection(url, Connector.READ_WRITE, dev);
                // set the request method to POST
                hcon.setRequestMethod(HttpConnection.POST);
                // overwrite content type to be form based
                //hcon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");


                //hcon.setRequestProperty("Content-Type", "application/json");

                if (method == METHOD_PUT) {
                    hcon.setRequestProperty("X-HTTP-Method-Override", "PUT");
                }

                // set message length
                if (data != null) {
                    hcon.setRequestProperty("Content-Length", "" + data.length());
                }

                if (data != null) {

                    DebugUtils.log("put data: " + data);
                    // obtain DataOutputStream for sending the request string
                    dos = hcon.openDataOutputStream();
                    byte[] request_body = data.getBytes();
                    // send request string to server
                    for (int i = 0; i < request_body.length; i++) {
                        dos.writeByte(request_body[i]);
                    }// end for( int i = 0; i < request_body.length; i++ )
                    dos.flush(); // Including this line may produce
                    // undesiredresults on certain devices
                }

                // obtain DataInputStream for receiving server response
                dis = new DataInputStream(hcon.openInputStream());
                // retrieve the response from server
                int ch;
                while ((ch = dis.read()) != -1) {
                    responseMessage.append((char) ch);
                }// end while( ( ch = dis.read() ) != -1 ) {
                // check status code
                int status = hcon.getResponseCode();
                switch (status) {
                    case HttpConnection.HTTP_OK: // Success!
                    case HttpConnection.HTTP_CREATED:
                        break;
                    case HttpConnection.HTTP_TEMP_REDIRECT:
                    case HttpConnection.HTTP_MOVED_TEMP:
                    case HttpConnection.HTTP_MOVED_PERM:
                        // Redirect: get the new location
                        url = hcon.getHeaderField("location");
                        System.out.println("Redirect: " + url);

                        if (dis != null) {
                            dis.close();
                        }
                        if (hcon != null) {
                            hcon.close();
                        }
                        hcon = null;
                        redirectTimes++;
                        redirect = true;
                        break;
                    default:
                        // Error: throw exception
                        hcon.close();
                        throw new IOException("Response status not OK:" + status);
                }

                // max 5 redirects
            } while (redirect == true && redirectTimes < 5);

            if (redirectTimes == 5) {
                throw new IOException("Too much redirects");
            }
        } catch (Exception e) {

            responseMessage.append("ERROR");
        } finally {
            // free up i/o streams and http connection
            try {
                if (hcon != null) {
                    hcon.close();
                }
                if (dis != null) {
                    dis.close();
                }
                if (dos != null) {
                    dos.close();
                }
            } catch (IOException ioe) {
            }// end try/catch
        }// end try/catch/finally
        return responseMessage.toString();
    }

    /**
     * CREATE not possible on J2ME therefore we use Rails emulation on PUT and
     * DELETE
     *
     * @param url
     * @param data
     */
    public String put(String url, String data, DeviceData dev) throws IOException {

        return post(url, data, METHOD_PUT, dev);
    }

    public static String insertNotification(DeviceData dev, String notification, Object params) {

        String res = "";

        RestClient rc = new RestClient(null, null);
        try {
            JSONObject body = new JSONObject();
            body.put("notification", notification);
            body.put("parameters", params);
            res = rc.post(Settings.SERVER_URL + "/device/" + dev.id + "/notification",
                    body.toString(), dev);
        } catch (Exception ex) {
        }

        DebugUtils.log("insertNotification: " + res);

        return res;
    }

    public static String registerDevice(DeviceData dev) {
        String res = "";

        RestClient rc = new RestClient(null, null);
        try {
            final String url = Settings.SERVER_URL
                    + "/device/" + dev.id;

            final JSONObject jbody = dev.json();
            res = rc.put(url, jbody.toString(), dev);
        } catch (IOException ex) {
        } catch (JSONException ex) {
        }

        DebugUtils.log("got register Device response: " + res);
        return res;
    }

    /**
     * Poll device commands.
     *
     * @param dev The device to poll commands for.
     * @param timestamp The last command timestamp. May be null.
     * @return The "poll" response.
     */
    public static String pollCommands(DeviceData dev, String timestamp) {
        String res = "";

        RestClient rc = new RestClient(null, null);
        try {
            String url = Settings.SERVER_URL
                    + "/device/" + dev.id
                    + "/command/poll"
                    + (timestamp != null ? "?timestamp=" + timestamp : "");

            res = rc.get(url, dev);
        } catch (IOException ex) {
        }

        return res;
    }

    /**
     * Update command's status and result.
     *
     * @param cmdId The command identifier.
     * @param status The command status.
     * @param result The command result. May be null or String or JSONObject.
     */
    public static void updateCommand(DeviceData dev, long cmdId, String status, Object result) {
        RestClient rc = new RestClient(null, null);
        try {
            String url = Settings.SERVER_URL
                    + "/device/" + dev.id
                    + "/command/" + cmdId;

            JSONObject jbody = new JSONObject();
            jbody.put("status", status);
            jbody.put("result", result);

            rc.put(url, jbody.toString(), dev);
        } catch (IOException ex) {
        } catch (JSONException ex) {
        }
    }

    /**
     * Get the server API information.
     *
     * @return The response string.
     */
    public static String getApiInfo() {
        String res = "";

        RestClient rc = new RestClient(null, null);
        try {
            res = rc.get(Settings.SERVER_URL + "/info", null);
        } catch (IOException ex) {
        }

        return res;
    }
}
