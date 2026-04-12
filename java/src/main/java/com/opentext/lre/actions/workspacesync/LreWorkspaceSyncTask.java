package com.opentext.lre.actions.workspacesync;

import com.microfocus.adm.performancecenter.plugins.common.pcentities.PcException;
import com.microfocus.adm.performancecenter.plugins.common.rest.PcRestProxy;
import com.opentext.lre.actions.common.helpers.constants.LreTestRunHelper;
import com.opentext.lre.actions.common.helpers.utils.LogHelper;
import com.opentext.lre.actions.common.helpers.utils.Result;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

public final class LreWorkspaceSyncTask {
    interface ScriptScanner {
        List<ScriptFolder> findScriptFolders(Path workspaceRoot) throws IOException;
    }

    interface FolderCompressor {
        Path compressFolder(ScriptFolder folder) throws Exception;
    }

    interface WorkspaceRestClient {
        boolean authenticate(String username, String password) throws Exception;
        void logout() throws Exception;
        int uploadScript(String subjectPath, boolean overwriteScript, boolean runtimeOnly,
                         boolean preserveAssets, String zipPath) throws Exception;
    }

    interface WorkspaceRestClientFactory {
        WorkspaceRestClient create(LreWorkspaceSyncModel model) throws PcException;
    }

    private final LreWorkspaceSyncModel model;
    private final ScriptScanner scanner;
    private final FolderCompressor compressor;
    private final WorkspaceRestClientFactory restClientFactory;

    public LreWorkspaceSyncTask(LreWorkspaceSyncModel model) {
        this(model,
                workspaceRoot -> new WorkspaceScriptFolderScanner().findScriptFolders(workspaceRoot),
                folder -> new ZipFolderCompressor().compressFolder(folder),
                taskModel -> {
                    PcRestProxy proxy = new PcRestProxy(
                            taskModel.getProtocol(),
                            taskModel.getLreServerAndPort(),
                            taskModel.isAuthenticateWithToken(),
                            taskModel.getDomain(),
                            taskModel.getProject(),
                            taskModel.getProxyOutURL(),
                            taskModel.getUsernameProxy(),
                            taskModel.getPasswordProxy());

                    return new WorkspaceRestClient() {
                        @Override
                        public boolean authenticate(String username, String password) throws Exception {
                            return proxy.authenticate(username, password);
                        }

                        @Override
                        public void logout() throws Exception {
                            proxy.logout();
                        }

                        @Override
                        public int uploadScript(String subjectPath, boolean overwriteScript, boolean runtimeOnly,
                                                boolean preserveAssets, String zipPath) throws Exception {
                            return proxy.uploadScript(subjectPath, overwriteScript, runtimeOnly, preserveAssets, zipPath);
                        }
                    };
                });
    }

    LreWorkspaceSyncTask(LreWorkspaceSyncModel model,
                         ScriptScanner scanner,
                         FolderCompressor compressor,
                         WorkspaceRestClientFactory restClientFactory) {
        this.model = model;
        this.scanner = Objects.requireNonNull(scanner, "scanner");
        this.compressor = Objects.requireNonNull(compressor, "compressor");
        this.restClientFactory = Objects.requireNonNull(restClientFactory, "restClientFactory");
        // Set the static flag for stack trace output based on configuration
        LreTestRunHelper.ENABLE_STACKTRACE = model.isEnableStacktrace();
    }

    public Result execute() {
        WorkspaceRestClient restClient = createRestClient();
        if (restClient == null) {
            return Result.FAILURE;
        }

        boolean loggedIn = false;
        try {
            loggedIn = restClient.authenticate(model.getUsername(), model.getPassword());
            if (!loggedIn) {
                LogHelper.log("Login failed.", true);
                return Result.FAILURE;
            }

            // Convert workspace String to Path for file operations
            Path workspacePath = Paths.get(model.getWorkspace()).toAbsolutePath();
            List<ScriptFolder> scriptFolders = scanner.findScriptFolders(workspacePath);
            if (scriptFolders.isEmpty()) {
                LogHelper.log("No script folders found in workspace.", true);
                return Result.SUCCESS;
            }

            return processScriptFolderUploads(restClient, scriptFolders);
        } catch (Exception e) {
            LogHelper.log("Workspace sync failed: %s", true, e.getMessage());
            LogHelper.logStackTrace(e);
            return Result.FAILURE;
        } finally {
            if (loggedIn) {
                try {
                    restClient.logout();
                } catch (Exception e) {
                    LogHelper.log("Logout failed: %s", true, e.getMessage());
                }
            }
        }
    }

