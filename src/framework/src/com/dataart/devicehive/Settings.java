package com.dataart.devicehive;

/**
 * Application global settings.
 */
public class Settings {

    public static final String SERIAL_PORT_NAME = "socket://localhost:10101";
    public static final int RX_BUF_SIZE = 1024;

    public static final String SERVER_URL = "http://ecloud.dataart.com/ecapi8";
    public static final String networkName = "IOE";
    public static final String networkDesc = "IOE test network";

    public static final boolean DIAGNOSTIC_LOG_ENABLED = true;
    public static final boolean LOG_ENABLED = true;
    public static final boolean LOG_SPI_ENABLED = true;
    public static final boolean LOG_USE_REMOTE_LOGGER = false;
    static String LOG_REMOTE_URL = "socket://10.10.1.122:55238";

    public static final int LOG_RUN_DEALY = 1000 * 60 * 1;
    //public static final String LOG_REMOTE_URL = "socket://127.0.0.1:20202";
}
