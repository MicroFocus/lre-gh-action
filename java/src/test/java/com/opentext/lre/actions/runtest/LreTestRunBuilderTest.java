package com.opentext.lre.actions.runtest;

import com.microfocus.adm.performancecenter.plugins.common.pcentities.PostRunAction;
import com.opentext.lre.actions.common.helpers.result.model.junit.JUnitTestCaseStatus;
import com.opentext.lre.actions.common.helpers.result.model.junit.Testcase;
import com.opentext.lre.actions.common.helpers.result.model.junit.Testsuite;
import junit.framework.TestCase;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class LreTestRunBuilderTest extends TestCase {

    public void testWaitTimeIsClampedToMax300() throws Exception {
        LreTestRunBuilder builder = newBuilderWithTrendWaitTime("999");

        int wait = invokeWaitTime(builder);

        assertEquals(300, wait);
    }

    public void testWaitTimeReturnsZeroForInvalidInput() throws Exception {
        LreTestRunBuilder builder = newBuilderWithTrendWaitTime("abc");

        int wait = invokeWaitTime(builder);

        assertEquals(0, wait);
    }

    public void testResolveResultsFileFallsBackToWorkspaceWhenOutputBlank() throws Exception {
        Path workspace = Files.createTempDirectory("lre-builder-ws");
        LreTestRunBuilder builder = newBuilderForPaths(workspace, "");

        File resultFile = invokeResolveResultsFile(builder);

        assertTrue(resultFile.getParentFile().getAbsolutePath().contains("LreResult"));
        assertTrue(resultFile.getAbsolutePath().startsWith(workspace.toAbsolutePath().toString()));
        assertTrue(resultFile.getName().startsWith("Results"));
        assertTrue(resultFile.getName().endsWith(".xml"));
    }

    public void testResolveResultsFileUsesOutputWhenAvailable() throws Exception {
        Path workspace = Files.createTempDirectory("lre-builder-ws");
        Path output = Files.createTempDirectory("lre-builder-out");
        LreTestRunBuilder builder = newBuilderForPaths(workspace, output.toString());

        File resultFile = invokeResolveResultsFile(builder);

        assertEquals(output.toAbsolutePath().toString(), resultFile.getParentFile().getAbsolutePath());
        assertTrue(resultFile.getName().startsWith("Results"));
        assertTrue(resultFile.getName().endsWith(".xml"));
    }

    public void testContainsErrorsOrFailuresReturnsTrueWhenAnyTestCaseFailed() throws Exception {
        LreTestRunBuilder builder = newBuilderWithTrendWaitTime("10");

        Testsuite passingSuite = createSuiteWithStatus(JUnitTestCaseStatus.PASS);
        Testsuite failingSuite = createSuiteWithStatus(JUnitTestCaseStatus.FAILURE);

        boolean hasFailures = invokeContainsErrorsOrFailures(builder, List.of(passingSuite, failingSuite));

        assertTrue(hasFailures);
    }

    public void testContainsErrorsOrFailuresReturnsFalseWhenAllPass() throws Exception {
        LreTestRunBuilder builder = newBuilderWithTrendWaitTime("10");

        Testsuite suite = createSuiteWithStatus(JUnitTestCaseStatus.PASS);

        boolean hasFailures = invokeContainsErrorsOrFailures(builder, List.of(suite));

        assertFalse(hasFailures);
    }

    public void testGetLreTestRunModelIsCached() {
        Path workspace = createTempDirUnchecked("lre-builder-cache");
        LreTestRunBuilder builder = newBuilderForPaths(workspace, "");

        LreTestRunModel model1 = builder.getLreTestRunModel();
        LreTestRunModel model2 = builder.getLreTestRunModel();

        assertSame(model1, model2);
    }


    private LreTestRunBuilder newBuilderForPaths(Path workspace, String output) {
        return new LreTestRunBuilder(
                "server?tenant=abc",
                new UsernamePasswordCredentials("user", "pass".toCharArray()),
                "domain",
                "project",
                "EXISTING_TEST",
                "1",
                "",
                "1",
                "AUTO",
                "0",
                "30",
                PostRunAction.DO_NOTHING,
                false,
                false,
                "desc",
                "NO_TREND",
                "",
                false,
                "",
                new UsernamePasswordCredentials("", new char[0]),
                "NO_RETRY",
                "5",
                "3",
                "0",
                false,
                false,
                true,
                output,
                workspace.toString()
        );
    }

    private LreTestRunBuilder newBuilderWithTrendWaitTime(String trendWait) {
        Path workspace = createTempDirUnchecked("lre-builder-trend");
        return new LreTestRunBuilder(
                "server?tenant=abc",
                new UsernamePasswordCredentials("user", "pass".toCharArray()),
                "domain",
                "project",
                "EXISTING_TEST",
                "1",
                "",
                "1",
                "AUTO",
                "0",
                "30",
                PostRunAction.DO_NOTHING,
                false,
                false,
                "desc",
                "NO_TREND",
                "",
                false,
                "",
                new UsernamePasswordCredentials("", new char[0]),
                "NO_RETRY",
                "5",
                "3",
                trendWait,
                false,
                false,
                true,
                "",
                workspace.toString()
        );
    }

    private Testsuite createSuiteWithStatus(String status) {
        Testcase testcase = new Testcase();
        testcase.setStatus(status);
        testcase.setName("tc");

        Testsuite suite = new Testsuite();
        suite.getTestcase().add(testcase);
        suite.setName("suite");
        suite.setTests("1");
        return suite;
    }

    private int invokeWaitTime(LreTestRunBuilder target) throws Exception {
        Method method = target.getClass().getDeclaredMethod("getWaitTimeInSecondsBeforeRequestingTrendReport");
        method.setAccessible(true);
        return (Integer) method.invoke(target);
    }

    private File invokeResolveResultsFile(LreTestRunBuilder target) throws Exception {
        Method method = target.getClass().getDeclaredMethod("resolveResultsFile");
        method.setAccessible(true);
        return (File) method.invoke(target);
    }

    private boolean invokeContainsErrorsOrFailures(LreTestRunBuilder target, List<Testsuite> suites) throws Exception {
        Method method = target.getClass().getDeclaredMethod("containsErrorsOrFailures", List.class);
        method.setAccessible(true);
        return (Boolean) method.invoke(target, suites);
    }

    private Path createTempDirUnchecked(String prefix) {
        try {
            return Files.createTempDirectory(prefix);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

