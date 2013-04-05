package com.dataart.devicehive;


/**
 *
 */
public class DebugUtils {
    private static Logger logger = null;

    static long loggerStartTime;
    static boolean isLogTime = false;
    
    
    public static void init() {
        loggerStartTime = System.currentTimeMillis();
        
        if (Settings.LOG_USE_REMOTE_LOGGER) {
            // NOTE: it's very expensive to make separate connection per each log message
            // unfortunatelly JavaME on our board doesn't send the second message for the same connection
            // TODO: need to ensure this is true!!!
            logger = new Logger(Settings.LOG_REMOTE_URL, true);
            logger.start();
            logger.log("socket logger initialized");
        }
    }

    private static void logImpl(String s) {
        if (Settings.LOG_USE_REMOTE_LOGGER && logger != null) {
            logger.log(s);
        }

        if (!Settings.LOG_USE_REMOTE_LOGGER || logger == null) {
            System.out.println(s);
        }
    }

    public static void log(String s) {
        if (Settings.LOG_ENABLED) {
            logImpl(s);
        }
    }

    public static void logSpi(String s) {
        if (Settings.LOG_SPI_ENABLED) {
            logImpl(s);
        }
    }
    
    public static void diagnosticLog(String s) {
        if (Settings.DIAGNOSTIC_LOG_ENABLED) {
            logImpl("IOE diagnostic: " + s);
        }
    }
    
    public static void diagnosticLogError(String s, Throwable thr) {
        if (Settings.DIAGNOSTIC_LOG_ENABLED ) {
            if(isLogTime) {
                logImpl("IOE diagnostic exception: " + s + " error: " + thr.getMessage());
            }
            else {
                if( System.currentTimeMillis() - loggerStartTime > Settings.LOG_RUN_DEALY) {
                    isLogTime = true;
                }
            }
        }
    }
    
}
