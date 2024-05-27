package com.opentext.lre.actions.common.helpers.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;

public class LogHelper {

    // Static logger instance
    private static final Logger logger = LogManager.getLogger(LogHelper.class);
    private static boolean stackTraceEnabled = false;

    // Static initializer block to configure logging
    static {
        // Suppress warnings from ResponseProcessCookies
        Logger apacheHttpLogger = LogManager.getLogger("org.apache.http.client.protocol.ResponseProcessCookies");
        if (apacheHttpLogger instanceof org.apache.logging.log4j.core.Logger) {
            ((org.apache.logging.log4j.core.Logger) apacheHttpLogger).setLevel(org.apache.logging.log4j.Level.ERROR);
        }
    }

    // Method to setup log file path
    public static void setup(String logFilePath, boolean enableStackTrace) {
        org.apache.logging.log4j.jul.LogManager.getLogManager().reset();
        java.util.logging.Logger httpClientLogger = java.util.logging.Logger.getLogger("org.apache.http.client.protocol.ResponseProcessCookies");
        httpClientLogger.setLevel(java.util.logging.Level.SEVERE);

        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration config = context.getConfiguration();

        // Create a new file appender with the specified log file path
        PatternLayout layout = PatternLayout.newBuilder()
                .withPattern("%d{yyyy-MM-dd HH:mm:ss} %-5level %logger{36} - %msg%n")
                .build();
        FileAppender appender = FileAppender.newBuilder()
                .withFileName(logFilePath)
                .withName("File")
                .withLayout(layout)
                .build();
        appender.start();

        // Add the appender to the configuration
        config.addAppender(appender);

        // Get the root logger and add the appender
        LoggerConfig rootLoggerConfig = config.getRootLogger();
        rootLoggerConfig.addAppender(appender, null, null);

        // Update the context with the new configuration
        context.updateLoggers(config);

        logger.info("Log file path set to: " + logFilePath);
        stackTraceEnabled = enableStackTrace;
    }

    // Method to log info messages
    public static void info(String message) {
        logger.info(message);
    }

    // Method to log error messages with exception
    public static void error(String message, Throwable throwable) {
        if(stackTraceEnabled) {
            logger.error(message, throwable);
        } else {
            if(throwable != null && !throwable.getMessage().trim().isEmpty()) {
                logger.error(message, throwable.getMessage());
            }
            logger.error(message);
        }
    }

    // Method to log error messages
    public static void error(String message) {
        logger.error(message);
    }

    // Add more methods as needed for different log levels
    public static void debug(String message) {
        logger.debug(message);
    }

    public static void warn(String message) {
        logger.warn(message);
    }

    public static void log(String format, boolean addDate, Object... args) {
        if (logger == null)
            return;
        logger.info(String.format(format, args));
    }

    // Method to log messages with an optional throwable
    public static void logStackTrace(Throwable throwable) {
        if(stackTraceEnabled || (throwable != null && throwable.getMessage().trim().isEmpty())) {
            logger.error("Error - Stack Trace: ", throwable);
        } else if(throwable != null) {
            logger.error(throwable.getMessage());
        }
    }
    public static void logStackTrace(String errorMessage, Throwable throwable) {
        if(stackTraceEnabled || (throwable != null && throwable.getMessage().trim().isEmpty())) {
            logger.error("Error: " + errorMessage + " Stack Trace: ", throwable);
        } else if(throwable != null) {
            logger.error(errorMessage + " - " + throwable.getMessage());
        }
    }
}