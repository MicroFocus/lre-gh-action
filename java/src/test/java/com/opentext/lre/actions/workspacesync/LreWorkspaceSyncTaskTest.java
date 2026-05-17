package com.opentext.lre.actions.workspacesync;

import com.opentext.lre.actions.common.helpers.utils.Result;
import junit.framework.TestCase;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

public class LreWorkspaceSyncTaskTest extends TestCase {

    public void testExecuteReturnsFailureWhenAuthenticationFails() throws Exception {
        Path workspace = Files.createTempDirectory("lre-sync-auth-fail");
        LreWorkspaceSyncModel model = createModel(workspace.toString(), 50);

        FakeRestClient restClient = new FakeRestClient(false, List.of());
        LreWorkspaceSyncTask task = createTask(model, root -> List.of(), createZipCompressor(), restClient);

        Result result = task.execute();

        assertEquals(Result.FAILURE, result);
        assertFalse(restClient.logoutCalled);
        assertEquals(0, restClient.uploadCallCount);
    }

    public void testExecuteReturnsSuccessWhenNoScriptsFound() throws Exception {
        Path workspace = Files.createTempDirectory("lre-sync-no-scripts");
        LreWorkspaceSyncModel model = createModel(workspace.toString(), 50);

        FakeRestClient restClient = new FakeRestClient(true, List.of());
        LreWorkspaceSyncTask task = createTask(model, root -> List.of(), createZipCompressor(), restClient);

        Result result = task.execute();

        assertEquals(Result.SUCCESS, result);
        assertTrue(restClient.logoutCalled);
        assertEquals(0, restClient.uploadCallCount);
    }

    public void testExecuteReturnsSuccessWhenAtLeastHalfUploadsSucceed() throws Exception {
        Path workspace = Files.createTempDirectory("lre-sync-success-rate");
        List<ScriptFolder> folders = createScriptFolders(workspace, "s1", "s2", "s3", "s4");
        LreWorkspaceSyncModel model = createModel(workspace.toString(), 50);

        FakeRestClient restClient = new FakeRestClient(true, Arrays.asList(101, 0, 102, 0));
        LreWorkspaceSyncTask task = createTask(model, root -> folders, createZipCompressor(), restClient);

        Result result = task.execute();

        assertEquals(Result.SUCCESS, result);
        assertTrue(restClient.logoutCalled);
        assertEquals(4, restClient.uploadCallCount);
    }

    public void testExecuteStopsAfterFiveConsecutiveFailures() throws Exception {
        Path workspace = Files.createTempDirectory("lre-sync-five-failures");
        List<ScriptFolder> folders = createScriptFolders(workspace, "a", "b", "c", "d", "e", "f", "g");
        LreWorkspaceSyncModel model = createModel(workspace.toString(), 50);

        FakeRestClient restClient = new FakeRestClient(true, Arrays.asList(0, 0, 0, 0, 0, 111, 112));
        LreWorkspaceSyncTask task = createTask(model, root -> folders, createZipCompressor(), restClient);

        Result result = task.execute();

        assertEquals(Result.FAILURE, result);
        assertEquals(5, restClient.uploadCallCount);
        assertTrue(restClient.logoutCalled);
    }

    public void testExecuteReturnsFailureWhenBelowConfiguredSuccessThreshold() throws Exception {
        Path workspace = Files.createTempDirectory("lre-sync-threshold-fail");
        List<ScriptFolder> folders = createScriptFolders(workspace, "s1", "s2", "s3", "s4");
        LreWorkspaceSyncModel model = createModel(workspace.toString(), 80);

        FakeRestClient restClient = new FakeRestClient(true, Arrays.asList(101, 0, 102, 0));
        LreWorkspaceSyncTask task = createTask(model, root -> folders, createZipCompressor(), restClient);

        Result result = task.execute();

        assertEquals(Result.FAILURE, result);
        assertTrue(restClient.logoutCalled);
        assertEquals(4, restClient.uploadCallCount);
    }

    public void testExecuteReturnsSuccessWhenThresholdIsZero() throws Exception {
        Path workspace = Files.createTempDirectory("lre-sync-threshold-zero");
        List<ScriptFolder> folders = createScriptFolders(workspace, "s1", "s2", "s3", "s4");
        LreWorkspaceSyncModel model = createModel(workspace.toString(), 0);

        FakeRestClient restClient = new FakeRestClient(true, Arrays.asList(0, 0, 0, 0));
        LreWorkspaceSyncTask task = createTask(model, root -> folders, createZipCompressor(), restClient);

        Result result = task.execute();

        assertEquals(Result.SUCCESS, result);
        assertTrue(restClient.logoutCalled);
        assertEquals(4, restClient.uploadCallCount);
    }

