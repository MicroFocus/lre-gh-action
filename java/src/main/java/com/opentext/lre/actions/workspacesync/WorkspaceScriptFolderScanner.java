package com.opentext.lre.actions.workspacesync;

import com.opentext.lre.actions.common.helpers.constants.LreTestRunConstants;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class WorkspaceScriptFolderScanner {

    private static final Set<String> SCRIPT_EXTENSIONS = Set.of(
            LreTestRunConstants.USR_EXTENSION,
            LreTestRunConstants.JMX_EXTENSION,
            LreTestRunConstants.GATLING_EXTENSION,
            LreTestRunConstants.SELENIUM_EXTENSION
    );

    public List<ScriptFolder> findScriptFolders(Path workspaceRoot) throws IOException {
        List<ScriptFolder> result = new ArrayList<>();
        scanFolder(workspaceRoot, workspaceRoot, result);
        return result;
    }

    private void scanFolder(Path folder, Path workspaceRoot, List<ScriptFolder> result) throws IOException {

        if (!Files.isDirectory(folder, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }

        boolean containsExtensionScript = false;
        boolean hasDevWebMain = false;
        boolean hasDevWebRts = false;

        List<Path> subFolders = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder)) {
            for (Path child : stream) {

                if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
                    subFolders.add(child);
                    continue;
                }

                String fileName = child.getFileName().toString();

                // Extension-based detection
                if (isScriptExtension(fileName)) {
                    containsExtensionScript = true;
                }

                // DevWeb detection (exact file names, case-insensitive for cross-OS consistency)
                if (LreTestRunConstants.DEVWEB_MAIN_FILE.equalsIgnoreCase(fileName)) {
                    hasDevWebMain = true;
                } else if (LreTestRunConstants.DEVWEB_RTS_FILE.equalsIgnoreCase(fileName)) {
                    hasDevWebRts = true;
                }
            }
        }

        boolean isDevWebScript = hasDevWebMain && hasDevWebRts;

        if (containsExtensionScript || isDevWebScript) {
            result.add(new ScriptFolder(folder, workspaceRoot));
            return; // 🔥 prune subtree
        }

        // Recurse only if this folder is not a script folder
        for (Path subFolder : subFolders) {
            scanFolder(subFolder, workspaceRoot, result);
        }
    }

    private boolean isScriptExtension(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        return SCRIPT_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }
}
