package com.opentext.lre.actions.runtest;

import com.microfocus.adm.performancecenter.plugins.common.pcentities.*;
import com.opentext.lre.actions.common.helpers.LocalizationManager;
import com.opentext.lre.actions.common.helpers.constants.LreTestRunHelper;
import com.opentext.lre.actions.common.helpers.result.model.junit.Error;
import com.opentext.lre.actions.common.helpers.result.model.junit.Failure;
import com.opentext.lre.actions.common.helpers.result.model.junit.JUnitTestCaseStatus;
import com.opentext.lre.actions.common.helpers.result.model.junit.Testcase;
import com.opentext.lre.actions.common.helpers.result.model.junit.Testsuite;
import com.opentext.lre.actions.common.helpers.result.model.junit.Testsuites;
import com.opentext.lre.actions.common.helpers.utils.LogHelper;
import com.thoughtworks.xstream.XStream;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.commons.lang3.StringUtils;
import com.opentext.lre.actions.common.helpers.result.model.junit.*;

import java.beans.IntrospectionException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import static com.microfocus.adm.performancecenter.plugins.common.pcentities.RunState.FINISHED;
import static com.microfocus.adm.performancecenter.plugins.common.pcentities.RunState.RUN_FAILURE;
import static com.opentext.lre.actions.common.helpers.constants.LreTestRunHelper.*;

public class LreTestRunBuilder {
    public enum BuildStatus {
        Initiated (100, "Initiated"),
        Running (200, "Running"),
        Aborted (300, "Aborted"),
        Failed(400, "Failed"),
        Successful(500, "Successful");

        private final int value;
        private final String name;

        BuildStatus(int value, String name) {
            this.value = value;
            this.name = name;
        }

        public int getValue() {
            return this.value;
        }

        public String toString() {
            return this.name;
        }
    }

    // <editor-fold defaultstate="collapsed" desc="fields">
    public static final String artifactsResourceName = "LreResult";
    public static final String runReportStructure = "%s/%s/LreReports/HtmlReport";
    public static final String runNVInsightsReportStructure = "%s/%s/LreReports/NVInsights";
    public static final String trendReportStructure = "%s/%s/LreReports/TrendReports";
    public static final String pcReportArchiveName = "Reports.zip";
    public static final String pcNVInsightsReportArchiveName = "NVInsights.zip";
    public static final String pcReportFileName = "Report.html";
    public static final String pcNVInsightsReportFileName = "index.html";
    public static final String TRENDED = "Trended";
    public static final String ERROR = "Error";
    private static final String artifactsDirectoryName = "LreResult";
    private String junitResultsFileName;

    //private transient static Run<?, ?> _run;

    private final String timeslotDurationHours;
    private final String timeslotDurationMinutes;
    private final boolean statusBySLA;
    private LreTestRunModel lreTestRunModel;
    private final String lreServerAndPort;
    private final UsernamePasswordCredentials usernamePasswordCredentialsLre;
    private final String domain;
    private final String project;
    private final String testToRun;
    private final String testId;
    private final String testContentToCreate;
    private final String testInstanceId;
    private final String autoTestInstanceID;
    private final PostRunAction postRunAction;
    private final boolean vudsMode;
    private final String description;
    private final String addRunToTrendReport;
    private final String trendReportId;
    private final boolean httpsProtocol;
    private final String proxyOutURL;
    public final UsernamePasswordCredentials usernamePasswordCredentialsProxy;
    private final String retry;
    private final String retryDelay;
    private final String retryOccurrences;
    private final String trendReportWaitTime;
    private final boolean authenticateWithToken;
    private final boolean searchTimeslot;
    private final boolean enableStackTrace;
    private int runId;
    private String testName;
    private final Path output;
    private final Path workspace;
    private BuildStatus buildStatus;

