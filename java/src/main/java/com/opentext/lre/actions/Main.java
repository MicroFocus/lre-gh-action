package com.opentext.lre.actions;

import com.opentext.lre.actions.common.helpers.InputRetriever;
import com.opentext.lre.actions.common.helpers.LocalizationManager;
import com.opentext.lre.actions.common.helpers.utils.DateFormatter;
import com.opentext.lre.actions.common.helpers.utils.LogHelper;
import com.opentext.lre.actions.runtest.LreTestRunBuilder;
import com.opentext.lre.actions.runtest.LreTestRunModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.net.ServerSocket;

import java.io.File;

import static com.opentext.lre.actions.runtest.LreTestRunBuilder.artifactsResourceName;

public class Main
{
    private static final int PORT = 57395;
    private static ServerSocket serverSocket;
    private static LreTestRunModel lreTestRunModel;

    public static void main( String[] args ) throws Exception {
        int exit = 0;
        // Check if another instance is already running
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
            if(serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
        }
    }

    private static void initEnvironmentVariables(String[] args) throws Exception {
        InputRetriever inputRetriever = new InputRetriever(args);
        lreTestRunModel = inputRetriever.getLreTestRunModel();
    }

    private static int performOperations() {
        int exit = 0;
        try {
            if (lreTestRunModel != null) {
                PrepareLogger();
                LreTestRunBuilder lreTestRunBuilder = new LreTestRunBuilder(lreTestRunModel);
                boolean buildSuccess = lreTestRunBuilder.perform();
                if(buildSuccess) {
                    LogHelper.log("Build successful", true);
                } else {
                    exit = 1;
                    LogHelper.error("Build failed");
                }
            } else {
                exit = 1;
                LogHelper.error(LocalizationManager.getString("SkippingEverything"));
            }
            LogHelper.log(LocalizationManager.getString("ThatsAllFolks"), true);
        } catch (Exception ex) {
            exit = 1;
            LogHelper.logStackTrace(ex);
        }
        return exit;
    }

    private static void PrepareLogger() throws IOException {
        DateFormatter dateFormatter = new DateFormatter("_E_yyyy_MMM_dd_'at'_HH_mm_ss_SSS_a_zzz");
        String logFileName = "lre_run_test_" + dateFormatter.getDate() + ".log";
        File dir = new File(Paths.get(lreTestRunModel.getWorkspace(), artifactsResourceName, logFileName).toString());
        if(!dir.getParentFile().exists()) {
            try {
                Files.createDirectory(dir.getParentFile().toPath());
            } catch (IOException e) {
                if(!dir.getParentFile().exists()) {
                    boolean isDirectoryCreated = dir.getParentFile().mkdirs();
                    if (isDirectoryCreated) {
                        throw new IOException("could not create directory " + dir.getParentFile().toString());
                    }
                }
            }
        }
        LogHelper.setup(dir.getAbsolutePath(), lreTestRunModel.isEnableStacktrace());
        LogHelper.log(LocalizationManager.getString("BeginningLRETestExecution"), true);
    }
}
