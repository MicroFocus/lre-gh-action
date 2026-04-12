package com.opentext.lre.actions.workspacesync;

import junit.framework.TestCase;

import java.nio.file.Files;
import java.nio.file.Path;

public class ScriptFolderTest extends TestCase {

    public void testRelativePathWhenScriptFolderDirectlyUnderWorkspace() throws Exception {
        Path workspace = Files.createTempDirectory("lre-script-folder");
        Path scriptRoot = Files.createDirectories(workspace.resolve("rootScript"));

        ScriptFolder folder = new ScriptFolder(scriptRoot, workspace);

        assertEquals(workspace.relativize(scriptRoot), folder.getRelativePath());
    }

    public void testRelativePathWhenScriptFolderNestedUnderWorkspace() throws Exception {
        Path workspace = Files.createTempDirectory("lre-script-folder");
        Path scriptRoot = Files.createDirectories(workspace.resolve("team").resolve("scriptA"));

        ScriptFolder folder = new ScriptFolder(scriptRoot, workspace);

        assertEquals(workspace.relativize(scriptRoot.getParent()), folder.getRelativePath());
    }

    public void testZipFileNameUsesFolderName() throws Exception {
        Path workspace = Files.createTempDirectory("lre-script-folder");
        Path scriptRoot = Files.createDirectories(workspace.resolve("apiSmoke"));

        ScriptFolder folder = new ScriptFolder(scriptRoot, workspace);

        assertEquals("apiSmoke.zip", folder.getZipFileName());
    }
}

