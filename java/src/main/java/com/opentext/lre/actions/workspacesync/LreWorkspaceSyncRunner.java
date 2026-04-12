package com.opentext.lre.actions.workspacesync;

import com.opentext.lre.actions.common.helpers.utils.DateFormatter;
import com.opentext.lre.actions.common.helpers.utils.LogHelper;
import com.opentext.lre.actions.common.helpers.utils.Result;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class LreWorkspaceSyncRunner {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: LreWorkspaceSyncRunner <config.json>");
            System.exit(2);
        }

        Path configPath = Paths.get(args[0]);
        String configContent = Files.readString(configPath, StandardCharsets.UTF_8);
        JSONObject json = new JSONObject(configContent);

        LreWorkspaceSyncModel model = LreWorkspaceSyncRunnerConfig.fromJson(json);

        // Initialize logging
        setupLogging(model);
        LogHelper.log("Step Description: %s", true, model.getDescription());
        LogHelper.log("Starting workspace synchronization", true);
        LogHelper.log("Workspace path: %s", true, model.getWorkspace());

        LreWorkspaceSyncTask task = new LreWorkspaceSyncTask(model);
        Result result = task.execute();

        LogHelper.log("Workspace synchronization completed with result: %s", true, result);
        System.exit(result == Result.SUCCESS ? 0 : 1);
    }

    private static void setupLogging(LreWorkspaceSyncModel model) throws Exception {
        DateFormatter dateFormatter = new DateFormatter("_E_yyyy_MMM_dd_'at'_HH_mm_ss_SSS_a_zzz");
        String logFileName = "lre_workspace_sync_" + dateFormatter.getDate() + ".log";

        // Create logs directory in the workspace path
        Path workspacePath = Paths.get(model.getWorkspace()).toAbsolutePath();
        Path logsDir = workspacePath.resolve("logs");
        File logsDirFile = logsDir.toFile();
        if (!logsDirFile.exists() && !logsDirFile.mkdirs()) {
            throw new IOException("Could not create log directory: " + logsDir);
        }

        Path logFile = logsDir.resolve(logFileName);
        String logFilePath = logFile.toAbsolutePath().toString();
        System.setProperty("log.file", logFilePath);

        // Enable stack trace for detailed error reporting
        LogHelper.setup(logFilePath, model.isEnableStacktrace());

        LogHelper.log("Log file: %s", true, logFilePath);
    }
}

