package com.opentext.lre.actions.runtest;

import com.microfocus.adm.performancecenter.plugins.common.pcentities.PostRunAction;
import com.microfocus.adm.performancecenter.plugins.common.pcentities.*;
import com.microfocus.adm.performancecenter.plugins.common.pcentities.pcsubentities.test.Test;
import com.microfocus.adm.performancecenter.plugins.common.rest.PcRestProxy;
import com.opentext.lre.actions.common.helpers.LocalizationManager;
import com.opentext.lre.actions.common.helpers.constants.LreTestRunConstants;
import com.opentext.lre.actions.common.helpers.utils.LogHelper;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.opentext.lre.actions.common.helpers.constants.LreTestRunHelper.getZipFiles;

public class LreTestRunClient {
    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(LreTestRunClient.class);
    private LreTestRunModel lreTestRunModel;
    private String testToCreate;
    private String testName;
    private String testFolderPath;
    private String fileExtension;
    private PcRestProxy restProxy;
    private boolean loggedIn;
    private int testInstanceID = 0;
    private int timeslotId = -1;

    public LreTestRunClient(LreTestRunModel lreTestRunModel,
                            String testToCreate,
                            String testName,
                            String testFolderPath,
                            String fileExtension) {
        try {
            this.lreTestRunModel = lreTestRunModel;
            this.testToCreate = testToCreate;
            this.testName = testName;
            this.testFolderPath = testFolderPath;
            this.fileExtension = fileExtension;
            restProxy = new PcRestProxy(lreTestRunModel.getProtocol(),
                    lreTestRunModel.getLreServerAndPort(), lreTestRunModel.isAuthenticateWithToken(),
                    lreTestRunModel.getDomain(), lreTestRunModel.getProject(),
                    lreTestRunModel.getProxyOutURL(), lreTestRunModel.getUsernameProxy(),
                    lreTestRunModel.getPasswordProxy());
        } catch (PcException e) {
            LogHelper.log("%s: %s", true,
                    LocalizationManager.getString("Error"),
                    e.getMessage());
        }
    }

    public <T extends PcRestProxy> LreTestRunClient(LreTestRunModel lreTestRunModel, /*PrintStream logger,*/ T proxy) {
        this.lreTestRunModel = lreTestRunModel;
        this.restProxy = proxy;
    }

