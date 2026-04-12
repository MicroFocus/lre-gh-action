package com.opentext.lre.actions.workspacesync;

import junit.framework.TestCase;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipFolderCompressorTest extends TestCase {

    public void testCompressFolderCreatesZipWithExpectedEntries() throws Exception {
        Path workspace = Files.createTempDirectory("lre-zip");
        Path scriptRoot = Files.createDirectories(workspace.resolve("script1"));
        Path nestedDir = Files.createDirectories(scriptRoot.resolve("data"));
        Files.writeString(scriptRoot.resolve("test.usr"), "dummy");
        Files.writeString(nestedDir.resolve("input.txt"), "payload");

        ScriptFolder folder = new ScriptFolder(scriptRoot, workspace);
        ZipFolderCompressor compressor = new ZipFolderCompressor();

        Path zipPath = compressor.compressFolder(folder);

        assertTrue(Files.exists(zipPath));
        assertEquals("script1.zip", zipPath.getFileName().toString());

        Set<String> entries = new HashSet<>();
        try (ZipInputStream zin = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                entries.add(entry.getName());
            }
        }

        assertTrue(entries.contains("test.usr"));
        assertTrue(entries.contains("data/"));
        assertTrue(entries.contains("data/input.txt"));
    }
}

