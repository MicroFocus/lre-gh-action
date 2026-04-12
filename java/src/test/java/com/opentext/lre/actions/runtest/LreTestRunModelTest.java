package com.opentext.lre.actions.runtest;

import com.microfocus.adm.performancecenter.plugins.common.pcentities.PostRunAction;
import junit.framework.TestCase;

public class LreTestRunModelTest extends TestCase {
    private LreTestRunModel createModel(String serverAndPort,
                                        boolean httpsProtocol,
                                        String retryDelay,
                                        String retryOccurrences,
                                        String trendReportWaitTime) {
        return new LreTestRunModel(
                serverAndPort,
                "user",
                "pass",
                "domain",
                "project",
                "EXISTING_TEST",
                "1",
                "",
                "AUTO",
                "1",
                "1",
                "0",
                PostRunAction.DO_NOTHING,
                false,
                "desc",
                "NO_TREND",
                "",
                httpsProtocol,
                "",
                "puser",
                "ppass",
                "NO_RETRY",
                retryDelay,
                retryOccurrences,
                trendReportWaitTime,
                false,
                false,
                false,
                false,
                "out",
                "ws");
    }

    public void testServerAndPortNormalizationAndProtocol() {
        LreTestRunModel model = createModel("server?tenant=abc", false, "5", "3", "0");

        assertEquals("server/?tenant=abc", model.getLreServerAndPort());
        assertEquals("http", model.getProtocol());
    }

    public void testRetryAndTrendDefaults() {
        LreTestRunModel model = createModel("server", true, "0", "-1", "abc");

        assertEquals("5", model.getRetryDelay());
        assertEquals("3", model.getRetryOccurrences());
        assertEquals("0", model.getTrendReportWaitTime());
        assertEquals("https", model.getProtocol());
    }

    public void testToStringIncludesKeyParams() {
        LreTestRunModel model = createModel("server", false, "5", "3", "0");
        String value = model.toString();

        assertTrue(value.contains("lreServerAndPort='server'"));
        assertTrue(value.contains("username='user'"));
        assertTrue(value.contains("domain='domain'"));
        assertTrue(value.contains("project='project'"));
    }
}

