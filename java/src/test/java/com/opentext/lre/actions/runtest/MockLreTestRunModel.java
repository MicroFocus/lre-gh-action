package com.opentext.lre.actions.runtest;

import com.microfocus.adm.performancecenter.plugins.common.pcentities.PostRunAction;
import org.apache.commons.httpclient.UsernamePasswordCredentials;

public class MockLreTestRunModel  extends LreTestRunModel {
    public MockLreTestRunModel(String serverAndPort,
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
                               boolean enableStack,
                               String workspace) {
        super(serverAndPort,
                username,
                password,
                domain,
                project,
                testToRun,
                testId,
                testContentToCreate,
                autoTestInstanceID,
                testInstanceId,
                timeslotDurationHours,
                timeslotDurationMinutes,
                postRunAction,
                vudsMode,
                description,
                "NO_TREND",
                null,
                httpsProtocol,
                null,
                null,
                null,
                retry,
                retryDelay,
                retryOccurrences,
                trendReportWaitTime,
                authenticateWithToken,
                searchTimeslot,
                statusBySla,
                enableStack,
                workspace);
    }

}