    public void setBuildStatus(BuildStatus buildStatus) {
        this.buildStatus = buildStatus;
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="constructor">
    public LreTestRunBuilder(
            String lreServerAndPort,
            UsernamePasswordCredentials usernamePasswordCredentialsLre,
            String domain,
            String project,
            String testToRun,
            String testId,
            String testContentToCreate,
            String testInstanceId,
            String autoTestInstanceID,
            String timeslotDurationHours,
            String timeslotDurationMinutes,
            PostRunAction postRunAction,
            boolean vudsMode,
            boolean statusBySLA,
            String description,
            String addRunToTrendReport,
            String trendReportId,
            boolean httpsProtocol,
            String proxyOutURL,
            UsernamePasswordCredentials usernamePasswordCredentialsProxy,
            String retry,
            String retryDelay,
            String retryOccurrences,
            String trendReportWaitTime,
            boolean authenticateWithToken,
            boolean searchTimeslot,
            boolean enableStackTrace,
            String output,
            String workspace) {

        this.lreServerAndPort = getLreServerAndPort(lreServerAndPort);
        this.usernamePasswordCredentialsLre = usernamePasswordCredentialsLre;
        this.domain = domain;
        this.project = project;
        this.testToRun = testToRun;
        this.testId = testId;
        this.testContentToCreate = testContentToCreate;
        this.testInstanceId = testInstanceId;
        this.autoTestInstanceID = autoTestInstanceID;
        this.timeslotDurationHours = timeslotDurationHours;
        this.timeslotDurationMinutes = timeslotDurationMinutes;
        this.postRunAction = postRunAction;
        this.vudsMode = vudsMode;
        this.statusBySLA = statusBySLA;
        this.description = description;
        this.addRunToTrendReport = addRunToTrendReport;
        this.trendReportId = trendReportId;
        this.httpsProtocol = httpsProtocol;
        this.proxyOutURL = proxyOutURL;
        this.usernamePasswordCredentialsProxy = usernamePasswordCredentialsProxy;
        this.retry = (retry == null || retry.isEmpty()) ? "NO_RETRY" : retry;
        this.retryDelay = ("NO_RETRY".equals(this.retry)) ? "0" : (retryDelay == null || retryDelay.isEmpty()) ? "5" : retryDelay;
        this.retryOccurrences = ("NO_RETRY".equals(this.retry)) ? "0" : (retryOccurrences == null || retryOccurrences.isEmpty()) ? "3" : retryOccurrences;
        this.trendReportWaitTime = (trendReportWaitTime != null && !trendReportWaitTime.isEmpty() && LreTestRunHelper.isInteger(trendReportWaitTime)) ? trendReportWaitTime : "0";
        this.authenticateWithToken = authenticateWithToken;
        this.searchTimeslot = searchTimeslot;
        this.output = (output == null) ? null : Paths.get(output);
        this.workspace = Paths.get(workspace);
        this.buildStatus = BuildStatus.Initiated;
        this.enableStackTrace = enableStackTrace;
    }

    private static String getLreServerAndPort(String lreServerAndPort) {
        if(lreServerAndPort.contains("?") && !lreServerAndPort.contains("/?")) {
            return lreServerAndPort.replace("?", "/?");
        }
        return lreServerAndPort;
    }

    public LreTestRunBuilder(LreTestRunModel lreTestRunModel)
    {
        this(lreTestRunModel.getLreServerAndPort(),
                new UsernamePasswordCredentials(lreTestRunModel.getUsername(), lreTestRunModel.getPassword().toCharArray()),
                lreTestRunModel.getDomain(),
                lreTestRunModel.getProject(),
                lreTestRunModel.getTestToRun(),
                lreTestRunModel.getTestId(),
                lreTestRunModel.getTestContentToCreate(),
                lreTestRunModel.getTestInstanceId(),
                lreTestRunModel.getAutoTestInstanceID(),
                lreTestRunModel.getTimeslotDurationHours(),
                lreTestRunModel.getTimeslotDurationMinutes(),
                lreTestRunModel.getPostRunAction(),
                lreTestRunModel.isVudsMode(),
                lreTestRunModel.isStatusBySla(),
                lreTestRunModel.getDescription(),
                lreTestRunModel.getAddRunToTrendReport(),
                lreTestRunModel.getTrendReportId(),
                lreTestRunModel.isHttpsProtocol(),
                lreTestRunModel.getProxyOutURL(),
                new UsernamePasswordCredentials(lreTestRunModel.getUsernameProxy(), lreTestRunModel.getPasswordProxy().toCharArray()),
                lreTestRunModel.getRetry(),
                lreTestRunModel.getRetryDelay(),
                lreTestRunModel.getRetryOccurrences(),
                lreTestRunModel.getTrendReportWaitTime(),
                lreTestRunModel.isAuthenticateWithToken(),
                lreTestRunModel.isSearchTimeslot(),
                lreTestRunModel.isEnableStacktrace(),
                lreTestRunModel.getOutput(),
                lreTestRunModel.getWorkspace());
        this.lreTestRunModel = lreTestRunModel;
        // Set the static flag for stack trace output based on configuration
        LreTestRunHelper.ENABLE_STACKTRACE = lreTestRunModel.isEnableStacktrace();

    }


    // </editor-fold>

    private static String passwordToString(UsernamePasswordCredentials credentials) {
        if (credentials == null) {
            return "";
        }
        char[] password = credentials.getPassword();
        return password == null ? "" : new String(password);
    }

    private static String usernameToString(UsernamePasswordCredentials credentials) {
        return credentials == null ? "" : credentials.getUserName();
    }

    public LreTestRunModel getLreTestRunModel() {
        if (lreTestRunModel == null) {
            lreTestRunModel =
                    new LreTestRunModel(
                            lreServerAndPort.trim(),
                            usernamePasswordCredentialsLre.getUserName(),
                            passwordToString(usernamePasswordCredentialsLre),
                            domain.trim(),
                            project.trim(),
                            testToRun,
                            testId.trim(),
                            testContentToCreate,
                            autoTestInstanceID,
                            testInstanceId.trim(),
                            timeslotDurationHours.trim(),
                            timeslotDurationMinutes.trim(),
                            postRunAction,
                            vudsMode,
                            description,
                            addRunToTrendReport,
                            trendReportId,
                            httpsProtocol,
                            proxyOutURL,
                            usernameToString(usernamePasswordCredentialsProxy),
                            passwordToString(usernamePasswordCredentialsProxy),
                            retry,
                            retryDelay,
                            retryOccurrences,
                            trendReportWaitTime,
                            authenticateWithToken,
                            searchTimeslot,
                            statusBySLA,
                            enableStackTrace,
                            output == null ? "" : output.toString(),
                            workspace.toString());
        }
        return lreTestRunModel;
    }

    public boolean perform() throws InterruptedException, IOException {
        String testToCreate = "";
        String testName = "";
        String testFolderPath = "";
        String fileExtension = "";

        if ("CREATE_TEST".equals(getLreTestRunModel().getTestToRun())) {
            if (verifyStringIsPath(workspace, getLreTestRunModel().getTestContentToCreate())) {
                testName = fileNameWithoutExtension(workspace, getLreTestRunModel().getTestContentToCreate());
                testFolderPath = filePath(getLreTestRunModel().getTestContentToCreate());
                testToCreate = fileContenToString(workspace, getLreTestRunModel().getTestContentToCreate());
                fileExtension = retreiveFileExtension(workspace, getLreTestRunModel().getTestContentToCreate());
            } else {
                testToCreate = getLreTestRunModel().getTestContentToCreate();
            }
        }

        LreTestRunClient lreTestRunClient =
                new LreTestRunClient(getLreTestRunModel(), testToCreate, testName,
                        testFolderPath, fileExtension);

        Testsuites testsuites = execute(lreTestRunClient);

        File resultsFile = resolveResultsFile();

        createResultsFileIfNeeded(resultsFile);

        setBuildStatus(createRunResults(resultsFile, testsuites));
        provideStepResultStatus(buildStatus);

        return BuildStatus.Successful.equals(buildStatus);
    }

    private File resolveResultsFile() {
        File fallbackFile = new File(
                workspace.toString(),
                artifactsResourceName + File.separator + getJunitResultsFileName()
        );

        if (output == null || output.toString().isBlank()) {
            return fallbackFile;
        }

        File preferredFile = new File(
                output.toString(),
                getJunitResultsFileName()
        );

        File parentDir = preferredFile.getParentFile();
        if (parentDir != null && (parentDir.exists() || parentDir.mkdirs())) {
            return preferredFile;
        }

        LogHelper.log(
                "Output path not usable (" + output + "), falling back to workspace.",
                true
        );

        return fallbackFile;
    }

    private void createResultsFileIfNeeded(File resultsFile) throws IOException {
        File parentDir = resultsFile.getParentFile();

        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs() && !parentDir.exists()) {
                throw new IOException("Failed to create directory: " + parentDir);
            }
        }

