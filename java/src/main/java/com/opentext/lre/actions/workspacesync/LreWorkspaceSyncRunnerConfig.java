package com.opentext.lre.actions.workspacesync;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

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
        int workspaceSyncSuccessThresholdPercent = parseIntOrDefault(json.opt("lre_workspace_sync_success_threshold"), 50);
        boolean workspaceSyncIncremental = json.optBoolean("lre_workspace_sync_incremental", false);
        boolean workspaceSyncDeleteRemovedScripts = json.optBoolean("lre_workspace_sync_delete_removed_scripts", false);
        boolean workspaceSyncChangesDetermined = json.optBoolean("lre_workspace_sync_changes_determined", false);
        List<String> workspaceSyncChangedFiles = parseStringListOrEmpty(json.opt("lre_workspace_sync_changed_files"));
        List<String> workspaceSyncDeletedFiles = parseStringListOrEmpty(json.opt("lre_workspace_sync_deleted_files"));
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
                workspaceSyncSuccessThresholdPercent,
                workspaceSyncIncremental,
                workspaceSyncDeleteRemovedScripts,
                workspaceSyncChangesDetermined,
                workspaceSyncChangedFiles,
                workspaceSyncDeletedFiles,
                authenticateWithToken,
                lreEnableStacktrace,
                description);
    }

    private static List<String> parseStringListOrEmpty(Object value) {
        List<String> result = new ArrayList<>();
        if (value == null) {
            return result;
        }

        if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            for (int i = 0; i < array.length(); i++) {
                String item = array.optString(i, "").trim();
                if (!item.isEmpty()) {
                    result.add(item);
                }
            }
            return result;
        }

        if (value instanceof String) {
            String textValue = ((String) value).trim();
            if (textValue.isEmpty()) {
                return result;
            }

            String[] items = textValue.split("[\\r\\n,]+");
            for (String item : items) {
                String trimmed = item.trim();
                if (!trimmed.isEmpty()) {
                    result.add(trimmed);
                }
            }
        }

        return result;
    }

    private static int parseIntOrDefault(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            String textValue = ((String) value).trim();
            if (textValue.isEmpty()) {
                return defaultValue;
            }
            try {
                return Integer.parseInt(textValue);
            } catch (NumberFormatException ex) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private static String requireString(JSONObject json, String key) {
        String value = json.optString(key, "").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Missing required config value: " + key);
        }
        return value;
    }
}
