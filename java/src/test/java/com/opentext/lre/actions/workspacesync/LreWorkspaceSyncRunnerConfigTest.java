package com.opentext.lre.actions.workspacesync;

import junit.framework.TestCase;
import org.json.JSONObject;

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
                .put("lre_enable_stacktrace", true)
                .put("lre_description", "sync");

        LreWorkspaceSyncModel model = LreWorkspaceSyncRunnerConfig.fromJson(json);

        assertEquals("https", model.getProtocol());
        assertTrue(model.isAuthenticateWithToken());
        assertEquals("http://proxy", model.getProxyOutURL());
        assertEquals("puser", model.getUsernameProxy());
        assertEquals("ppass", model.getPasswordProxy());
        assertFalse(model.isRuntimeOnly());
        assertTrue(model.isEnableStacktrace());
        assertEquals("sync", model.getDescription());
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

