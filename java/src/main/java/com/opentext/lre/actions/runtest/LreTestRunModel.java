package com.opentext.lre.actions.runtest;

import com.microfocus.adm.performancecenter.plugins.common.pcentities.PostRunAction;
import com.opentext.lre.actions.common.helpers.constants.LreTestRunHelper;

public class LreTestRunModel {

    // <editor-fold default-state="collapsed" desc="fields">
    private final String lreServerAndPort;
    private final String username;
    private final String password;
    private final String domain;
    private final String project;
    private final String testToRun;
    private final String testContentToCreate;
    private final String autoTestInstanceID;
    private final PostRunAction postRunAction;
    private final boolean vudsMode;
    private final String description;
    private final String addRunToTrendReport;
    private final boolean httpsProtocol;
    private final String proxyOutURL;
    private final String usernameProxy;
    private final String passwordProxy;
    private final boolean authenticateWithToken;
    private final boolean searchTimeslot;
    private String testId;
    private String testInstanceId;
    private String trendReportId;
    private String buildParameters;
    private String retry;
    private String retryDelay;
    private String retryOccurrences;
    private String trendReportWaitTime;
    private String timeslotDurationHours;
    private String timeslotDurationMinutes;
    private boolean statusBySla;
    private boolean enableStacktrace;
    private String workspace;

    // </editor-fold>

    // <editor-fold default-state="collapsed" desc="Constructor">
    public LreTestRunModel(String lreServerAndPort,
                           String username,
                           String password,
                           String domain,
                           String project,
                           String testToRun,
                           String testId,
                           String testContentToCreate,
                           String autoTestInstanceID,
                           String testInstanceId,
                           String timeslotDurationHours,
                           String timeslotDurationMinutes,
                           PostRunAction postRunAction,
                           boolean vudsMode,
                           String description,
                           String addRunToTrendReport,
                           String trendReportId,
                           boolean httpsProtocol,
                           String proxyOutURL,
                           String usernameProxy,
                           String passwordProxy,
                           String retry,
                           String retryDelay,
                           String retryOccurrences,
                           String trendReportWaitTime,
                           boolean authenticateWithToken,
                           boolean searchTimeslot,
                           boolean statusBySla,
                           boolean enableStacktrace,
                           String workspace) {
        this.lreServerAndPort = getCorrectLreServerAndPort(lreServerAndPort);
        this.username = username;
        this.password = password;
        this.domain = domain;
        this.project = project;
        this.testToRun = testToRun;
        this.testId = testId;
        this.testContentToCreate = testContentToCreate;
        this.autoTestInstanceID = autoTestInstanceID;
        this.testInstanceId = testInstanceId;
        this.timeslotDurationHours = timeslotDurationHours;
        this.timeslotDurationMinutes = timeslotDurationMinutes;
        this.postRunAction = postRunAction;
        this.vudsMode = vudsMode;
        this.description = description;
        this.addRunToTrendReport = addRunToTrendReport;
        this.httpsProtocol = httpsProtocol;
        this.trendReportId = trendReportId;
        this.proxyOutURL = proxyOutURL;
        this.usernameProxy = usernameProxy;
        this.passwordProxy = passwordProxy;
        this.buildParameters = "";
        this.retry = retry;
        this.retryDelay = LreTestRunHelper.verifyStringValueIsIntAndPositive(retryDelay, 5);
        this.retryOccurrences = LreTestRunHelper.verifyStringValueIsIntAndPositive(retryOccurrences, 3);
        this.trendReportWaitTime = LreTestRunHelper.verifyStringValueIsIntAndPositive(trendReportWaitTime, 0);
        this.authenticateWithToken = authenticateWithToken;
        this.searchTimeslot = searchTimeslot;
        this.statusBySla = statusBySla;
        this.enableStacktrace = enableStacktrace;
        this.workspace = workspace;
    }

