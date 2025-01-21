package com.opentext.lre.actions.common.helpers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import com.opentext.lre.actions.common.helpers.utils.LogHelper;
import org.json.JSONObject;

public class OutputUpdater {
    private String parameterKey = "lre_run_id";
    private String filePath;
    public OutputUpdater(Path workspace)
    {
        String directoryPath = workspace != null ? workspace.toString() : System.getProperty("user.dir");
        filePath = directoryPath + File.separator + parameterKey + ".conf";
    }

    // Method to update or create the config file with the specified parameter
    public void updateParameter(String parameterValue) {
        JSONObject json = new JSONObject();
        json.put(parameterKey, parameterValue);

        // Check if the file exists and whether it has write permissions
        File file = new File(filePath);

        // If the file exists, check if it's writable
        if (file.exists()) {
            if (file.canWrite()) {
                // File exists and is writable, so proceed with writing
                writeToFile(file, json);
            } else {
                // If the file exists but cannot be written to, print a message
                LogHelper.error("OutputUpdater - Error: File is not writable.");
            }
        } else {
            // File doesn't exist, create a new file
            writeToFile(file, json);
        }
    }

    // Method to write the JSON data to the file
    private void writeToFile(File file, JSONObject json) {
        try (FileWriter fileWriter = new FileWriter(file)) {
            // Write or update the file with the new parameter value
            fileWriter.write(json.toString());
            LogHelper.info("File updated: " + file.getAbsolutePath());
        } catch (IOException e) {
            LogHelper.logStackTrace("OutputUpdater - Error while writing to file.", e);
        }
    }
}
