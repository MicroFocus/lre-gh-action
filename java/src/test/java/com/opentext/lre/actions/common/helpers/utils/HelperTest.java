package com.opentext.lre.actions.common.helpers.utils;

import junit.framework.TestCase;

import java.nio.file.Files;
import java.nio.file.Path;

public class HelperTest extends TestCase {

    public void testGetParentReturnsPathParent() throws Exception {
        Path root = Files.createTempDirectory("lre-helper");
        Path nested = Files.createDirectories(root.resolve("a").resolve("b"));

        Path parent = Helper.getParent(nested);

        assertEquals(nested.getParent(), parent);
    }

    public void testIsUsrScriptReturnsTrueWhenUsrFileExists() throws Exception {
        Path dir = Files.createTempDirectory("lre-helper-usr");
        Files.writeString(dir.resolve("script.usr"), "dummy");

        assertTrue(Helper.isUsrScript(dir.toString()));
    }

    public void testIsUsrScriptReturnsFalseWhenUsrFileMissing() throws Exception {
        Path dir = Files.createTempDirectory("lre-helper-no-usr");
        Files.writeString(dir.resolve("notes.txt"), "dummy");

        assertFalse(Helper.isUsrScript(dir.toString()));
    }
}