    private String getCorrectLreServerAndPort(String lreServerAndPort) {
        if(lreServerAndPort.contains("?") && !lreServerAndPort.contains("/?")) {
            return lreServerAndPort.replace("?", "/?");
        }
        return lreServerAndPort;
    }
    // </editor-fold>

    // <editor-fold default-state="collapsed" desc="helper">
    public String getRetry() { return this.retry; }
    public String getRetryDelay() {
        return this.retryDelay;
    }
    public String getRetryOccurrences() { return this.retryOccurrences; }
    public String getTrendReportWaitTime() { return this.trendReportWaitTime; }
    public String getLreServerAndPort() { return this.lreServerAndPort; }
    public String getUsername() { return this.username; }
    public String getPassword() { return this.password; }
    public String getUsernameProxy() { return this.usernameProxy; }
    public String getPasswordProxy() { return this.passwordProxy; }
    public String getTestToRun() {
        return this.testToRun;
    }
    public String getTestContentToCreate() { return this.testContentToCreate; }
    public String getDomain() { return this.domain; }
    public String getProject() { return this.project; }
    public String getTestId() { return this.testId; }
    public void setTestId(String testId) { this.testId = testId; }
    public String getTestInstanceId() { return this.testInstanceId; }
    public String getAutoTestInstanceID() {
        return this.autoTestInstanceID;
    }
    public String getTimeslotDurationHours() { return this.timeslotDurationHours; }
    public String getTimeslotDurationMinutes() { return this.timeslotDurationMinutes; }
    public boolean isVudsMode() { return this.vudsMode; }
    public PostRunAction getPostRunAction() { return this.postRunAction; }
    public String getDescription() { return this.description; }
    public boolean isHttpsProtocol() { return this.httpsProtocol; }
    public String getProxyOutURL() {
        return this.proxyOutURL;
    }
    public String getBuildParameters() { return this.buildParameters; }
    public void setBuildParameters(String buildParameters) { this.buildParameters = buildParameters; }
    public String getTrendReportId() {
        return trendReportId;
    }
    public void setTrendReportId(String trendReportId) {
        this.trendReportId = trendReportId;
    }
    public String getAddRunToTrendReport() {
        return this.addRunToTrendReport;
    }
    public String getProtocol() { return httpsProtocol ? "https" : "http"; }
    public boolean isAuthenticateWithToken() {
        return this.authenticateWithToken;
    }
    public boolean isSearchTimeslot() {
        return this.searchTimeslot;
    }

    public void setStatusBySla(boolean statusBySla) { this.statusBySla = statusBySla; }
    public boolean isStatusBySla() {return this.statusBySla; }

    public void setEnableStacktrace(boolean enableStacktrace) { this.enableStacktrace = enableStacktrace; }
    public boolean isEnableStacktrace() {return this.enableStacktrace; }

    public void setWorkspace(String workspace) { this.workspace = workspace; }
    public String getWorkspace() { return this.workspace; }


    @Override
    public String toString() { return String.format("%s", runParamsToString().substring(1)); }
    public String runParamsToString() {
        String vudsModeString = (vudsMode) ? "true" : "false";
        String trendString = ("USE_ID").equals(addRunToTrendReport) ? String.format(", TrendReportID = '%s'", trendReportId) : "";
        return String.format("[lreServerAndPort='%s', username='%s', domain='%s', project='%s', TestID='%s', " +
                        "TestInstanceID='%s', TimeslotDurationHours='%s', TimeslotDurationMinutes='%s', PostRunAction='%s', " +
                        "VUDsMode='%s', trending='%s', HTTPSProtocol='%s', authenticateWithToken='%s']",
                lreServerAndPort, username, domain, project, testId,
                testInstanceId, timeslotDurationHours, timeslotDurationMinutes, postRunAction.getValue(),
                vudsModeString, trendString, httpsProtocol, authenticateWithToken);
    }
    // </editor-fold>
}
