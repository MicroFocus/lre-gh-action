package com.opentext.lre.actions.common.helpers;

import com.microfocus.adm.performancecenter.plugins.common.pcentities.PostRunAction;
import com.opentext.lre.actions.common.helpers.utils.LogHelper;
import com.opentext.lre.actions.runtest.LreTestRunModel;
import org.json.JSONObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class InputRetriever {
    private final String configContent;
    private final JSONObject config;
    private final boolean useConfiguration;
    public InputRetriever(String[] args) throws IOException {
        useConfiguration = args.length > 0;
        if(useConfiguration) {
            // Read the configuration file
            configContent = new String(Files.readAllBytes(Paths.get(args[0])));
            config = new JSONObject(configContent);
        } else {
            configContent = null;
            config = null;
        }
    }

    public LreTestRunModel getLreTestRunModel() throws Exception {
        String lre_action = GetParameterStrValue("lre_action", false, "ExecuteLreTest");
        if("ExecuteLreTest".equalsIgnoreCase(lre_action)) {
            String lre_description = GetParameterStrValue("lre_description", false, "");
            String lre_server = GetParameterStrValue("lre_server", true, "");
            boolean lre_https_protocol = GetParameterBoolValue("lre_https_protocol", false, false);
            boolean lre_authenticate_with_token = GetParameterBoolValue("lre_authenticate_with_token", false, false);
            String lre_username = GetParameterStrValueFromEnvironment("lre_username", true, "");
            String lre_password = GetParameterStrValueFromEnvironment("lre_password", true, "");
            String lre_domain = GetParameterStrValue("lre_domain", true, "");
            String lre_project = GetParameterStrValue("lre_project", true, "");
            String lre_test = GetParameterStrValue("lre_test", true, "");
            String lre_test_instance = GetParameterStrValue("lre_test_instance", false, "AUTO");
            String lre_timeslot_duration_hours = GetParameterStrValue("lre_timeslot_duration_hours", false, "0");
            String lre_timeslot_duration_minutes = GetParameterStrValue("lre_timeslot_duration_minutes", false, "30");
            PostRunAction lre_post_run_action = getPostRunAction("lre_post_run_action");
            boolean lre_vuds_mode = GetParameterBoolValue("lre_vuds_mode", false, false);
            String lre_trend_report = GetParameterStrValue("lre_trend_report", false, "");
            String lre_proxy_out_url = GetParameterStrValue("lre_proxy_out_url", false, "");
            String lre_username_proxy = GetParameterStrValueFromEnvironment("lre_username_proxy", false, "");
            String lre_password_proxy = GetParameterStrValueFromEnvironment("lre_password_proxy", false, "");
            boolean lre_search_timeslot = GetParameterBoolValue("lre_search_timeslot", false, false);
            boolean lre_status_by_sla = GetParameterBoolValue("lre_status_by_sla", false, false);
            String lre_output_dir = GetParameterStrValue("lre_output_dir", false, "./");

            String lre_retry = GetParameterStrValue("lre_retry", false, "1");
            String lre_retry_delay = GetParameterStrValue("lre_retry_delay", false, "1");
            String lre_retry_occurrences = GetParameterStrValue("lre_retry_occurrences", false, "1");
            String lre_trend_report_wait_time = GetParameterStrValue("lre_trend_report_wait_time", false, "0");
            boolean lre_enable_stacktrace = GetParameterBoolValue("lre_enable_stacktrace", false, false);

            String lre_test_to_run = getTestToRun(lre_test);
            String lre_test_content_to_create = lre_test_to_run.equals("CREATE_TEST") ? lre_test : "";
            String lre_test_id = lre_test_to_run.equals("EXISTING_TEST") ? lre_test : "";

            String lre_auto_test_instance = lre_test_instance.equalsIgnoreCase("AUTO") ? "AUTO" : "";
            String lre_test_instance_id = lre_auto_test_instance.equalsIgnoreCase("AUTO") ? "" : lre_test_instance;

            String lre_add_run_to_trend_report = lre_trend_report.equalsIgnoreCase("ASSOCIATED") ? "ASSOCIATED":
                    tryParseIntStrictlyPositive(lre_trend_report) ? "USE_ID" : "";
            String lre_trend_report_id = lre_add_run_to_trend_report.equals("USE_ID") ? lre_trend_report : "";

            if(lre_enable_stacktrace) {
                LogHelper.log("configContent: " + configContent, true);
            }

            return new LreTestRunModel(
                    lre_server,
                    lre_username,
                    lre_password,
                    lre_domain,
                    lre_project,
                    lre_test_to_run,
                    lre_test_id,
                    lre_test_content_to_create,
                    lre_auto_test_instance,
                    lre_test_instance_id,
                    lre_timeslot_duration_hours,
                    lre_timeslot_duration_minutes,
                    lre_post_run_action,
                    lre_vuds_mode,
                    lre_description,
                    lre_add_run_to_trend_report,
                    lre_trend_report_id,
                    lre_https_protocol,
                    lre_proxy_out_url,
                    lre_username_proxy,
                    lre_password_proxy,
                    lre_retry,
                    lre_retry_delay,
                    lre_retry_occurrences,
                    lre_trend_report_wait_time,
                    lre_authenticate_with_token,
                    lre_search_timeslot,
                    lre_status_by_sla,
                    lre_enable_stacktrace,
                    lre_output_dir);
        } else {
            LogHelper.log("configContent: " + configContent, true);
            return null;
        }
    }

    private String GetParameterStrValue(String parameterKey,
                                        boolean isRequired,
                                        String defaultValue) throws Exception {
        if(useConfiguration) {
            return GetParameterStrValueFromConfig(parameterKey, isRequired, defaultValue);
        } else {
            return GetParameterStrValueFromEnvironment(parameterKey, isRequired, defaultValue);
        }
    }

    private String GetParameterStrValueFromEnvironment(String parameterKey,
                                                  boolean isRequired,
                                                  String defaultValue) throws Exception {
        String parameterValue = null;
        try {
            if (parameterKey != null && !parameterKey.isEmpty()) {
                parameterValue = System.getenv(parameterKey);
            }
            if (parameterValue == null || parameterValue.isEmpty()) {
                if (isRequired) {
                    throw new Exception("no value to required parameter '" + parameterKey + "'");
                }
                parameterValue = defaultValue;
            }
            return parameterValue;
        } catch (Exception ex) {
            throw new Exception("unexpected error while getting parameter");
        }
    }

    private String GetParameterStrValueFromConfig(String parameterKey,
                                        boolean isRequired,
                                        String defaultValue) throws Exception {
        String parameterValue = null;
        try {
            if(parameterKey != null && !parameterKey.isEmpty()) {
                parameterValue = config.optString(parameterKey, defaultValue);
            }
            if(parameterValue == null ||  parameterValue.isEmpty()) {
                if(isRequired) {
                    throw new Exception("no value to required parameter '" + parameterKey + "'");
                }
                parameterValue = defaultValue;
            }
            return parameterValue;
        } catch (Exception ex) {
            throw new Exception("unexpected error while getting parameter");
        }
    }

    private boolean GetParameterBoolValue(String parameterKey,
                                          boolean isRequired,
                                          boolean defaultValue) throws Exception {
        if(useConfiguration) {
            return GetParameterBoolValueFromConfig(parameterKey, isRequired, defaultValue);
        } else {
            return GetParameterBoolValueFromEnvironment(parameterKey, isRequired, defaultValue);
        }
    }

    private boolean GetParameterBoolValueFromEnvironment(String parameterKey,
                                                    boolean isRequired,
                                                    boolean defaultValue) throws Exception {
        String parameterValue = null;
        try {
            if (parameterKey != null && !parameterKey.isEmpty()) {
                parameterValue = System.getenv(parameterKey);
            }
            if (parameterValue == null || parameterValue.isEmpty()) {
                if (isRequired) {
                    throw new Exception("no value to required parameter '" + parameterKey + "'");
                }
                return defaultValue;
            }
            return Boolean.parseBoolean(parameterValue);
        } catch (Exception ex) {
            throw new Exception("unexpected error while getting parameter");
        }
    }

    private boolean GetParameterBoolValueFromConfig(String parameterKey,
                                          boolean isRequired,
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

    private PostRunAction getPostRunAction(String lre_post_run_action_param_name) throws Exception {
        String lre_post_run_action_str = GetParameterStrValue(lre_post_run_action_param_name, true, "Collate and Analyze");
        PostRunAction lre_post_run_action = (lre_post_run_action_str.equalsIgnoreCase("Collate Results") ||
                lre_post_run_action_str.equalsIgnoreCase("CollateResults")) ? PostRunAction.COLLATE :
                ((lre_post_run_action_str.equalsIgnoreCase("Collate and Analyze") ||
                        lre_post_run_action_str.equalsIgnoreCase("CollateandAnalyze")) ? PostRunAction.COLLATE_AND_ANALYZE :
                        PostRunAction.DO_NOTHING);
        return lre_post_run_action;
    }

    private String getTestToRun(String lre_test) throws Exception {
        if(tryParseIntStrictlyPositive(lre_test)) {
            return "EXISTING_TEST";
        } else if(lre_test != null && lre_test.toLowerCase().endsWith("yaml")) {
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
                int value = Integer.valueOf(text.trim());
                if(value > 0)
                    return value;
            }
        }
        return null;
    }
}
