package com.opentext.lre.actions.common.helpers.constants;

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
        return isInteger(s, 10);
    }
    private static boolean isInteger(String s, int radix) {
        if (s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            if (i == 0 && s.charAt(i) == '-') {
                if (s.length() == 1) return false;
                else continue;
            }
            if (Character.digit(s.charAt(i), radix) < 0) return false;
        }
        return true;
    }

    public static String useParameterIfNeeded(String buildParameters, String attribute) {
        if (buildParameters != null && attribute != null && attribute.startsWith("$")) {
            String attributeParameter = attribute.replace("$", "").replace("{", "").replace("}", "");
            String[] buildParametersArray = buildParameters.replace("{", "").replace("}", "").split(",");
            for (String buildParameter : buildParametersArray) {
                if (buildParameter.trim().startsWith(attributeParameter + "=")) {
                    return buildParameter.trim().replace(attributeParameter + "=", "");
                }
            }
        }
        return attribute;
    }

    public static void getZipFiles(String filename, String destinationname) {
        try {
            byte[] buf = new byte[1024];
            ZipInputStream zipinputstream = null;
            ZipEntry zipentry;
            zipinputstream = new ZipInputStream(
                    new FileInputStream(filename));

            zipentry = zipinputstream.getNextEntry();
            while (zipentry != null) {
                //for each entry to be extracted
                String entryName = destinationname + File.separatorChar + zipentry.getName();
                entryName = entryName.replace('/', File.separatorChar);
                entryName = entryName.replace('\\', File.separatorChar);
                //System.out.println("entryname " + entryName);
                int n;
                FileOutputStream fileoutputstream;
                File newFile = new File(entryName);
                if (zipentry.isDirectory()) {
                    if (!newFile.mkdirs()) {
                        break;
                    }
                    zipentry = zipinputstream.getNextEntry();
                    continue;
                }

                fileoutputstream = new FileOutputStream(entryName);

                while ((n = zipinputstream.read(buf, 0, 1024)) > -1) {
                    fileoutputstream.write(buf, 0, n);
                }

                fileoutputstream.close();
                zipinputstream.closeEntry();
                zipentry = zipinputstream.getNextEntry();

            }//while

            zipinputstream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean verifyStringIsPath(Path workspace, String strPossiblePath) throws InterruptedException, IOException {
        File file = new File(workspace.toString(), strPossiblePath);
        return file.exists();
    }

    public static String retreiveFileExtension(Path workspace, String strPossiblePath) {
        File filePath = new File(workspace.toString(), strPossiblePath);
        return ".".concat(FilenameUtils.getExtension(filePath.getName()).toLowerCase());
    }

    public static String fileNameWithoutExtension(Path workspace, String strPossiblePath)
            throws InterruptedException, IOException {
        File file = new File(workspace.toString(), strPossiblePath);
        return FilenameUtils.removeExtension(file.getName());
    }

    public static String filePath(String strPossiblePath)
            throws InterruptedException, IOException {
        File file = new File(strPossiblePath);
        File fileParent = file.getParentFile();
        return (fileParent.getPath() == null || fileParent.getPath().isEmpty()) ? "default_folder" : fileParent.getPath();
    }

    public static String fileContenToString(Path workspace, String filePath)
            throws InterruptedException, IOException {
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
