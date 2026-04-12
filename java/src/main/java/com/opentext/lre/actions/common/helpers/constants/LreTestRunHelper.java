package com.opentext.lre.actions.common.helpers.constants;

import com.opentext.lre.actions.common.helpers.utils.LogHelper;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.nio.file.Files;

public class LreTestRunHelper {
    // Static field to control stack trace output - set based on lre_enable_stacktrace configuration
    public static boolean ENABLE_STACKTRACE = false;

    public LreTestRunHelper() {
    }

    public static String verifyStringValueIsIntAndPositive(String supplied, int defaultValue) {
        if (supplied != null && isInteger(supplied)) {
            int suppliedInt = Integer.parseInt(supplied);
            if (suppliedInt > 0)
                return Integer.toString(suppliedInt);
        }
        return Integer.toString(defaultValue);
    }

    public static boolean isInteger(String s) {
        if (s == null || s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            if (i == 0 && s.charAt(i) == '-') {
                if (s.length() == 1) return false;
                else continue;
            }
            if (Character.digit(s.charAt(i), 10) < 0) return false;
        }
        return true;
    }


    public static void getZipFiles(String filename, String destinationname) {
        try (ZipInputStream zipinputstream =
                     new ZipInputStream(new FileInputStream(filename))) {
            Path destinationPath = Paths.get(destinationname).toAbsolutePath().normalize();
            Files.createDirectories(destinationPath);

            byte[] buf = new byte[1024];
            ZipEntry zipentry;

            while ((zipentry = zipinputstream.getNextEntry()) != null) {
                Path resolvedPath = destinationPath.resolve(
                                zipentry.getName()
                                        .replace('/', File.separatorChar)
                                        .replace('\\', File.separatorChar))
                        .normalize();

                if (!resolvedPath.startsWith(destinationPath)) {
                    throw new IOException("Blocked unsafe ZIP entry: " + zipentry.getName());
                }

                File newFile = resolvedPath.toFile();

                if (zipentry.isDirectory()) {
                    Files.createDirectories(resolvedPath);
                    zipinputstream.closeEntry();
                    continue;
                }

                // 🔑 Ensure parent directories exist
                Path parentDir = resolvedPath.getParent();
                if (parentDir != null) {
                    Files.createDirectories(parentDir);
                }

                try (FileOutputStream fileoutputstream = new FileOutputStream(newFile)) {
                    int n;
                    while ((n = zipinputstream.read(buf)) > 0) {
                        fileoutputstream.write(buf, 0, n);
                    }
                }

                zipinputstream.closeEntry();
            }

        } catch (Exception e) {
            // Log error. Print stack trace only if lre_enable_stacktrace is true
            LogHelper.log("Failed to extract archive: %s", true, e.getMessage());
            LogHelper.logStackTrace(e);
        }
    }

    public static boolean verifyStringIsPath(Path workspace, String strPossiblePath) {
        File file = new File(workspace.toString(), strPossiblePath);
        return file.exists();
    }

    public static String retreiveFileExtension(Path workspace, String strPossiblePath) {
        File filePath = new File(workspace.toString(), strPossiblePath);
        return ".".concat(FilenameUtils.getExtension(filePath.getName()).toLowerCase());
    }

    public static String fileNameWithoutExtension(Path workspace, String strPossiblePath) {
        File file = new File(workspace.toString(), strPossiblePath);
        return FilenameUtils.removeExtension(file.getName());
    }

    public static String filePath(String strPossiblePath) {
        File file = new File(strPossiblePath);
        File fileParent = file.getParentFile();
        return (fileParent == null || fileParent.getPath().isEmpty()) ? "default_folder" : fileParent.getPath();
    }

    public static String fileContenToString(Path workspace, String filePath)
            throws IOException {
        File file = new File(workspace.toString(), filePath);
        Path fileName = Paths.get(file.getAbsolutePath());
        return readFile(fileName.toAbsolutePath().toString(), Charset.defaultCharset());
    }

    static String readFile(String path, Charset encoding)
            throws IOException
    {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }
}
