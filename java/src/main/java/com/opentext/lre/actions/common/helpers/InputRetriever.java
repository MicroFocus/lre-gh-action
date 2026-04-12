package com.opentext.lre.actions.common.helpers;

import com.microfocus.adm.performancecenter.plugins.common.pcentities.PostRunAction;
import com.opentext.lre.actions.runtest.LreTestRunModel;
import com.opentext.lre.actions.workspacesync.LreWorkspaceSyncModel;
import org.json.JSONObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class InputRetriever {
    private static final String DEFAULT_LRE_ACTION = "ExecuteLreTest";
    private final JSONObject config;
    private final boolean useConfiguration;

    public InputRetriever(String[] args) throws IOException {
        useConfiguration = args.length > 0;
        if(useConfiguration) {
            // Read the configuration file
            String configContent = new String(Files.readAllBytes(Paths.get(args[0])));
            config = new JSONObject(configContent);
        } else {
            config = null;
        }
    }

    /**
     * Inner class to hold common LRE parameters shared by all models
     */
    private static class CommonLreParameters {
        String lreServer;
        boolean lreHttpsProtocol;
        boolean lreAuthenticateWithToken;
        boolean lreEnableStacktrace;
        String lreUsername;
        String lrePassword;
        String lreDomain;
        String lreProject;
        String lreProxyOutUrl;
        String lreUsernameProxy;
        String lrePasswordProxy;
        String lreWorkspaceDir;
        String lreDescription;
    }

    /**
     * Retrieves common LRE parameters shared by all models
     */
    private CommonLreParameters getCommonLreParameters() throws Exception {
        CommonLreParameters params = new CommonLreParameters();
        params.lreServer = getParameterStrValue("lre_server", true, "");
        params.lreHttpsProtocol = getParameterBoolValue("lre_https_protocol", true);
        params.lreAuthenticateWithToken = getParameterBoolValue("lre_authenticate_with_token", false);
        params.lreEnableStacktrace = getParameterBoolValue("lre_enable_stacktrace", false);
        params.lreUsername = getParameterStrValueFromEnvironment("lre_username", true, "");
        params.lrePassword = getParameterStrValueFromEnvironment("lre_password", true, "");
        params.lreDomain = getParameterStrValue("lre_domain", true, "");
        params.lreProject = getParameterStrValue("lre_project", true, "");
        params.lreProxyOutUrl = getParameterStrValue("lre_proxy_out_url", false, "");
        params.lreUsernameProxy = getParameterStrValueFromEnvironment("lre_username_proxy", false, "");
        params.lrePasswordProxy = getParameterStrValueFromEnvironment("lre_password_proxy", false, "");
        params.lreWorkspaceDir = getParameterStrValueFromConfigOrFromEnvironment("lre_workspace_dir", true);
        params.lreDescription = getParameterStrValue("lre_description", false, "");
        return params;
    }

    /**
     * Gets the action to execute from configuration or environment
     * @return the action name (e.g., "ExecuteLreTest", "WorkspaceSync")
     */
    public String getLreAction() throws Exception {
        return getParameterStrValue("lre_action", false, DEFAULT_LRE_ACTION);
    }

    public LreTestRunModel getLreTestRunModel() throws Exception {
        String lre_action = getLreAction();
        if("ExecuteLreTest".equalsIgnoreCase(lre_action)) {
            // Get common parameters
            CommonLreParameters common = getCommonLreParameters();

            // Get test-specific parameters
            String lre_test = getParameterStrValue("lre_test", true, "");
            String lre_test_instance = getParameterStrValue("lre_test_instance", false, "AUTO");
            String lre_timeslot_duration_hours = getParameterStrValue("lre_timeslot_duration_hours", false, "0");
            String lre_timeslot_duration_minutes = getParameterStrValue("lre_timeslot_duration_minutes", false, "30");
            PostRunAction lre_post_run_action = getPostRunAction();
            boolean lre_vuds_mode = getParameterBoolValue("lre_vuds_mode", false);
            String lre_trend_report = getParameterStrValue("lre_trend_report", false, "");
            boolean lre_search_timeslot = getParameterBoolValue("lre_search_timeslot", false);
            boolean lre_status_by_sla = getParameterBoolValue("lre_status_by_sla", false);
            String lre_output_dir = getParameterStrValueFromConfigOrFromEnvironment("lre_output_dir", false) ;
            String lre_retry = getParameterStrValue("lre_retry", false, "1");
            String lre_retry_delay = getParameterStrValue("lre_retry_delay", false, "1");
            String lre_retry_occurrences = getParameterStrValue("lre_retry_occurrences", false, "1");
            String lre_trend_report_wait_time = getParameterStrValue("lre_trend_report_wait_time", false, "0");

            String lre_test_to_run = getTestToRun(lre_test);
            String lre_test_content_to_create = lre_test_to_run.equals("CREATE_TEST") ? lre_test : "";
            String lre_test_id = lre_test_to_run.equals("EXISTING_TEST") ? lre_test : "";

            String lre_auto_test_instance = lre_test_instance.equalsIgnoreCase("AUTO") ? "AUTO" : "";
            String lre_test_instance_id = lre_auto_test_instance.equalsIgnoreCase("AUTO") ? "" : lre_test_instance;

            String lre_add_run_to_trend_report = lre_trend_report.equalsIgnoreCase("ASSOCIATED") ? "ASSOCIATED":
                    tryParseIntStrictlyPositive(lre_trend_report) ? "USE_ID" : "";
            String lre_trend_report_id = lre_add_run_to_trend_report.equals("USE_ID") ? lre_trend_report : "";

            return new LreTestRunModel(
                    common.lreServer,
                    common.lreUsername,
                    common.lrePassword,
                    common.lreDomain,
                    common.lreProject,
                    lre_test_to_run,
                    lre_test_id,
                    lre_test_content_to_create,
                    lre_auto_test_instance,
                    lre_test_instance_id,
                    lre_timeslot_duration_hours,
                    lre_timeslot_duration_minutes,
                    lre_post_run_action,
                    lre_vuds_mode,
                    common.lreDescription,
                    lre_add_run_to_trend_report,
                    lre_trend_report_id,
                    common.lreHttpsProtocol,
                    common.lreProxyOutUrl,
                    common.lreUsernameProxy,
                    common.lrePasswordProxy,
                    lre_retry,
                    lre_retry_delay,
                    lre_retry_occurrences,
                    lre_trend_report_wait_time,
                    common.lreAuthenticateWithToken,
                    lre_search_timeslot,
                    lre_status_by_sla,
                    common.lreEnableStacktrace,
                    lre_output_dir,
                    common.lreWorkspaceDir);
        } else {
            return null;
        }
    }

    /**
     * Gets the LreWorkspaceSyncModel from configuration or environment variables
     * @return LreWorkspaceSyncModel if lre_action is "WorkspaceSync", null otherwise
     */
    public LreWorkspaceSyncModel getLreWorkspaceSyncModel() throws Exception {
        String lre_action = getLreAction();
        if("WorkspaceSync".equalsIgnoreCase(lre_action)) {
            // Get common parameters
            CommonLreParameters common = getCommonLreParameters();

            // Get workspace sync-specific parameters
            boolean lre_runtime_only = getParameterBoolValue("lre_runtime_only", true);

            return new LreWorkspaceSyncModel(
                    common.lreServer,
                    common.lreHttpsProtocol,
                    common.lreUsername,
                    common.lrePassword,
                    common.lreDomain,
                    common.lreProject,
                    common.lreProxyOutUrl,
                    common.lreUsernameProxy,
                    common.lrePasswordProxy,
                    common.lreWorkspaceDir,
                    lre_runtime_only,
                    common.lreAuthenticateWithToken,
                    common.lreEnableStacktrace,
                    common.lreDescription);
        } else {
            return null;
        }
    }

    private String getParameterStrValueFromConfigOrFromEnvironment(String parameterKey,
                                                                   boolean isRequired) throws Exception {
        String parameterValue;
        try {
            parameterValue = getParameterStrValue(parameterKey, isRequired, "");
        } catch (Exception ex) {
            throw new Exception("GetParameterStrValueFromConfigOrFromEnvironment: failed to get parameter " + parameterKey, ex);
        }
        if (parameterValue == null || parameterValue.isEmpty()) {
            String parameterFromEnvironmentValue;
            try {
                parameterFromEnvironmentValue = getParameterStrValueFromEnvironment("GITHUB_WORKSPACE", isRequired, "");
                return parameterFromEnvironmentValue;
            } catch (Exception ex) {
                throw new Exception("unexpected error while getting parameter '" + parameterKey + "' or parameter from environment 'GITHUB_WORKSPACE'", ex);
            }
        } else {
            return parameterValue;
        }
    }


    private String getParameterStrValue(String parameterKey,
                                        boolean isRequired,
                                        String defaultValue) throws Exception {
        if(useConfiguration) {
            return getParameterStrValueFromConfig(parameterKey, isRequired, defaultValue);
        } else {
            return getParameterStrValueFromEnvironment(parameterKey, isRequired, defaultValue);
        }
    }

    private String getParameterStrValueFromEnvironment(String parameterKey,
                                                  boolean isRequired,
                                                  String defaultValue) throws Exception {
        String parameterValue;
        try {
            parameterValue = System.getenv(parameterKey);
            if (parameterValue == null || parameterValue.isEmpty()) {
                if (isRequired) {
                    throw new Exception("no value to required parameter '" + parameterKey + "'");
                }
                parameterValue = defaultValue;
            }
            return parameterValue;
        } catch (Exception ex) {
            throw new Exception("unexpected error while getting parameter '" + parameterKey + "'");
        }
    }

    private String getParameterStrValueFromConfig(String parameterKey,
                                        boolean isRequired,
                                        String defaultValue) throws Exception {
        String parameterValue = defaultValue;
        try {
            if(parameterKey != null && !parameterKey.isEmpty()) {
                parameterValue = config.optString(parameterKey, defaultValue);
            }
            if(parameterValue == null ||  parameterValue.isEmpty()) {
                if(isRequired) {
                    parameterValue = System.getenv(parameterKey);
                    if(parameterValue == null ||  parameterValue.isEmpty()) {
                    throw new Exception("no value to required parameter '" + parameterKey + "'");
                    }
                }
                parameterValue = defaultValue;
            }
            return parameterValue;
        } catch (Exception ex) {
            throw new Exception("unexpected error while getting parameter'" + parameterKey + "'");
        }
    }

    private boolean getParameterBoolValue(String parameterKey,
                                          boolean defaultValue) throws Exception {
        if(useConfiguration) {
            return getParameterBoolValueFromConfig(parameterKey, defaultValue);
        } else {
            return getParameterBoolValueFromEnvironment(parameterKey, defaultValue);
        }
    }

    private boolean getParameterBoolValueFromEnvironment(String parameterKey,
                                                    boolean defaultValue) throws Exception {
        String parameterValue = "";
        try {

            if (parameterKey != null && !parameterKey.isEmpty()) {
                parameterValue = System.getenv(parameterKey);
            }
            if (parameterValue == null || parameterValue.isEmpty()) {
                return defaultValue;
            }
            return Boolean.parseBoolean(parameterValue);
        } catch (Exception ex) {
            throw new Exception("unexpected error while getting parameter");
        }
    }

    private boolean getParameterBoolValueFromConfig(String parameterKey,
                                          boolean defaultValue) throws Exception {
        boolean parameterValue = defaultValue;
        try {
            if(parameterKey != null && !parameterKey.isEmpty()) {
                parameterValue = config.optBoolean(parameterKey, defaultValue);
            }
            return parameterValue;
        } catch (Exception ex) {
            throw new Exception("unexpected error while getting parameter");
        }
    }

    private PostRunAction getPostRunAction() throws Exception {
        String lre_post_run_action_str = getParameterStrValue("lre_post_run_action", true, "Collate and Analyze");
        return (lre_post_run_action_str.equalsIgnoreCase("Collate Results") ||
                lre_post_run_action_str.equalsIgnoreCase("CollateResults")) ? PostRunAction.COLLATE :
                ((lre_post_run_action_str.equalsIgnoreCase("Collate and Analyze") ||
                        lre_post_run_action_str.equalsIgnoreCase("CollateandAnalyze")) ? PostRunAction.COLLATE_AND_ANALYZE :
                        PostRunAction.DO_NOTHING);
    }

    private String getTestToRun(String lre_test) {
        if(tryParseIntStrictlyPositive(lre_test)) {
            return "EXISTING_TEST";
        } else if(lre_test != null && (lre_test.toLowerCase().endsWith("yaml") || lre_test.toLowerCase().endsWith("yml"))) {
            return "CREATE_TEST";
        }
        return "";
    }

    private static boolean tryParseIntStrictlyPositive(String text) {
        return ParseIntStrictlyPositive(text) != null;
    }

    public static Integer ParseIntStrictlyPositive(String text) {
        if (text != null && !text.isEmpty()) {
            if (text.trim().matches("[0-9]+")) {
                int value = Integer.parseInt(text.trim());
                if(value > 0)
                    return value;
            }
        }
        return null;
    }
}