    public void testExecuteReturnsFailureWhenThresholdIsHundredAndAnyUploadFails() throws Exception {
        Path workspace = Files.createTempDirectory("lre-sync-threshold-hundred");
        List<ScriptFolder> folders = createScriptFolders(workspace, "s1", "s2", "s3", "s4");
        LreWorkspaceSyncModel model = createModel(workspace.toString(), 100);

        FakeRestClient restClient = new FakeRestClient(true, Arrays.asList(101, 102, 0, 103));
        LreWorkspaceSyncTask task = createTask(model, root -> folders, createZipCompressor(), restClient);

        Result result = task.execute();

        assertEquals(Result.FAILURE, result);
        assertTrue(restClient.logoutCalled);
        assertEquals(4, restClient.uploadCallCount);
    }

    public void testExecuteInvalidThresholdFallsBackToFiftyPercent() throws Exception {
        Path workspace = Files.createTempDirectory("lre-sync-threshold-invalid");
        List<ScriptFolder> folders = createScriptFolders(workspace, "s1", "s2", "s3", "s4");
        LreWorkspaceSyncModel model = createModel(workspace.toString(), 200);

        FakeRestClient restClient = new FakeRestClient(true, Arrays.asList(101, 0, 102, 0));
        LreWorkspaceSyncTask task = createTask(model, root -> folders, createZipCompressor(), restClient);

        Result result = task.execute();

        assertEquals(Result.SUCCESS, result);
        assertTrue(restClient.logoutCalled);
        assertEquals(4, restClient.uploadCallCount);
    }

    public void testExecuteIncrementalSyncUploadsOnlyAffectedScripts() throws Exception {
        Path workspace = Files.createTempDirectory("lre-sync-incremental-filter");
        List<ScriptFolder> folders = createScriptFolders(workspace, "s1", "s2", "s3");
        LreWorkspaceSyncModel model = createModel(
                workspace.toString(),
                50,
                true,
                true,
                List.of("s2/script.usr", "s2/data.csv")
        );

        FakeRestClient restClient = new FakeRestClient(true, List.of(101));
        LreWorkspaceSyncTask task = createTask(model, root -> folders, createZipCompressor(), restClient);

        Result result = task.execute();

        assertEquals(Result.SUCCESS, result);
        assertTrue(restClient.logoutCalled);
        assertEquals(1, restClient.uploadCallCount);
    }

    public void testExecuteIncrementalSyncReturnsSuccessWhenNoChangedScripts() throws Exception {
        Path workspace = Files.createTempDirectory("lre-sync-incremental-none");
        List<ScriptFolder> folders = createScriptFolders(workspace, "s1", "s2");
        LreWorkspaceSyncModel model = createModel(
                workspace.toString(),
                50,
                true,
                true,
                List.of()
        );

        FakeRestClient restClient = new FakeRestClient(true, List.of());
        LreWorkspaceSyncTask task = createTask(model, root -> folders, createZipCompressor(), restClient);

        Result result = task.execute();

        assertEquals(Result.SUCCESS, result);
        assertTrue(restClient.logoutCalled);
        assertEquals(0, restClient.uploadCallCount);
    }

    public void testExecuteIncrementalSyncFallsBackToFullSyncWhenChangesNotDetermined() throws Exception {
        Path workspace = Files.createTempDirectory("lre-sync-incremental-fallback");
        List<ScriptFolder> folders = createScriptFolders(workspace, "s1", "s2", "s3", "s4");
        LreWorkspaceSyncModel model = createModel(
                workspace.toString(),
                50,
                true,
                false,
                List.of()
        );

        FakeRestClient restClient = new FakeRestClient(true, Arrays.asList(101, 102, 103, 104));
        LreWorkspaceSyncTask task = createTask(model, root -> folders, createZipCompressor(), restClient);

        Result result = task.execute();

        assertEquals(Result.SUCCESS, result);
        assertTrue(restClient.logoutCalled);
        assertEquals(4, restClient.uploadCallCount);
    }

    public void testExecuteIncrementalSyncDeletesRemovedScriptsWhenEnabled() throws Exception {
        Path workspace = Files.createTempDirectory("lre-sync-incremental-delete");
        List<ScriptFolder> folders = createScriptFolders(workspace, "s2");
        LreWorkspaceSyncModel model = createModel(
                workspace.toString(),
                50,
                true,
                true,
                true,
                List.of("s2/script.usr"),
                List.of("s1/script.usr")
        );

        List<LreWorkspaceSyncTask.WorkspaceScriptInfo> remoteScripts = List.of(
                new LreWorkspaceSyncTask.WorkspaceScriptInfo(701, "s1", "Subject")
        );
        FakeRestClient restClient = new FakeRestClient(true, List.of(101), remoteScripts);
        LreWorkspaceSyncTask task = createTask(model, root -> folders, createZipCompressor(), restClient);

        Result result = task.execute();

        assertEquals(Result.SUCCESS, result);
        assertTrue(restClient.logoutCalled);
        assertEquals(1, restClient.uploadCallCount);
        assertEquals(1, restClient.deleteCallCount);
        assertEquals(701, restClient.deletedScriptIds.get(0).intValue());
    }

