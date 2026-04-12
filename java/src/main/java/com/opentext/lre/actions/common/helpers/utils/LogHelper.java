package com.opentext.lre.actions.common.helpers.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class LogHelper {

    public static Logger logger;
    private static boolean stackTraceEnabled = false;

    private LogHelper() {
        // Utility class
    }

    private static void fallbackError(String message) {
        System.err.println(message);
    }

    private static void fallbackError(String message, Throwable throwable) {
        fallbackError(message);
        if (throwable != null) {
            throwable.printStackTrace(System.err);
        }
    }

    public static synchronized void setup(String logFilePath, boolean enableStackTrace) throws Exception {
        stackTraceEnabled = enableStackTrace;

        // Ensure directory exists
        File file = new File(logFilePath);
        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new Exception("Failed to create log directory: " + parent.getAbsolutePath());
        }

        // The log.file system property is already set in Main.java before calling this method
        // log4j2.xml uses ${sys:log.file} to configure the file appender
        // Simply get the logger - log4j2 will initialize with the system property
        logger = LogManager.getLogger(LogHelper.class);

        logger.info("Log file path set to: {}", logFilePath);
    }

    public static void log(String message, boolean addDate, Object... args) {
        String formatted = String.format(message, args);
        if (addDate) {
            // Timestamps are already controlled by the logging backend pattern.
        }
        if (logger != null) {
            logger.info(formatted);
        } else {
            System.out.println(formatted);
        }
    }

    public static void error(String message) {
        if (logger != null) {
            logger.error(message);
        } else {
            fallbackError(message);
        }
    }

    public static void error(String message, Throwable throwable) {
        if (logger == null) {
            if (stackTraceEnabled && throwable != null) {
                fallbackError(message, throwable);
            } else if (throwable != null && throwable.getMessage() != null) {
                fallbackError(message + " - " + throwable.getMessage());
            } else {
                fallbackError(message);
            }
            return;
        }

        if (stackTraceEnabled && throwable != null) {
            logger.error(message, throwable);
        } else if (throwable != null && throwable.getMessage() != null) {
            logger.error("{} - {}", message, throwable.getMessage());
        } else {
            logger.error(message);
        }
    }

    public static void logStackTrace(Throwable throwable) {
        if (logger == null) {
            if (stackTraceEnabled || (throwable != null && throwable.getMessage() == null)) {
                fallbackError("Error - Stack Trace:", throwable);
            } else if (throwable != null) {
                fallbackError(throwable.getMessage());
            }
            return;
        }

        if (stackTraceEnabled || (throwable != null && throwable.getMessage() == null)) {
            logger.error("Error - Stack Trace: ", throwable);
        } else if (throwable != null) {
            logger.error(throwable.getMessage());
        }
    }
    public static void logStackTrace(String errorMessage, Throwable throwable) {
        boolean missingThrowableMessage = throwable != null && (throwable.getMessage() == null || throwable.getMessage().trim().isEmpty());

        if (logger == null) {
            if(stackTraceEnabled || missingThrowableMessage) {
                fallbackError("Error: " + errorMessage + " Stack Trace:", throwable);
            } else if(throwable != null) {
                fallbackError(errorMessage + " - " + throwable.getMessage());
            } else {
                fallbackError(errorMessage);
            }
            return;
        }

        if(stackTraceEnabled || missingThrowableMessage) {
            logger.error("Error: {} Stack Trace: ", errorMessage, throwable);
        } else if(throwable != null) {
            logger.error("{} - {}", errorMessage, throwable.getMessage());
        } else {
            logger.error(errorMessage);
        }
    }
}
