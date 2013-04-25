package com.dataart.devicehive;

/**
 * Application global settings.
 */
public class Settings {

    public static String SERIAL_PORT_NAME = "socket://localhost:10101";
    public static final int RX_BUF_SIZE = 1024;

    public static String SERVER_URL = null;
    
    public static final boolean DIAGNOSTIC_LOG_ENABLED = true;
    public static final boolean LOG_ENABLED = true;
    public static final boolean LOG_SPI_ENABLED = true;
    public static final boolean LOG_USE_REMOTE_LOGGER = false;
    static String LOG_REMOTE_URL = "socket://localhost:55238";

    public static final int LOG_RUN_DEALY = 1000 * 60 * 1;
    //public static final String LOG_REMOTE_URL = "socket://127.0.0.1:20202";
}
