package com.opentext.lre.actions.common.helpers.utils;

import junit.framework.TestCase;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class LogHelperTest extends TestCase {

    public void testLoggerWritesToFile() throws Exception {
        // Create a temporary log file
        Path tempDir = Files.createTempDirectory("log-test");
        String logFilePath = tempDir.resolve("test.log").toString();

        // Set system property before setup
        System.setProperty("log.file", logFilePath);

        // Setup logger
        LogHelper.setup(logFilePath, false);

        // Write some logs
        LogHelper.log("Test message 1", true);
        LogHelper.log("Test message 2 with arg: %s", true, "value");
        LogHelper.error("Test error");

        // Give logger time to flush
        Thread.sleep(100);

        // Verify file exists and has content
        File logFile = new File(logFilePath);
        assertTrue("Log file should exist", logFile.exists());
        assertTrue("Log file should not be empty", logFile.length() > 0);

        // Read and verify content
        List<String> lines = Files.readAllLines(logFile.toPath());
        assertTrue("Log file should contain lines", lines.size() > 0);

        boolean foundMessage1 = false;
        boolean foundMessage2 = false;
        boolean foundError = false;

        for (String line : lines) {
            if (line.contains("Test message 1")) foundMessage1 = true;
            if (line.contains("Test message 2 with arg: value")) foundMessage2 = true;
            if (line.contains("Test error")) foundError = true;
        }

        assertTrue("Should find test message 1", foundMessage1);
        assertTrue("Should find test message 2", foundMessage2);
        assertTrue("Should find test error", foundError);

        // Cleanup
        logFile.delete();
        tempDir.toFile().delete();
    }
}

