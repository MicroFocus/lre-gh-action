package com.opentext.lre.actions.workspacesync;

import com.microfocus.adm.performancecenter.plugins.common.pcentities.PcException;
import com.microfocus.adm.performancecenter.plugins.common.pcentities.PcScript;
import com.microfocus.adm.performancecenter.plugins.common.rest.PcRestProxy;
import com.opentext.lre.actions.common.helpers.constants.LreTestRunHelper;
import com.opentext.lre.actions.common.helpers.utils.LogHelper;
import com.opentext.lre.actions.common.helpers.utils.Result;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class LreWorkspaceSyncTask {
    private static final int DEFAULT_SUCCESS_THRESHOLD_PERCENT = 50;

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
        List<WorkspaceScriptInfo> listScripts() throws Exception;
        void deleteScript(int scriptId) throws Exception;
    }

    static final class WorkspaceScriptInfo {
        private final int id;
        private final String name;
        private final String testFolderPath;

        WorkspaceScriptInfo(int id, String name, String testFolderPath) {
            this.id = id;
            this.name = name;
            this.testFolderPath = testFolderPath;
        }

        int getId() {
            return id;
        }

        String getName() {
            return name;
        }

        String getTestFolderPath() {
            return testFolderPath;
        }
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

                        @Override
                        public List<WorkspaceScriptInfo> listScripts() throws Exception {
                            List<PcScript> scripts = proxy.getScripts().getPcScriptList();
                            List<WorkspaceScriptInfo> result = new ArrayList<>();
                            if (scripts != null) {
                                for (PcScript script : scripts) {
                                    result.add(new WorkspaceScriptInfo(script.getID(), script.getName(), script.getTestFolderPath()));
                                }
                            }
                            return result;
                        }

                        @Override
                        public void deleteScript(int scriptId) throws Exception {
                            proxy.deleteScript(scriptId);
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

            List<ScriptFolder> targetFolders = filterScriptFoldersForIncrementalSync(scriptFolders);
            processDeletedScriptsIfNeeded(restClient, workspacePath, scriptFolders);
            if (targetFolders.isEmpty()) {
                LogHelper.log("No changed script folders detected for incremental WorkspaceSync. Nothing to upload.", true);
                return Result.SUCCESS;
            }

            return processScriptFolderUploads(restClient, targetFolders);
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

        // Determine final result based on configured success threshold.
        int successfulUploads = totalScripts - totalFailures;
        int successThresholdPercent = resolveSuccessThresholdPercent();
        double successRatePercent = (double) successfulUploads * 100 / totalScripts;

        if (successThresholdPercent == 0 || successRatePercent >= successThresholdPercent) {
            LogHelper.log("Upload process completed: %d out of %d scripts uploaded successfully.",
                    true, successfulUploads, totalScripts);
            return Result.SUCCESS;
        } else {
            LogHelper.log(
                    "Upload process failed: Only %d out of %d scripts uploaded successfully (required at least %d%% success).",
                    true, successfulUploads, totalScripts, successThresholdPercent);
            return Result.FAILURE;
        }
    }

    private int resolveSuccessThresholdPercent() {
        int configuredThreshold = model.getWorkspaceSyncSuccessThresholdPercent();
        if (configuredThreshold < 0 || configuredThreshold > 100) {
            LogHelper.log(
                    "Invalid lre_workspace_sync_success_threshold value '%d'. Falling back to default %d%%.",
                    true,
                    configuredThreshold,
                    DEFAULT_SUCCESS_THRESHOLD_PERCENT);
            return DEFAULT_SUCCESS_THRESHOLD_PERCENT;
        }
        return configuredThreshold;
    }

    private List<ScriptFolder> filterScriptFoldersForIncrementalSync(List<ScriptFolder> scriptFolders) {
        if (!model.isWorkspaceSyncIncremental()) {
            return scriptFolders;
        }

        if (!model.isWorkspaceSyncChangesDetermined()) {
            LogHelper.log("WorkspaceSync incremental mode enabled, but previous build context is unavailable. Falling back to full sync.", true);
            return scriptFolders;
        }

        List<String> changedFiles = model.getWorkspaceSyncChangedFiles();
        if (changedFiles.isEmpty()) {
            return List.of();
        }

        List<ScriptFolder> selectedFolders = new ArrayList<>();
        for (ScriptFolder folder : scriptFolders) {
            String folderPath = normalizeRelativePath(folder.getRelativePath().toString());
            if (isFolderAffectedByChangedFiles(folderPath, changedFiles)) {
                selectedFolders.add(folder);
            }
        }

        LogHelper.log("WorkspaceSync incremental mode selected %d out of %d script(s) for upload.",
                true, selectedFolders.size(), scriptFolders.size());
        return selectedFolders;
    }

    private boolean isFolderAffectedByChangedFiles(String folderPath, List<String> changedFiles) {
        String normalizedFolder = normalizeRelativePath(folderPath);
        if (normalizedFolder.isEmpty()) {
            return true;
        }

        String prefix = normalizedFolder.endsWith("/") ? normalizedFolder : normalizedFolder + "/";
        for (String changedFile : changedFiles) {
            String normalizedChangedFile = normalizeRelativePath(changedFile);
            if (normalizedChangedFile.equals(normalizedFolder) || normalizedChangedFile.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeRelativePath(String value) {
        String normalized = value.replace('\\', '/').trim();
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private void processDeletedScriptsIfNeeded(WorkspaceRestClient restClient,
                                               Path workspacePath,
                                               List<ScriptFolder> scriptFolders) {
        if (!model.isWorkspaceSyncDeleteRemovedScripts()) {
            return;
        }

        if (!model.isWorkspaceSyncIncremental() || !model.isWorkspaceSyncChangesDetermined()) {
            LogHelper.log("Deleted-script handling requested, but incremental context is unavailable. Deletion is skipped.", true);
            return;
        }

        List<DeletedScriptRef> scriptsForDelete = resolveScriptsForDelete(workspacePath, scriptFolders, model.getWorkspaceSyncDeletedFiles());
        if (scriptsForDelete.isEmpty()) {
            return;
        }

        for (DeletedScriptRef scriptForDelete : scriptsForDelete) {
            deleteScriptIfExists(restClient, scriptForDelete);
        }
    }

    private List<DeletedScriptRef> resolveScriptsForDelete(Path workspacePath,
                                                           List<ScriptFolder> existingScriptFolders,
                                                           List<String> deletedFiles) {
        Set<String> existingScriptFolderPaths = new HashSet<>();
        for (ScriptFolder folder : existingScriptFolders) {
            String relativeFolderPath = normalizeRelativePath(workspacePath.relativize(folder.getFullPath().toAbsolutePath()).toString());
            if (!relativeFolderPath.isEmpty()) {
                existingScriptFolderPaths.add(relativeFolderPath);
            }
        }

        Set<String> processedScriptFolders = new HashSet<>();
        List<DeletedScriptRef> scriptsForDelete = new ArrayList<>();
        for (String deletedFile : deletedFiles) {
            Path deletedPath = Paths.get(normalizeRelativePath(deletedFile));
            Path deletedParent = deletedPath.getParent();
            if (deletedParent == null) {
                continue;
            }

            String scriptFolderPath = normalizeRelativePath(deletedParent.toString());
            if (scriptFolderPath.isEmpty()) {
                continue;
            }

            if (isUnderExistingScriptFolder(scriptFolderPath, existingScriptFolderPaths)) {
                continue;
            }

            if (!processedScriptFolders.add(scriptFolderPath)) {
                continue;
            }

            Path scriptFolder = Paths.get(scriptFolderPath);
            Path scriptParent = scriptFolder.getParent();
            Path scriptNamePath = scriptFolder.getFileName();
            if (scriptNamePath == null) {
                continue;
            }

            String subjectPath = LreSubjectPathBuilder.toSubjectPath(scriptParent);
            String scriptName = scriptNamePath.toString();
            scriptsForDelete.add(new DeletedScriptRef(subjectPath, scriptName));
        }

        return scriptsForDelete;
    }

    private boolean isUnderExistingScriptFolder(String candidateFolder, Set<String> existingScriptFolders) {
        for (String existingFolder : existingScriptFolders) {
            if (candidateFolder.equals(existingFolder) || candidateFolder.startsWith(existingFolder + "/")) {
                return true;
            }
        }
        return false;
    }

    private void deleteScriptIfExists(WorkspaceRestClient restClient, DeletedScriptRef scriptToDelete) {
        try {
            LogHelper.log("Deleting script '%s\\%s' from the project...", true,
                    scriptToDelete.subjectPath, scriptToDelete.scriptName);

            WorkspaceScriptInfo remoteScript = findScript(restClient.listScripts(), scriptToDelete.subjectPath, scriptToDelete.scriptName);
            if (remoteScript == null) {
                LogHelper.log("---- Script '%s\\%s' was not found in the project, therefore it cannot be deleted.",
                        true, scriptToDelete.subjectPath, scriptToDelete.scriptName);
                return;
            }

            restClient.deleteScript(remoteScript.getId());
            LogHelper.log("++++ Script '%s\\%s' deleted successfully.", true,
                    scriptToDelete.subjectPath, scriptToDelete.scriptName);
        } catch (Exception ex) {
            LogHelper.log("**** Could not delete script '%s\\%s'. Error: %s", true,
                    scriptToDelete.subjectPath, scriptToDelete.scriptName, ex.getMessage());
            LogHelper.logStackTrace(ex);
        }
    }

    private WorkspaceScriptInfo findScript(List<WorkspaceScriptInfo> scripts, String subjectPath, String scriptName) {
        if (scripts == null) {
            return null;
        }

        for (WorkspaceScriptInfo script : scripts) {
            if (script.getName() == null || script.getTestFolderPath() == null) {
                continue;
            }

            if (script.getName().equalsIgnoreCase(scriptName)
                    && script.getTestFolderPath().equalsIgnoreCase(subjectPath)) {
                return script;
            }
        }
        return null;
    }

    private static final class DeletedScriptRef {
        private final String subjectPath;
        private final String scriptName;

        private DeletedScriptRef(String subjectPath, String scriptName) {
            this.subjectPath = subjectPath;
            this.scriptName = scriptName;
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
