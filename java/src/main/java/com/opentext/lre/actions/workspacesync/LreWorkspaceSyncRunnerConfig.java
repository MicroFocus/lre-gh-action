package com.opentext.lre.actions.workspacesync;

import org.json.JSONObject;

public final class LreWorkspaceSyncRunnerConfig {
    private LreWorkspaceSyncRunnerConfig() {
    }

    public static LreWorkspaceSyncModel fromJson(JSONObject json) {
        String server = requireString(json, "lre_server");
        boolean https = json.optBoolean("lre_https_protocol", true);
        boolean authenticateWithToken = json.optBoolean("lre_authenticate_with_token", false);
        String username = requireString(json, "lre_username");
        String password = requireString(json, "lre_password");
        String domain = requireString(json, "lre_domain");
        String project = requireString(json, "lre_project");
        String proxyUrl = json.optString("lre_proxy_out_url", "");
        String proxyUsername = json.optString("lre_username_proxy", "");
        String proxyPassword = json.optString("lre_password_proxy", "");
        String workspacePath = requireString(json, "lre_workspace_dir");
        boolean runtimeOnly = json.optBoolean("lre_runtime_only", true);
        boolean lreEnableStacktrace = json.optBoolean("lre_enable_stacktrace", false);
        String description = json.optString("lre_description", "");

        return new LreWorkspaceSyncModel(
                server,
                https,
                username,
                password,
                domain,
                project,
                proxyUrl,
                proxyUsername,
                proxyPassword,
                workspacePath,
                runtimeOnly,
                authenticateWithToken,
                lreEnableStacktrace,
                description);
    }

    private static String requireString(JSONObject json, String key) {
        String value = json.optString(key, "").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Missing required config value: " + key);
        }
        return value;
    }
}