    public boolean login() {
        try {
            loggedIn = restProxy.authenticate(
                    this.lreTestRunModel.getUsername(),
                    this.lreTestRunModel.getPassword());
        } catch (NullPointerException | PcException | IOException e) {
            LogHelper.logStackTrace("login failed", e);
        }
        return loggedIn;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public int startRun() throws NumberFormatException, PcException, IOException {

        int testID;
        LogHelper.log("", true);
        if ("EXISTING_TEST".equals(lreTestRunModel.getTestToRun())) {
            testID = getTestForExistingTestId();
        } else {
            Test test = createTestFromYamlOrXml();
            if (test == null) {
                LogHelper.log("Could not create test from yaml.", true);
                return 0;
            }
            testID = Integer.parseInt(test.getID());
            lreTestRunModel.setTestId(test.getID());
            LogHelper.log("Running YAML test: Test ID %s, Name: %s, Path: %s", true, test.getID(), test.getName(), test.getTestFolderPath());
        }
        //log(listener, "", true);
        getOpenedTimeslot(testID);
        if (testInstanceID <= 0)
            getCorrectTestInstanceID(testID);
        else {
            LogHelper.log("Test instance already found in the timeslot.", true);
        }
        setCorrectTrendReportID();
        printInitMessage();
        PcRunResponse response = null;
        try {
            response = restProxy.startRun(testID,
                    testInstanceID,
                    new TimeslotDuration(lreTestRunModel.getTimeslotDurationHours(), lreTestRunModel.getTimeslotDurationMinutes()),
                    lreTestRunModel.getPostRunAction().getValue(),
                    lreTestRunModel.isVudsMode(),
                    timeslotId);
            LogHelper.log("%s (TestID: %s, RunID: %s, TimeslotID: %s)", true,
                    LocalizationManager.getString("RunStarted"),
                    (Object) response.getTestID(), (Object) response.getID(), (Object) response.getTimeslotID());

            return response.getID();
        } catch (NumberFormatException | PcException | IOException ex ) {
            LogHelper.log( "%s. %s: %s", true,
                    LocalizationManager.getString("StartRunFailed"),
                    LocalizationManager.getString("Error"),
                    ex.getMessage());
            LogHelper.logStackTrace(ex);
        }
        if (!("RETRY".equals(lreTestRunModel.getRetry()))) {
            return 0;
        } else {
            //counter
            int retryCount = 0;
            //values
            int retryDelay = Integer.parseInt(lreTestRunModel.getRetryDelay());
            int retryOccurrences = Integer.parseInt(lreTestRunModel.getRetryOccurrences());

            while (retryCount <= retryOccurrences) {
                retryCount++;
                try {
                    if (retryCount <= retryOccurrences) {
                        LogHelper.log("%s. %s (%s %s). %s: %s.", true,
                                LocalizationManager.getString("StartRunRetryFailed"),
                                LocalizationManager.getString("AttemptingStartAgainSoon"),
                                (Object) retryDelay,
                                LocalizationManager.getString("Minutes"),
                                LocalizationManager.getString("AttemptsRemaining"),
                                (Object) (retryOccurrences - retryCount + 1));
                        Thread.sleep((long) retryDelay * 60 * 1000);
                    }
                } catch (InterruptedException ex) {
                    LogHelper.log("wait interrupted", true);
                    LogHelper.logStackTrace(ex);
                    return 0;
                }

                response = startRunAgain(testID, testInstanceID, response);
                int ret = (response != null) ? response.getID() : 0;
                if (ret != 0) {
                    LogHelper.log("%s (TestID: %s, RunID: %s, TimeslotID: %s))", true,
                            LocalizationManager.getString("RunStarted"),
                            (Object) response.getTestID(),
                            (Object) response.getID(),
                            (Object) response.getTimeslotID());
                }
                return ret;
            }
        }
        return 0;
    }

    private PcRunResponse startRunAgain(int testID, int testInstance, PcRunResponse response) {
        try {
            response = restProxy.startRun(testID,
                    testInstance,
                    new TimeslotDuration(lreTestRunModel.getTimeslotDurationHours(), lreTestRunModel.getTimeslotDurationMinutes()),
                    lreTestRunModel.getPostRunAction().getValue(),
                    lreTestRunModel.isVudsMode(),
                    -1);
        } catch (NumberFormatException | PcException | IOException ex) {
            LogHelper.log("%s. %s: %s", true,
                    LocalizationManager.getString("StartRunRetryFailed"),
                    LocalizationManager.getString("Error"),
                    ex.getMessage());
            LogHelper.logStackTrace(ex);
        }
        return response;
    }

    private void printInitMessage() {
        LogHelper.log("\n%s \n" +
                        "====================\n" +
                        "%s: %s \n" +
                        "%s: %s \n" +
                        "%s: %s \n" +
                        "%s: %s \n" +
                        "%s: %s \n" +
                        "%s: %s \n" +
                        "%s: %s \n" +
                        "%s: %s \n" +
                        "====================\n",
                false,
                LocalizationManager.getString("ExecutingLoadTest"),
                LocalizationManager.getString("Domain"), lreTestRunModel.getDomain(),
                LocalizationManager.getString("Project"), lreTestRunModel.getProject(),
                LocalizationManager.getString("TestID"),(Object) Integer.parseInt(lreTestRunModel.getTestId()),
                LocalizationManager.getString("TestInstanceID"), (Object) testInstanceID,
                "Timeslot ID", (timeslotId > 0 ? (Object) timeslotId : "Will be created"),
                LocalizationManager.getString("TimeslotDuration"),
                new TimeslotDuration(lreTestRunModel.getTimeslotDurationHours(), lreTestRunModel.getTimeslotDurationMinutes()),
                LocalizationManager.getString("PostRunAction"), lreTestRunModel.getPostRunAction().getValue(),
                LocalizationManager.getString("UseVUDS"), (Object) lreTestRunModel.isVudsMode());
    }

    private Test createTestFromYamlOrXml() throws IOException, PcException {
        Test test = null;
        if (testName.isEmpty())
            test = restProxy.createOrUpdateTestFromYamlTest(testToCreate);
        else {
            switch (fileExtension.toLowerCase()) {
                case LreTestRunConstants.XML_EXTENSION:
                    test = restProxy.createOrUpdateTest(testName, testFolderPath, testToCreate);
                    break;
                case LreTestRunConstants.YAML_EXTENSION:
                case LreTestRunConstants.YML_EXTENSION:
                    test = restProxy.createOrUpdateTestFromYamlContent(testName, testFolderPath, testToCreate);
                    break;
                default:
                    LogHelper.log("File extension not supported.", true);
                    break;
            }
        }
        return test;
    }

    private int getTestForExistingTestId() throws IOException, PcException {
        int testID = Integer.parseInt(lreTestRunModel.getTestId());
        Test test = restProxy.getTest(testID);
        LogHelper.log("Running existing test: Test ID %s, Name: %s, Path: %s", true,
                test.getID(), test.getName(), test.getTestFolderPath());
        return testID;
    }

    private void getOpenedTimeslot(int testID) {
        timeslotId = -1;
        if (lreTestRunModel.isSearchTimeslot()) {
            try {
                LogHelper.log("Searching timeslot", true);
                Timeslots openedTimeslots = restProxy.GetOpenTimeslotsByTestId(testID);
                List<Timeslot> timeslots = openedTimeslots.getTimeslotsList();
                String timeslotIds = timeslots.stream().map(i -> Integer.toString(i.getID())).collect(Collectors.joining(", "));
                String timeslotNames = timeslots.stream().map(Timeslot::getName).collect(Collectors.joining(", "));
                String timeslotTestInstanceIDs = timeslots.stream().map(i -> Integer.toString(i.getLoadTestInstanceID())).collect(Collectors.joining(", "));
                LogHelper.log(
                        "Timeslots related to test ID %s are: timeslot Ids '%s', timeslot names '%s', timeslot TestInstance IDs '%s'.",
                        true,
                        (Object) testID,
                        timeslotIds,
                        timeslotNames,
                        timeslotTestInstanceIDs);
                Stream<Timeslot> timeslotsStream = openedTimeslots.getTimeslotsList().stream().filter((p) -> IsTimeslotPostRunActionValidComparedToRequestedPostRunAction(p.getPostRunAction()));
                ArrayList<Timeslot> timeslotsList = timeslotsStream.collect(Collectors.toCollection(ArrayList::new));
                long timeslotsListCount = timeslotsList.size();

                LogHelper.log(
                        "%s matching timeslot(s) found.",
                        true,
                        (Object) timeslotsListCount);

                if (timeslotsListCount > 0) {
                    Timeslot timeslot = timeslotsList.stream().findFirst().get();
                    timeslotId = timeslot.getID();
                    LogHelper.log(
                            "Found timeslot ID: %s",
                            true,
                            (Object) timeslotId);
                    if (timeslot.getLoadTestInstanceID() > 0) {
                        testInstanceID = timeslot.getLoadTestInstanceID();
                        LogHelper.log(
                                "Using timeslot %s defined to run TestInstance Id %s.",
                                true,
                                (Object) timeslotId,
                                (Object) testInstanceID);
                    }
                }
            } catch (Exception e) {
                LogHelper.log(
                        "getOpenedTimeslot %s. %s: %s", true,
                        LocalizationManager.getString("Failure"),
                        LocalizationManager.getString("Error"),
                        e.getMessage());
                LogHelper.logStackTrace(e);
            }
        }
    }

    private boolean IsTimeslotPostRunActionValidComparedToRequestedPostRunAction(String postRunAction) {
        try {
            TimeslotPostRunAction timeslotPostRUnAction = TimeslotPostRunAction.valueOf(postRunAction);
            PostRunAction requestedPostRunAction = lreTestRunModel.getPostRunAction();

            return requestedPostRunAction == PostRunAction.DO_NOTHING
                    || (requestedPostRunAction == PostRunAction.COLLATE && timeslotPostRUnAction == TimeslotPostRunAction.CollateOnly)
                    || (requestedPostRunAction == PostRunAction.COLLATE && timeslotPostRUnAction == TimeslotPostRunAction.CollateAnalyze)
                    || (requestedPostRunAction == PostRunAction.COLLATE_AND_ANALYZE && timeslotPostRUnAction == TimeslotPostRunAction.CollateAnalyze);
        } catch (Exception ex) {
            return false;
        }
    }

    private void getCorrectTestInstanceID(int testID) throws IOException, PcException {
        if ("AUTO".equals(lreTestRunModel.getAutoTestInstanceID())) {
            try {
                LogHelper.log(LocalizationManager.getString("SearchingTestInstance"), true);
                PcTestInstances pcTestInstances = null;
                try {
                    pcTestInstances = restProxy.getTestInstancesByTestId(testID);
                } catch (PcException ex) {
                    LogHelper.log(
                            "%s - getTestInstancesByTestId %s. Error: %s", true,
                            LocalizationManager.getString("Failure"),
                            LocalizationManager.getString("Error"),
                            ex.getMessage());
                }

                if (pcTestInstances != null && pcTestInstances.getTestInstancesList() != null) {
                    PcTestInstance pcTestInstance = pcTestInstances.getTestInstancesList().get(pcTestInstances.getTestInstancesList().size() - 1);
                    testInstanceID = pcTestInstance.getInstanceId();
                    LogHelper.log(
                            "%s: %s", true,
                            LocalizationManager.getString("FoundTestInstanceID"),
                            (Object) testInstanceID);
                } else {
                    LogHelper.log(LocalizationManager.getString("NotFoundTestInstanceID"), true);
                    LogHelper.log(LocalizationManager.getString("SearchingAvailableTestSet"), true);
                    // Get a random TestSet
                    PcTestSets pcTestSets = restProxy.GetAllTestSets();
                    if (pcTestSets != null && pcTestSets.getPcTestSetsList() != null) {
                        PcTestSet pcTestSet = pcTestSets.getPcTestSetsList().get(pcTestSets.getPcTestSetsList().size() - 1);
                        int testSetID = pcTestSet.getTestSetID();
                        LogHelper.log(
                                "%s (Test ID: %s, TestSet ID: %s", true,
                                LocalizationManager.getString("CreatingNewTestInstance"),
                                (Object) testID,
                                (Object) testSetID);
                        testInstanceID = restProxy.createTestInstance(testID, testSetID);
                        LogHelper.log(
                                "%s: %s", true,
                                LocalizationManager.getString("TestInstanceCreatedSuccessfully"),
                                (Object) testInstanceID);
                    } else {

                        String msg = LocalizationManager.getString("NoTestSetAvailable");
                        LogHelper.log("%s: %s", true,
                                LocalizationManager.getString("Error"),
                                msg);
                        throw new PcException(msg);
                    }
                }
            } catch (Exception e) {
                LogHelper.log(
                        "getCorrectTestInstanceID %s. %s: %s", true,
                        LocalizationManager.getString("Failure"),
                        LocalizationManager.getString("Error"),
                        e.getMessage());
                LogHelper.logStackTrace(e);
                testInstanceID = 0;
                throw e;
            }
        } else
            testInstanceID = Integer.parseInt(lreTestRunModel.getTestInstanceId());
    }

    private void setCorrectTrendReportID() throws IOException, PcException {
        // If the user selected "Use trend report associated with the test" we want the report ID to be the one from the test
        String msg = LocalizationManager.getString("NoTrendReportAssociated") + "\n" +
                LocalizationManager.getString("PleaseTurnAutomaticTrendOn") + "\n" +
                LocalizationManager.getString("PleaseTurnAutomaticTrendOnAlternative");
        if (("ASSOCIATED").equals(lreTestRunModel.getAddRunToTrendReport()) && lreTestRunModel.getPostRunAction() != PostRunAction.DO_NOTHING) {
            PcTest pcTest = restProxy.getTestData(Integer.parseInt(lreTestRunModel.getTestId()));
            //if the trend report ID is parametrized
            if (!lreTestRunModel.getTrendReportId().startsWith("$")) {
                if (pcTest.getTrendReportId() > -1)
                    lreTestRunModel.setTrendReportId(String.valueOf(pcTest.getTrendReportId()));
                else {
                    throw new PcException(msg);
                }
            } else {
                try {
                    if (Integer.parseInt(lreTestRunModel.getTrendReportId()) > -1)
                        lreTestRunModel.setTrendReportId(String.valueOf(lreTestRunModel.getTrendReportId()));
                    else {
                        throw new PcException(msg);
                    }
                } catch (Exception ex) {
                    LogHelper.logStackTrace(ex);
                    throw new PcException(msg + System.getProperty("line.separator") + ex);
                }
            }
        }
    }

    public String getTestName() throws IOException, PcException {

        try {
            PcTest pcTest = restProxy.getTestData(Integer.parseInt(lreTestRunModel.getTestId()));
            return pcTest.getTestName();
        } catch (PcException | IOException ex) {
            LogHelper.log(
                    "getTestData failed for testId : %s", true, lreTestRunModel.getTestId());
            LogHelper.logStackTrace(ex);
            throw ex;
        }
    }

    public PcRunResponse waitForRunCompletion(int runId) throws InterruptedException, PcException, IOException {

        return waitForRunCompletion(runId, 5000);
    }

    public PcRunResponse waitForRunCompletion(int runId, int interval) throws InterruptedException, IOException {
        RunState state;
        switch (lreTestRunModel.getPostRunAction()) {
            case DO_NOTHING:
                state = RunState.BEFORE_COLLATING_RESULTS;
                break;
            case COLLATE:
                state = RunState.BEFORE_CREATING_ANALYSIS_DATA;
                break;
            case COLLATE_AND_ANALYZE:
                state = RunState.FINISHED;
                break;
            default:
                state = RunState.UNDEFINED;
                break;
        }
        return waitForRunState(runId, state, interval);
    }


    private PcRunResponse waitForRunState(int runId, RunState completionState, int interval) throws InterruptedException,
            IOException {

        int counter = 0;
        RunState[] states = {RunState.BEFORE_COLLATING_RESULTS, RunState.BEFORE_CREATING_ANALYSIS_DATA};
        PcRunResponse response = null;
        RunState lastState = RunState.UNDEFINED;
        int threeStrikes = 3;
        do {
            try {

                if (threeStrikes < 3) {
                    LogHelper.log("Cannot get response from PC about the state of RunID: %s %s time(s) consecutively", true,
                            (Object) runId,
                            (Object) (3 - threeStrikes));
                    if (threeStrikes == 0) {
                        LogHelper.log("%s: %s", true,
                                LocalizationManager.getString("StoppingMonitoringOnRun"),
                                (Object) runId);
                        break;
                    }
                    Thread.sleep(2000);
                    login();
                }
                response = restProxy.getRunData(runId);
                RunState currentState = RunState.get(response.getRunState());
                if (lastState.ordinal() < currentState.ordinal()) {
                    lastState = currentState;
                    LogHelper.log("RunID: %s - State = %s", true,
                            (Object) runId,
                            currentState.value());
                }

                // In case we are in state before collate or before analyze, we will wait 1 minute for the state to change otherwise we exit
                // because the user probably stopped the run from PC or timeslot has reached the end.
                if (Arrays.asList(states).contains(currentState)) {
                    counter++;
                    Thread.sleep(1000);
                    if (counter > 60) {
                        LogHelper.log("Run ID: %s  - %s = %s", true,
                                (Object) runId,
                                LocalizationManager.getString("StoppedFromLre"),
                                currentState.value());
                        break;
                    }
                } else {
                    counter = 0;
                    Thread.sleep(interval);
                }
                threeStrikes = 3;
            } catch (PcException e) {
                threeStrikes--;
            } catch (InterruptedException e) {
                LogHelper.log("Job execution interrupted: %s", true,
                        (Object) runId,
                        e.getMessage());
                throw e;
            }
        } while (lastState.ordinal() < completionState.ordinal());
        return response;
    }

    public File publishRunReport(int runId, String reportDirectory) throws IOException, PcException {
        PcRunResults runResultsList = restProxy.getRunResults(runId);
        if (runResultsList.getResultsList() != null) {
            for (PcRunResult result : runResultsList.getResultsList()) {
                if (result.getName().equals(LreTestRunBuilder.pcReportArchiveName)) {
                    File reportFile = getFilePath(runId, reportDirectory, result, false);
                    if (reportFile != null) return reportFile;
                }
            }
        }
        LogHelper.log(LocalizationManager.getString("FailedToGetRunReport"), true);
        return null;
    }

    public File publishRunNVInsightsReport(int runId, String reportDirectory) throws IOException, PcException {
        PcRunResults runResultsList = restProxy.getRunResults(runId);
        if (runResultsList.getResultsList() != null) {
            for (PcRunResult result : runResultsList.getResultsList()) {
                if (result.getName().equals(LreTestRunBuilder.pcNVInsightsReportArchiveName)) {
                    File reportFile = getFilePath(runId, reportDirectory, result, true);
                    if (reportFile != null) return reportFile;
                }
            }
        }
        //LogHelper.log(LocalizationManager.getString("FailedToGetRunNVInsightsReport"),true);
        return null;
    }

    private File getFilePath(int runId, String reportDirectory, PcRunResult result, boolean nvInsights)
            throws IOException, PcException {
        File dir = new File(reportDirectory);
        if(!dir.exists()) {
            dir.mkdirs();
        }
        String reportArchiveFullPath = dir.getCanonicalPath() + IOUtils.DIR_SEPARATOR +
                (nvInsights ? LreTestRunBuilder.pcNVInsightsReportArchiveName :
                        LreTestRunBuilder.pcReportArchiveName);
        try {
            restProxy.GetRunResultData(runId, result.getID(), reportArchiveFullPath);
        } catch (PcException ex) {
            if (!nvInsights)
                throw ex;
            else
                return null;
        }
        File fp = new File(reportArchiveFullPath);
        getZipFiles(reportArchiveFullPath, fp.getParent());
//        fp.delete();
        File reportFile = nvInsights ?
                new File(Paths.get(dir.getAbsolutePath(), LreTestRunBuilder.pcNVInsightsReportFileName).toString()) :
                new File(Paths.get(dir.getAbsolutePath(), LreTestRunBuilder.pcReportFileName).toString());
        if (reportFile.exists()) {
            LogHelper.log((nvInsights ?
                            LocalizationManager.getString("PublishingNVInsightsReport") :
                            LocalizationManager.getString("PublishingAnalysisReport")),
                    true);
            LogHelper.log(reportFile.toString(), false);
            return reportFile;
        }
        return null;
    }

    public void logout() {
        if (!loggedIn)
            return;

        boolean logoutSucceeded = false;
        try {
            logoutSucceeded = restProxy.logout();
            loggedIn = !logoutSucceeded;
        } catch (PcException | IOException e) {
            LogHelper.log("%s: %s", true,
                    LocalizationManager.getString("Error"),
                    e.getMessage());
            LogHelper.logStackTrace(e);
        }
        LogHelper.log("%s", true,
                logoutSucceeded ?
                        LocalizationManager.getString("LogoutSucceeded") :
                        LocalizationManager.getString("LogoutFailed"));
    }

    public void stopRun(int runId) {
        boolean stopRunSucceeded = false;
        try {
            LogHelper.log("%s", true,
                    LocalizationManager.getString("StoppingRun"));
            stopRunSucceeded = restProxy.stopRun(runId, "stop");
        } catch (PcException | IOException e) {
            LogHelper.log("%s: %s", true,
                    LocalizationManager.getString("Error"),
                    e.getMessage());
            LogHelper.logStackTrace(e);
        }
        LogHelper.log("%s", true,
                stopRunSucceeded ?
                        LocalizationManager.getString("StopRunSucceeded") :
                        LocalizationManager.getString("StopRunFailed"));
    }

    public PcRunEventLog getRunEventLog(int runId) {
        try {
            return restProxy.getRunEventLog(runId);
        } catch (PcException | IOException e) {
            LogHelper.log("%s: %s", true,
                    LocalizationManager.getString("Error"),
                    e.getMessage());
            LogHelper.logStackTrace(e);
        }
        return null;
    }

    public void addRunToTrendReport(int runId, String trendReportId) {

        TrendReportRequest trRequest = new TrendReportRequest(lreTestRunModel.getProject(), runId, null);
        LogHelper.log("Adding run: %s to trend report: %s", true,
                (Object) runId,
                trendReportId);
        try {
            restProxy.updateTrendReport(trendReportId, trRequest);
            LogHelper.log("%s: %s %s: %s", true,
                    LocalizationManager.getString("PublishingRun"),
                    (Object) runId,
                    LocalizationManager.getString("OnTrendReport"),
                    trendReportId);
        } catch (PcException e) {
            LogHelper.log( "%s: %s", true,
                    LocalizationManager.getString("FailedToAddRunToTrendReport"),
                    e.getMessage());
            LogHelper.logStackTrace(e);
        } catch (IOException e) {
            LogHelper.log("%s: %s.", true,
                    LocalizationManager.getString("FailedToAddRunToTrendReport"),
                    LocalizationManager.getString("ProblemConnectingToPCServer"));
            LogHelper.logStackTrace(e);
        }
    }

    public void waitForRunToPublishOnTrendReport(int runId, String trendReportId) throws PcException, IOException, InterruptedException {

        ArrayList<PcTrendedRun> trendReportMetaDataResultsList;
        boolean publishEnded = false;
        int counterPublishStarted = 0;
        int counterPublishNotStarted = 0;
        boolean resultNotFound = true;

        do {
            trendReportMetaDataResultsList = restProxy.getTrendReportMetaData(trendReportId);

            if (trendReportMetaDataResultsList.isEmpty()) break;

            for (PcTrendedRun result : trendReportMetaDataResultsList) {
                resultNotFound = result.getRunID() != runId;
                if (resultNotFound) continue;

                if (result.getState().equals(LreTestRunBuilder.TRENDED) || result.getState().equals(LreTestRunBuilder.ERROR)) {
                    publishEnded = true;
                    LogHelper.log("Run: %s %s: %s", true,
                            (Object) runId,
                            LocalizationManager.getString("PublishingStatus"),
                            result.getState());
                    break;
                } else {
                    Thread.sleep(5000);
                    counterPublishStarted++;
                    if (counterPublishStarted >= 360) {
                        String msg = String.format("%s: %s",
                                LocalizationManager.getString("Error"),
                                LocalizationManager.getString("PublishingEndTimeout"));
                        throw new PcException(msg);
                    }
                }
            }
            if (!publishEnded && resultNotFound) {
                Thread.sleep(5000);
                counterPublishNotStarted++;
                if (counterPublishNotStarted >= 120) {
                    String msg = String.format("%s: %s",
                            LocalizationManager.getString("Error"),
                            LocalizationManager.getString("PublishingStartTimeout"));
                    throw new PcException(msg);
                }
            }
        } while (!publishEnded && counterPublishStarted < 120);
    }

    public void downloadTrendReportAsPdf(String trendReportId, String directory) throws PcException {
        try {
            LogHelper.log("%s: %s %s", true,
                    LocalizationManager.getString("DownloadingTrendReport"),
                    trendReportId,
                    LocalizationManager.getString("InPDFFormat"));
            InputStream in = restProxy.getTrendingPDF(trendReportId);
            File dir = new File(directory);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String filePath = directory + IOUtils.DIR_SEPARATOR + "trendReport" + trendReportId + ".pdf";
            Path destination = Paths.get(filePath);
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
            LogHelper.log("%s: %s %s", true,
                    LocalizationManager.getString("TrendReport"),
                    trendReportId,
                    LocalizationManager.getString("SuccessfullyDownloaded"));
        } catch (Exception e) {
            LogHelper.log("%s: %s", true,
                    LocalizationManager.getString("FailedToDownloadTrendReport"),
                    e.getMessage());
            LogHelper.logStackTrace(e);
            throw new PcException(e.getMessage());
        }
    }

    public void publishTrendReport(String filePath, String trendReportId) {

        if (filePath == null) {
            return;
        }
        String message = String.format( LocalizationManager.getString("ViewTrendReport") + " " + trendReportId + " under " + filePath);
        LogHelper.log(message, false);

    }


    // This method will return a map with the following structure: <transaction_name:selected_measurement_value>
    // for example:
    // <Action_Transaction:0.001>
    // <Virtual transaction 2:0.51>
    // This function uses reflection since we know only at runtime which transactions data will be reposed from the rest request.
    public Map<String, String> getTrendReportByXML(String trendReportId, int runId,
                                                   TrendReportTypes.DataType dataType,
                                                   TrendReportTypes.PctType pctType,
                                                   TrendReportTypes.Measurement measurement)
            throws IOException, PcException {
        Map<String, String> measurmentsMap = new LinkedHashMap<String, String>();
        measurmentsMap.put("RunId", "_" + runId + "_");
        measurmentsMap.put("Trend Measurement Type", measurement.toString() + "_" + pctType.toString());
        TrendReportTransactionDataRoot res = restProxy.getTrendReportByXML(trendReportId, runId);
        List<Object> RowsListObj = res.getTrendReportRoot();
        if (RowsListObj != null) {
            for (Object o : RowsListObj) {
                try {
                    java.lang.reflect.Method rowListMethod = o.getClass().getMethod("getTrendReport" + dataType.toString() + "DataRowList");
                    for (Object DataRowObj : (ArrayList<Object>) rowListMethod.invoke(o)) {
                        if (DataRowObj.getClass().getMethod("getPCT_TYPE").invoke(DataRowObj).equals(pctType.toString())) {
                            java.lang.reflect.Method method;
                            method = DataRowObj.getClass().getMethod("get" + measurement);
                            measurmentsMap.put(DataRowObj.getClass().getMethod("getPCT_NAME").invoke(DataRowObj).toString(), method.invoke(DataRowObj) == null ? "" : method.invoke(DataRowObj).toString());
                        }
                    }
                } catch (NoSuchMethodException e) {
                    LogHelper.log("No such method exception: " + e.getMessage(), true);
                    LogHelper.logStackTrace(e);
                } catch (Exception e) {
                    LogHelper.log(" Error on getTrendReportByXML: %s ", true, e.getMessage());
                    LogHelper.logStackTrace(e);
                }
            }
        }
        return measurmentsMap;
    }
}