        if (resultsFile.createNewFile()) {
            LogHelper.log("File created: " + resultsFile.getName(), true);
        } else {
            LogHelper.log("File " + resultsFile.getName() + " already exists.", true);
        }
    }

    private void provideStepResultStatus(BuildStatus buildStatus) {
        String runIdStr =
                (runId > 0) ? String.format(" (PC RunID: %s)", runId) : "";
        LogHelper.log(String.format("%s%s: %s\n====================",
                LocalizationManager.getString("ResultStatus"),
                runIdStr,
                buildStatus.toString()), true);
    }

    private Testsuites run(LreTestRunClient lreTestRunClient)
            throws InterruptedException,
            IOException, PcException {
        PcRunResponse response = null;
        String errorMessage = "";
        String eventLogString = "";
        boolean trendReportReady = false;
        try {
            runId = lreTestRunClient.startRun();
            if (runId == 0)
                return null;
        } catch (NumberFormatException | PcException | IOException ex) {
            LogHelper.log("%s. %s: %s", true, LocalizationManager.getString("StartRunFailed"),
                    LocalizationManager.getString("Error"), ex.getMessage());
            LogHelper.logStackTrace(ex);
            throw ex;
        }
        //getTestName failure should not fail test execution.
        try {
            testName = lreTestRunClient.getTestName();
            if (testName == null) {
                testName = String.format("TestId_%s", getLreTestRunModel().getTestId());
                LogHelper.log("getTestName failed. Using '%s' as testname.", true, testName);
            }
//            else {
//                LogHelper.log("%s '%s'.", true,
//                        LocalizationManager.getString("TestNameIs"), testName);
//            }
        } catch (PcException | IOException ex) {
            testName = String.format("TestId_%s", getLreTestRunModel().getTestId());
            LogHelper.log("getTestName failed. Using '%s' as testname. Error: %s \n", true,
                    testName, ex.getMessage());
            LogHelper.logStackTrace(ex);
        }
        try {
//            publishRunIdVariable(runId);
//            LogHelper.log("%s: %s = %s \n", true,
//                    LocalizationManager.getString("SetEnvironmentVariable"),
//                    RUNID_BUILD_VARIABLE, (Object) runId);
            response = lreTestRunClient.waitForRunCompletion(runId);
            if (response != null && RunState.get(response.getRunState()) == FINISHED &&
                    getLreTestRunModel().getPostRunAction() != PostRunAction.DO_NOTHING) {
                lreTestRunClient.publishRunReport(runId, getReportDirectory());
                lreTestRunClient.publishRunNVInsightsReport(runId,
                        getNVInsightsReportDirectory());
                // Adding the trend report section if ID has been set or if the Associated Trend report is selected.
                if (((("USE_ID").equals(getLreTestRunModel().getAddRunToTrendReport()) &&
                        getLreTestRunModel().getTrendReportId() != null) ||
                        ("ASSOCIATED").equals(getLreTestRunModel().getAddRunToTrendReport())) &&
                        RunState.get(response.getRunState()) != RUN_FAILURE) {
                    Thread.sleep(5000);
                    lreTestRunClient.addRunToTrendReport(this.runId, getLreTestRunModel().getTrendReportId());
                    lreTestRunClient.waitForRunToPublishOnTrendReport(this.runId,
                            getLreTestRunModel().getTrendReportId());
                    int waitTimeInSecondsBeforeRequestingTrendReport =
                            getWaitTimeInSecondsBeforeRequestingTrendReport();
                    if (waitTimeInSecondsBeforeRequestingTrendReport > 0) {
                        String waitTimeBeforeRequestingTrendReportMessage =
                                String.format("Waiting %s seconds before downloading trend report",
                                        waitTimeInSecondsBeforeRequestingTrendReport);
                        LogHelper.log("%s", true,
                                waitTimeBeforeRequestingTrendReportMessage);
                        Thread.sleep(waitTimeInSecondsBeforeRequestingTrendReport * 1000L);
                    }
                    lreTestRunClient.downloadTrendReportAsPdf(getLreTestRunModel().getTrendReportId(),
                            getTrendReportsDirectory());
                    trendReportReady = true;
                }
            } else if (response != null &&
                    RunState.get(response.getRunState()).ordinal() > FINISHED.ordinal()) {
                PcRunEventLog eventLog = lreTestRunClient.getRunEventLog(runId);
                eventLogString = buildEventLogString(eventLog);
            }
        } catch (PcException e) {
            LogHelper.log("Error: %s", true, e.getMessage());
            LogHelper.logStackTrace(e);
        }
        Testsuites ret = new Testsuites();
        if (response != null) {
            parseLreRunResponse(ret, response, errorMessage, eventLogString);
        }
        try {
            parseLreTrendResponse(lreTestRunClient, trendReportReady);
        } catch (IntrospectionException | NoSuchMethodException e) {
            LogHelper.logStackTrace(e);
        }
        return ret;
    }

    private String getNVInsightsReportDirectory() {
        return String.format(
                runNVInsightsReportStructure,
                this.workspace,
                artifactsDirectoryName);
    }

    private void parseLreTrendResponse(
            LreTestRunClient pcTestRunClient,
            boolean trendReportReady)
            throws IntrospectionException, NoSuchMethodException {
        if (trendReportReady) {
            String reportUrlTemp = trendReportStructure.replaceFirst(
                    "%s/", "") + "/trendReport%s.pdf";
            String reportUrl = String.format(reportUrlTemp,
                    artifactsResourceName, getLreTestRunModel().getTrendReportId());
            pcTestRunClient.publishTrendReport(reportUrl, getLreTestRunModel()
                    .getTrendReportId());
        }
    }

    private String getTrendReportsDirectory() {
        return String.format(
                trendReportStructure,
                this.workspace,
                artifactsDirectoryName);
    }

    private String getReportDirectory() {
        return String.format(
                runReportStructure,
                this.workspace,
                artifactsDirectoryName);
    }

    private String buildEventLogString(PcRunEventLog eventLog) {

        String logFormat = "%-5s | %-7s | %-19s | %s\n";
        StringBuilder eventLogStr = new StringBuilder("Event Log:\n\n" + String.format(logFormat,
                "ID", "TYPE", "TIME", "DESCRIPTION"));
        for (PcRunEventLogRecord record : eventLog.getRecordsList()) {
            eventLogStr.append(String.format(logFormat, record.getID(),
                    record.getType(), record.getTime(), record.getDescription()));
        }
        return eventLogStr.toString();
    }

    private int getWaitTimeInSecondsBeforeRequestingTrendReport() {
        try {
            int waitTimeInSecondsBeforeRequestingTrendReport = Integer.parseInt(trendReportWaitTime);
            waitTimeInSecondsBeforeRequestingTrendReport =
                    Math.min(waitTimeInSecondsBeforeRequestingTrendReport, 300);
            waitTimeInSecondsBeforeRequestingTrendReport =
                    Math.max(waitTimeInSecondsBeforeRequestingTrendReport, 0);
            return waitTimeInSecondsBeforeRequestingTrendReport;
        }
        catch (NumberFormatException e) {
            return 0;
        }
    }

    private Testsuites execute(LreTestRunClient lreTestRunClient) {
        try {
            try {
                if (!StringUtils.isBlank(getLreTestRunModel().getDescription())) {
                    LogHelper.log(LocalizationManager.getString("TestDescription")
                            + ": " + getLreTestRunModel().getDescription(), true);
                }
                if (!beforeRun(lreTestRunClient)) {
                    return null;
                }
                return run(lreTestRunClient);
            } catch (InterruptedException e) {
                this.setBuildStatus(BuildStatus.Aborted);
                lreTestRunClient.stopRun(runId);
                throw e;
            } catch (Exception e) {
                LogHelper.logStackTrace(e);
            } finally {
                lreTestRunClient.logout();
            }
            return null;
        }
        catch (Exception e) {
            LogHelper.logStackTrace(e);
            return null;
        }
    }

    private boolean beforeRun(LreTestRunClient pcTestRunClient) {
        //return validateLreForm() && pcTestRunClient.login();
        return pcTestRunClient.login();
    }

    private void parseLreRunResponse(Testsuites ret,
                                           PcRunResponse runResponse,
                                           String errorMessage, String eventLogString)
    {
        List<Testsuite> testSuites = ret.getTestsuite();
        Testsuite testSuite = new Testsuite();
        Testcase testCase = new Testcase();
        //testCase.setClassname("Performance Tests.Test ID: " + runResponse.getTestID());
        testCase.setClassname("Performance Test.Load Test");
        testCase.setName(testName + "(ID:" + runResponse.getTestID() + ")");
        testCase.setTime(String.valueOf(runResponse.getDuration() * 60));
        updateTestStatus(testCase, runResponse, errorMessage, eventLogString);
        testSuite.getTestcase().add(testCase);
        testSuite.setName("Performance Test ID: " + runResponse.getTestID() + ", Run ID: " + runResponse.getID());
        testSuites.add(testSuite);
    }

    private void updateTestStatus(Testcase testCase, PcRunResponse response, String errorMessage, String eventLog) {
        RunState runState = RunState.get(response.getRunState());
        if (runState == RUN_FAILURE) {
            setError(testCase,
                    String.format("%s. %s",
                            runState,
                            errorMessage),
                    eventLog);
        } else if (statusBySLA && runState == FINISHED &&
                !(response.getRunSLAStatus().equalsIgnoreCase("passed"))) {
            setFailure(testCase, "Run measurements did not reach SLA criteria. Run SLA Status: "
                    + response.getRunSLAStatus(), eventLog);
        } else if (runState.hasFailure()) {
            setFailure(testCase,
                    String.format("%s. %s",
                            runState,
                            errorMessage),
                    eventLog);
        } else if (errorMessage != null && !errorMessage.isEmpty()) {
            setFailure(testCase,
                    String.format("%s. %s",
                            runState,
                            errorMessage),
                    eventLog);
        } else {
            testCase.setStatus(JUnitTestCaseStatus.PASS);
        }
    }

    private void setError(Testcase testCase, String message, String eventLog) {
        Error error = new Error();
        error.setMessage(message);
        if (!(eventLog == null || eventLog.isEmpty()))
            testCase.getSystemErr().add(eventLog);
        testCase.getError().add(error);
        testCase.setStatus(JUnitTestCaseStatus.ERROR);
        LogHelper.log(String.format("%s %s",
                message,
                eventLog), true);
    }

    private void setFailure(Testcase testCase, String message, String eventLog) {
        Failure failure = new Failure();
        failure.setMessage(message);
        if (!(eventLog == null || eventLog.isEmpty()))
            testCase.getSystemErr().add(eventLog);
        testCase.getFailure().add(failure);
        testCase.setStatus(JUnitTestCaseStatus.FAILURE);
        LogHelper.log(String.format("Failure: %s %s",
                message,
                eventLog), true);
    }


    private String getJunitResultsFileName() {
        Format formatter = new SimpleDateFormat("ddMMyyyyHHmmssSSS");
        String time = formatter.format(new Date());
        junitResultsFileName = String.format("Results%s.xml", time);
        return junitResultsFileName;
    }

    private BuildStatus createRunResults(File filePath, Testsuites testsuites) {
        BuildStatus testBuildStatus = BuildStatus.Successful;
        try {
            if (testsuites != null) {
                StringWriter writer = testsuitesToStringWriter(testsuites);
                FileWriter fileWriter = new FileWriter(filePath);
                fileWriter.write(writer.toString());
                fileWriter.close();
                if (containsErrorsOrFailures(testsuites.getTestsuite())) {
                    testBuildStatus = BuildStatus.Failed;
                }
            } else {
                LogHelper.log(String.format("%s", LocalizationManager
                        .getString("EmptyResults")), true);
                testBuildStatus = BuildStatus.Failed;
            }
        } catch (Exception cause) {
            LogHelper.log(String.format(
                    "%s. %s: %s",
                    LocalizationManager.getString("FailedToCreateRunResults"),
                    LocalizationManager.getString("Exception"),
                    cause.getMessage()), true);
            testBuildStatus = BuildStatus.Failed;
        }
        return testBuildStatus;
    }

    private StringWriter testsuitesToStringWriter(Testsuites testsuites) {
        StringWriter writer = new StringWriter();
        try {
            JAXBContext context = JAXBContext.newInstance(Testsuites.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.marshal(testsuites, writer);
        } catch (Exception ex) {
            XStream xstream = new XStream();
            xstream.autodetectAnnotations(true);
            xstream.toXML(testsuites, writer);
        }
        return writer;
    }
    private boolean containsErrorsOrFailures(List<Testsuite> testsuites) {
        boolean ret = false;
        for (Testsuite testsuite : testsuites) {
            for (Testcase testcase : testsuite.getTestcase()) {
                String status = testcase.getStatus();
                if (status.equals(JUnitTestCaseStatus.ERROR)
                        || status.equals(JUnitTestCaseStatus.FAILURE)) {
                    ret = true;
                    break;
                }
            }
        }
        return ret;
    }
}
