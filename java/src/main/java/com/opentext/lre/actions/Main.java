package com.opentext.lre.actions;

import com.opentext.lre.actions.common.helpers.InputRetriever;
import com.opentext.lre.actions.common.helpers.LocalizationManager;
import com.opentext.lre.actions.common.helpers.utils.DateFormatter;
import com.opentext.lre.actions.common.helpers.utils.LogHelper;
import com.opentext.lre.actions.common.helpers.utils.Result;
import com.opentext.lre.actions.runtest.LreTestRunBuilder;
import com.opentext.lre.actions.runtest.LreTestRunModel;
import com.opentext.lre.actions.workspacesync.LreWorkspaceSyncModel;
import com.opentext.lre.actions.workspacesync.LreWorkspaceSyncTask;
import org.apache.logging.log4j.jul.LogManager;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.opentext.lre.actions.runtest.LreTestRunBuilder.artifactsResourceName;

public class Main {

    private static final int PORT = 57396;
    private static ServerSocket serverSocket;
    private static LreTestRunModel lreTestRunModel;
    private static LreWorkspaceSyncModel lreWorkspaceSyncModel;
    private static String lreAction;

    public static void main(String[] args) throws Exception {
        // Check if another instance is already running
        int exit;
        if (!checkForRunningInstance()) {
            // If not, proceed with initialization and operational code
            initEnvironmentVariables(args);
            exit = performOperations();
        } else {
            // If another instance is already running, exit gracefully
            System.err.println("Another instance is already running.");
            exit = 1;
        }
        releaseSocket();
        System.exit(exit);
    }

    private static boolean checkForRunningInstance() {
        try {
            if(serverSocket == null) { // if socket is not null, socket was previously caught by current instance
                serverSocket = new ServerSocket(PORT);
            }
            return false; // No other instance is running
        } catch (IOException e) {
            // Another instance is already running
            return true;
        }
    }

    private static void releaseSocket() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
    }

    private static void initEnvironmentVariables(String[] args) throws Exception {
        InputRetriever inputRetriever = new InputRetriever(args);
        lreAction = inputRetriever.getLreAction();
        lreTestRunModel = inputRetriever.getLreTestRunModel();
        lreWorkspaceSyncModel = inputRetriever.getLreWorkspaceSyncModel();
    }

    private static int performOperations() {
        int exit;
        try {
            if ("ExecuteLreTest".equalsIgnoreCase(lreAction) && lreTestRunModel != null) {
                exit = executeTestRun();
            } else if ("WorkspaceSync".equalsIgnoreCase(lreAction) && lreWorkspaceSyncModel != null) {
                exit = executeWorkspaceSync();
            } else {
                exit = 1;
                LogHelper.error("No valid action or model found. Action: " + lreAction);
            }
            LogHelper.log(LocalizationManager.getString("ThatsAllFolks"), true);
        } catch (Exception ex) {
            exit = 1;
            LogHelper.logStackTrace(ex);
        }
        return exit;
    }

    private static int executeTestRun() throws Exception {
        DateFormatter dateFormatter = new DateFormatter("_E_yyyy_MMM_dd_'at'_HH_mm_ss_SSS_a_zzz");
        String logFileName = "lre_run_test_" + dateFormatter.getDate() + ".log";
        File logFile = new File(Paths.get(lreTestRunModel.getWorkspace(), artifactsResourceName, logFileName).toString());
        if (!logFile.getParentFile().exists() && !logFile.getParentFile().mkdirs()) {
            throw new IOException("Could not create log directory: " + logFile.getParentFile());
        }
        String logFilePath = logFile.getAbsolutePath();

        // Set log file property BEFORE any logging initialization
        System.setProperty("log.file", logFilePath);

        // Suppress java.util.logging cookie warnings from HTTP client
        Logger httpLogger = Logger.getLogger("org.apache.http.client.protocol.ResponseProcessCookies");
        httpLogger.setLevel(Level.SEVERE);

        // Bridge java.util.logging to log4j2
        java.util.logging.LogManager.getLogManager().reset();
        LogManager.getLogManager();

        // Now setup log4j2
        LogHelper.setup(logFilePath, lreTestRunModel.isEnableStacktrace());
        LogHelper.log(LocalizationManager.getString("BeginningLRETestExecution"), true);

        // Run the main builder
        LreTestRunBuilder lreTestRunBuilder = new LreTestRunBuilder(lreTestRunModel);
        boolean buildSuccess = lreTestRunBuilder.perform();
        if (buildSuccess) {
            LogHelper.log("Build successful", true);
            return 0;
        } else {
            LogHelper.error("Build failed");
            return 1;
        }
    }

    private static int executeWorkspaceSync() throws Exception {
        DateFormatter dateFormatter = new DateFormatter("_E_yyyy_MMM_dd_'at'_HH_mm_ss_SSS_a_zzz");
        String logFileName = "lre_workspace_sync_" + dateFormatter.getDate() + ".log";

        // Convert workspace String to Path
        java.nio.file.Path workspacePath = Paths.get(lreWorkspaceSyncModel.getWorkspace()).toAbsolutePath();

        // Create logs directory in the workspace path
        File logsDir = workspacePath.resolve("logs").toFile();
        if (!logsDir.exists() && !logsDir.mkdirs()) {
            throw new IOException("Could not create log directory: " + logsDir);
        }

        File logFile = new File(logsDir, logFileName);
        String logFilePath = logFile.getAbsolutePath();

        // Set log file property BEFORE any logging initialization
        System.setProperty("log.file", logFilePath);

        // Suppress java.util.logging cookie warnings from HTTP client
        Logger httpLogger = Logger.getLogger("org.apache.http.client.protocol.ResponseProcessCookies");
        httpLogger.setLevel(Level.SEVERE);

        // Bridge java.util.logging to log4j2
        java.util.logging.LogManager.getLogManager().reset();
        LogManager.getLogManager();

        // Now setup log4j2 - enable stack trace for detailed error reporting
        LogHelper.setup(logFilePath, lreWorkspaceSyncModel.isEnableStacktrace());

        LogHelper.log("Starting workspace synchronization", true);
        LogHelper.log("Workspace path: %s", true, lreWorkspaceSyncModel.getWorkspace());

        LreWorkspaceSyncTask task = new LreWorkspaceSyncTask(lreWorkspaceSyncModel);
        Result result = task.execute();

        LogHelper.log("Workspace synchronization completed with result: %s", true, result);
        return result == Result.SUCCESS ? 0 : 1;
    }
}