    public void testExecuteIncrementalSyncDoesNotDeleteRemovedScriptsWhenDisabled() throws Exception {
        Path workspace = Files.createTempDirectory("lre-sync-incremental-delete-disabled");
        List<ScriptFolder> folders = createScriptFolders(workspace, "s2");
        LreWorkspaceSyncModel model = createModel(
                workspace.toString(),
                50,
                true,
                true,
                false,
                List.of("s2/script.usr"),
                List.of("s1/script.usr")
        );

        List<LreWorkspaceSyncTask.WorkspaceScriptInfo> remoteScripts = List.of(
                new LreWorkspaceSyncTask.WorkspaceScriptInfo(701, "s1", "Subject")
        );
        FakeRestClient restClient = new FakeRestClient(true, List.of(101), remoteScripts);
        LreWorkspaceSyncTask task = createTask(model, root -> folders, createZipCompressor(), restClient);

        Result result = task.execute();

        assertEquals(Result.SUCCESS, result);
        assertTrue(restClient.logoutCalled);
        assertEquals(1, restClient.uploadCallCount);
        assertEquals(0, restClient.deleteCallCount);
    }

    private LreWorkspaceSyncTask createTask(LreWorkspaceSyncModel model,
                                            LreWorkspaceSyncTask.ScriptScanner scanner,
                                            LreWorkspaceSyncTask.FolderCompressor compressor,
                                            FakeRestClient restClient) {
        return new LreWorkspaceSyncTask(
                model,
                scanner,
                compressor,
                taskModel -> restClient
        );
    }

    private LreWorkspaceSyncTask.FolderCompressor createZipCompressor() {
        return folder -> {
            Path zipPath = folder.getFullPath().resolveSibling(folder.getZipFileName());
            Files.writeString(zipPath, "zip-content");
            return zipPath;
        };
    }

    private List<ScriptFolder> createScriptFolders(Path workspace, String... names) throws Exception {
        List<ScriptFolder> folders = new ArrayList<>();
        for (String name : names) {
            Path dir = Files.createDirectories(workspace.resolve(name));
            Files.writeString(dir.resolve("script.usr"), "dummy");
            folders.add(new ScriptFolder(dir, workspace));
        }
        return folders;
    }

    private LreWorkspaceSyncModel createModel(String workspacePath, int successThresholdPercent) {
        return createModel(workspacePath, successThresholdPercent, false, false, false, List.of(), List.of());
    }

    private LreWorkspaceSyncModel createModel(String workspacePath,
                                              int successThresholdPercent,
                                              boolean incrementalSync,
                                              boolean changesDetermined,
                                              List<String> changedFiles) {
        return createModel(workspacePath,
                successThresholdPercent,
                incrementalSync,
                changesDetermined,
                false,
                changedFiles,
                List.of());
    }

    private LreWorkspaceSyncModel createModel(String workspacePath,
                                              int successThresholdPercent,
                                              boolean incrementalSync,
                                              boolean changesDetermined,
                                              boolean deleteRemovedScripts,
                                              List<String> changedFiles,
                                              List<String> deletedFiles) {
        return new LreWorkspaceSyncModel(
                "server?tenant=abc",
                false,
                "user",
                "pass",
                "domain",
                "project",
                "",
                "",
                "",
                workspacePath,
                true,
                successThresholdPercent,
                incrementalSync,
                deleteRemovedScripts,
                changesDetermined,
                changedFiles,
                deletedFiles,
                false,
                true,
                "desc"
        );
    }

    private static final class FakeRestClient implements LreWorkspaceSyncTask.WorkspaceRestClient {
        private final boolean authResult;
        private final Queue<Integer> uploadResults;
        private final List<LreWorkspaceSyncTask.WorkspaceScriptInfo> remoteScripts;
        private boolean logoutCalled;
        private int uploadCallCount;
        private int deleteCallCount;
        private final List<Integer> deletedScriptIds;

        private FakeRestClient(boolean authResult, List<Integer> uploadResults) {
            this(authResult, uploadResults, List.of());
        }

        private FakeRestClient(boolean authResult,
                               List<Integer> uploadResults,
                               List<LreWorkspaceSyncTask.WorkspaceScriptInfo> remoteScripts) {
            this.authResult = authResult;
            this.uploadResults = new ArrayDeque<>(uploadResults);
            this.remoteScripts = new ArrayList<>(remoteScripts);
            this.logoutCalled = false;
            this.uploadCallCount = 0;
            this.deleteCallCount = 0;
            this.deletedScriptIds = new ArrayList<>();
        }

        @Override
        public boolean authenticate(String username, String password) {
            return authResult;
        }

        @Override
        public void logout() {
            logoutCalled = true;
        }

        @Override
        public int uploadScript(String subjectPath, boolean overwriteScript, boolean runtimeOnly,
                                boolean preserveAssets, String zipPath) {
            uploadCallCount++;
            if (uploadResults.isEmpty()) {
                return 0;
            }
            return uploadResults.remove();
        }

        @Override
        public List<LreWorkspaceSyncTask.WorkspaceScriptInfo> listScripts() {
            return new ArrayList<>(remoteScripts);
        }

        @Override
        public void deleteScript(int scriptId) {
            deleteCallCount++;
            deletedScriptIds.add(scriptId);
        }
    }
}

