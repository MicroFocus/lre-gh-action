package com.opentext.lre.actions.workspacesync;

import junit.framework.TestCase;

import java.nio.file.Paths;

public class LreSubjectPathBuilderTest extends TestCase {

    public void testNullPathReturnsSubjectRoot() {
        assertEquals("Subject", LreSubjectPathBuilder.toSubjectPath(null));
    }

    public void testDotPathReturnsSubjectRoot() {
        assertEquals("Subject", LreSubjectPathBuilder.toSubjectPath(Paths.get(".")));
    }

    public void testNestedPathIsAppendedToSubjectRoot() {
        String subjectPath = LreSubjectPathBuilder.toSubjectPath(Paths.get("Team", "Scripts", "Smoke"));
        assertEquals("Subject\\Team\\Scripts\\Smoke", subjectPath);
    }
}

