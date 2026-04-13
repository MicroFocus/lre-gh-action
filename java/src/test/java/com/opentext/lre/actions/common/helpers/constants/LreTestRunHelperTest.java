package com.opentext.lre.actions.common.helpers.constants;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class LreTestRunHelperTest extends TestCase {


    public void testGetZipFilesExtractsRegularEntries() throws Exception {
        Path zipFile = Files.createTempFile("lre-helper", ".zip");
        Path destination = Files.createTempDirectory("lre-helper-out");

        writeZip(zipFile, zip -> {
            zip.putNextEntry(new ZipEntry("folder/"));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("folder/file.txt"));
            zip.write("hello".getBytes());
            zip.closeEntry();
        });

        LreTestRunHelper.getZipFiles(zipFile.toString(), destination.toString());

        assertTrue(Files.exists(destination.resolve("folder")));
        assertEquals("hello", Files.readString(destination.resolve("folder").resolve("file.txt")));
    }

    public void testGetZipFilesExtractsNestedEntriesWithoutDirectoryRecords() throws Exception {
        Path zipFile = Files.createTempFile("lre-helper-nested", ".zip");
        Path destination = Files.createTempDirectory("lre-helper-nested-out");

        writeZip(zipFile, zip -> {
            zip.putNextEntry(new ZipEntry("LreReports/HtmlReport/Report/adv_properties.css"));
            zip.write("body { color: black; }".getBytes());
            zip.closeEntry();
        });

        LreTestRunHelper.getZipFiles(zipFile.toString(), destination.toString());

        assertEquals(
                "body { color: black; }",
                Files.readString(destination
                        .resolve("LreReports")
                        .resolve("HtmlReport")
                        .resolve("Report")
                        .resolve("adv_properties.css")));
    }

    public void testGetZipFilesExtractsWindowsStyleNestedEntries() throws Exception {
        Path zipFile = Files.createTempFile("lre-helper-windows", ".zip");
        Path destination = Files.createTempDirectory("lre-helper-windows-out");

        writeZip(zipFile, zip -> {
            zip.putNextEntry(new ZipEntry("LreReports\\HtmlReport\\Report\\adv_properties.css"));
            zip.write("body { background: white; }".getBytes());
            zip.closeEntry();
        });

        LreTestRunHelper.getZipFiles(zipFile.toString(), destination.toString());

        assertEquals(
                "body { background: white; }",
                Files.readString(destination
                        .resolve("LreReports")
                        .resolve("HtmlReport")
                        .resolve("Report")
                        .resolve("adv_properties.css")));
    }

    public void testGetZipFilesBlocksZipSlipEntries() throws Exception {
        Path zipFile = Files.createTempFile("lre-helper-slip", ".zip");
        Path destination = Files.createTempDirectory("lre-helper-slip-out");
        Path outsideFile = destination.getParent().resolve("evil.txt");
        Files.deleteIfExists(outsideFile);

        writeZip(zipFile, zip -> {
            zip.putNextEntry(new ZipEntry("../evil.txt"));
            zip.write("evil".getBytes());
            zip.closeEntry();
        });

        LreTestRunHelper.getZipFiles(zipFile.toString(), destination.toString());

        assertFalse(Files.exists(outsideFile));
        try (Stream<Path> files = Files.list(destination)) {
            assertEquals(0L, files.count());
        }
    }

    private void writeZip(Path zipFile, ZipWriter writer) throws Exception {
        try (OutputStream outputStream = Files.newOutputStream(zipFile);
             ZipOutputStream zipStream = new ZipOutputStream(outputStream)) {
            writer.write(zipStream);
        }
    }

    @FunctionalInterface
    private interface ZipWriter {
        void write(ZipOutputStream zipStream) throws IOException;
    }
}

