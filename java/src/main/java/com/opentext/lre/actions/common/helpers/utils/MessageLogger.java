//package com.opentext.lre.actions.common.helpers.utils;
//
//import java.io.IOException;
//import java.util.logging.FileHandler;
//import java.util.logging.Logger;
//import java.util.logging.SimpleFormatter;
//import java.util.logging.Level;
//
//public class MessageLogger {
//    private static final String LOG_FILE = "message_log.txt";
//    private static Logger logger;
//
//    // Static initializer to set up the logger
//    static {
//        logger = Logger.getLogger("MessageLogger");
//        try {
//            FileHandler fh = new FileHandler(LOG_FILE, true);
//            SimpleFormatter formatter = new SimpleFormatter();
//            fh.setFormatter(formatter);
//            logger.addHandler(fh);
//            logger.setLevel(Level.INFO);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    // Method to log messages
//    public static void log(String message) {
//        logger.info(message);
//    }
//}