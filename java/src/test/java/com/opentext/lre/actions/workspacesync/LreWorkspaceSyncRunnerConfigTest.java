package com.opentext.lre.actions.workspacesync;

import junit.framework.TestCase;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class LreWorkspaceSyncRunnerConfigTest extends TestCase {

    public void testFromJsonAppliesDefaults() {
        JSONObject json = new JSONObject()
                .put("lre_server", "myserver?tenant=abc")
                .put("lre_username", "user")
                .put("lre_password", "pass")
                .put("lre_domain", "D")
                .put("lre_project", "P")
                .put("lre_workspace_dir", "C:/ws");

        LreWorkspaceSyncModel model = LreWorkspaceSyncRunnerConfig.fromJson(json);

        assertEquals("myserver/?tenant=abc", model.getLreServerAndPort());
        assertEquals("https", model.getProtocol());
        assertEquals("user", model.getUsername());
        assertEquals("pass", model.getPassword());
        assertEquals("D", model.getDomain());
        assertEquals("P", model.getProject());
        assertEquals("C:/ws", model.getWorkspace());
        assertTrue(model.isRuntimeOnly());
        assertEquals(50, model.getWorkspaceSyncSuccessThresholdPercent());
        assertFalse(model.isWorkspaceSyncIncremental());
        assertFalse(model.isWorkspaceSyncDeleteRemovedScripts());
        assertFalse(model.isWorkspaceSyncChangesDetermined());
        assertEquals(List.of(), model.getWorkspaceSyncChangedFiles());
        assertEquals(List.of(), model.getWorkspaceSyncDeletedFiles());
        assertFalse(model.isAuthenticateWithToken());
        assertFalse(model.isEnableStacktrace());
        assertEquals("", model.getDescription());
    }

    public void testFromJsonReadsExplicitValues() {
        JSONObject json = new JSONObject()
                .put("lre_server", "myserver")
                .put("lre_https_protocol", true)
                .put("lre_authenticate_with_token", true)
                .put("lre_username", "user")
                .put("lre_password", "pass")
                .put("lre_domain", "D")
                .put("lre_project", "P")
                .put("lre_proxy_out_url", "http://proxy")
                .put("lre_username_proxy", "puser")
                .put("lre_password_proxy", "ppass")
                .put("lre_workspace_dir", "C:/ws")
                .put("lre_runtime_only", false)
                .put("lre_workspace_sync_success_threshold", 70)
                .put("lre_workspace_sync_incremental", true)
                .put("lre_workspace_sync_delete_removed_scripts", true)
                .put("lre_workspace_sync_changes_determined", true)
                .put("lre_workspace_sync_changed_files", new JSONArray().put("scriptA/main.js").put("scriptB/script.usr"))
                .put("lre_workspace_sync_deleted_files", new JSONArray().put("scriptZ/main.js"))
                .put("lre_enable_stacktrace", true)
                .put("lre_description", "sync");

        LreWorkspaceSyncModel model = LreWorkspaceSyncRunnerConfig.fromJson(json);

        assertEquals("https", model.getProtocol());
        assertTrue(model.isAuthenticateWithToken());
        assertEquals("http://proxy", model.getProxyOutURL());
        assertEquals("puser", model.getUsernameProxy());
        assertEquals("ppass", model.getPasswordProxy());
        assertFalse(model.isRuntimeOnly());
        assertEquals(70, model.getWorkspaceSyncSuccessThresholdPercent());
        assertTrue(model.isWorkspaceSyncIncremental());
        assertTrue(model.isWorkspaceSyncDeleteRemovedScripts());
        assertTrue(model.isWorkspaceSyncChangesDetermined());
        assertEquals(List.of("scriptA/main.js", "scriptB/script.usr"), model.getWorkspaceSyncChangedFiles());
        assertEquals(List.of("scriptZ/main.js"), model.getWorkspaceSyncDeletedFiles());
        assertTrue(model.isEnableStacktrace());
        assertEquals("sync", model.getDescription());
    }

    public void testFromJsonInvalidThresholdFallsBackToDefault() {
        JSONObject json = new JSONObject()
                .put("lre_server", "myserver")
                .put("lre_username", "user")
                .put("lre_password", "pass")
                .put("lre_domain", "D")
                .put("lre_project", "P")
                .put("lre_workspace_dir", "C:/ws")
                .put("lre_workspace_sync_success_threshold", "invalid");

        LreWorkspaceSyncModel model = LreWorkspaceSyncRunnerConfig.fromJson(json);

        assertEquals(50, model.getWorkspaceSyncSuccessThresholdPercent());
    }

    public void testFromJsonMissingRequiredKeyThrows() {
        JSONObject json = new JSONObject()
                .put("lre_username", "user")
                .put("lre_password", "pass")
                .put("lre_domain", "D")
                .put("lre_project", "P")
                .put("lre_workspace_dir", "C:/ws");

        try {
            LreWorkspaceSyncRunnerConfig.fromJson(json);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("lre_server"));
        }
    }
}