    private Result processScriptFolderUploads(WorkspaceRestClient restClient, List<ScriptFolder> scriptFolders) {
        final int MAX_CONSECUTIVE_FAILURES = 5;
        final int totalScripts = scriptFolders.size();

        int consecutiveFailures = 0;
        int totalFailures = 0;
        int currentIndex = 0;

        LogHelper.log("Found %d script(s) to upload.", true, totalScripts);

        for (ScriptFolder folder : scriptFolders) {
            currentIndex++;
            LogHelper.log("Script #%d out of %d", true, currentIndex, totalScripts);

            Result uploadResult = uploadFolder(restClient, folder);

            if (uploadResult == Result.FAILURE) {
                totalFailures++;
                consecutiveFailures++;

                if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                    LogHelper.log("Upload process terminated: %d consecutive failures detected. %d out of %d scripts uploaded successfully.",
                            true, MAX_CONSECUTIVE_FAILURES, currentIndex - totalFailures, totalScripts);
                    return Result.FAILURE;
                }
            } else {
                consecutiveFailures = 0;
            }
        }

        // Determine final result based on failure rate
        int successfulUploads = totalScripts - totalFailures;
        double successRate = (double) successfulUploads / totalScripts;

        if (successRate >= 0.5) {
            LogHelper.log("Upload process completed: %d out of %d scripts uploaded successfully.",
                    true, successfulUploads, totalScripts);
            return Result.SUCCESS;
        } else {
            LogHelper.log("Upload process failed: Only %d out of %d scripts uploaded successfully (less than 50%%).",
                    true, successfulUploads, totalScripts);
            return Result.FAILURE;
        }
    }

    private Result uploadFolder(WorkspaceRestClient restClient, ScriptFolder folder) {
        Path zipPath = null;
        try {
            zipPath = compressor.compressFolder(folder);
            String subjectPath = LreSubjectPathBuilder.toSubjectPath(folder.getRelativePath());
            String scriptName = folder.getFullPath().getFileName() == null
                    ? folder.getRelativePath().toString()
                    : folder.getFullPath().getFileName().toString();
            LogHelper.log("Starting uploading script %s to path %s", true, scriptName, subjectPath);
            int scriptId;
            try {
                scriptId = restClient.uploadScript(subjectPath, true, model.isRuntimeOnly(), true, zipPath.toString());
                if (scriptId == 0) {
                    LogHelper.log("Failed to upload script %s in folder %s to path %s", true, scriptName, folder.getRelativePath(), subjectPath);
                    return Result.FAILURE;
                }
            } catch (Exception e) {
                LogHelper.log("Upload failed for script %s in folder %s to path %s with error %s", true, scriptName, folder.getRelativePath(), subjectPath, e.getMessage());
                LogHelper.logStackTrace(e);
                return Result.FAILURE;
            }

            LogHelper.log("Script %s was successfully uploaded to path %s with ID = %d", true, scriptName, subjectPath, scriptId);
            return Result.SUCCESS;
        } catch (Exception e) {
            LogHelper.log("Upload failed for %s: %s", true, folder.getRelativePath(), e.getMessage());
            LogHelper.logStackTrace(e);
            return Result.FAILURE;
        } finally {
            if (zipPath != null) {
                try {
                    Files.deleteIfExists(zipPath);
                } catch (IOException e) {
                    LogHelper.log("Failed to delete temp zip: %s", true, zipPath);
                }
            }
        }
    }

    private WorkspaceRestClient createRestClient() {
        try {
            return restClientFactory.create(model);
        } catch (PcException e) {
            LogHelper.log("Connection to LRE server failed: %s", true, e.getMessage());
            LogHelper.logStackTrace(e);
            return null;
        }
    }
}
