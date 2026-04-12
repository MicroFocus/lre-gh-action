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
        LreWorkspaceSyncModel model = createModel(workspace.toString());

        FakeRestClient restClient = new FakeRestClient(false, List.of());
        LreWorkspaceSyncTask task = createTask(model, root -> List.of(), createZipCompressor(), restClient);

        Result result = task.execute();

        assertEquals(Result.FAILURE, result);
        assertFalse(restClient.logoutCalled);
        assertEquals(0, restClient.uploadCallCount);
    }

    public void testExecuteReturnsSuccessWhenNoScriptsFound() throws Exception {
        Path workspace = Files.createTempDirectory("lre-sync-no-scripts");
        LreWorkspaceSyncModel model = createModel(workspace.toString());

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
        LreWorkspaceSyncModel model = createModel(workspace.toString());

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
        LreWorkspaceSyncModel model = createModel(workspace.toString());

        FakeRestClient restClient = new FakeRestClient(true, Arrays.asList(0, 0, 0, 0, 0, 111, 112));
        LreWorkspaceSyncTask task = createTask(model, root -> folders, createZipCompressor(), restClient);

        Result result = task.execute();

        assertEquals(Result.FAILURE, result);
        assertEquals(5, restClient.uploadCallCount);
        assertTrue(restClient.logoutCalled);
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

    private LreWorkspaceSyncModel createModel(String workspacePath) {
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
                false,
                true,
                "desc"
        );
    }

    private static final class FakeRestClient implements LreWorkspaceSyncTask.WorkspaceRestClient {
        private final boolean authResult;
        private final Queue<Integer> uploadResults;
        private boolean logoutCalled;
        private int uploadCallCount;

        private FakeRestClient(boolean authResult, List<Integer> uploadResults) {
            this.authResult = authResult;
            this.uploadResults = new ArrayDeque<>(uploadResults);
            this.logoutCalled = false;
            this.uploadCallCount = 0;
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
    }
}

