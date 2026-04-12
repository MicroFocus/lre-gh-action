package com.opentext.lre.actions.workspacesync;

import junit.framework.TestCase;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class WorkspaceScriptFolderScannerTest extends TestCase {
    public void testStopsAtScriptFolder() throws Exception {
        Path workspace = Files.createTempDirectory("lre-ws");
        Path scriptRoot = Files.createDirectories(workspace.resolve("a").resolve("b"));
        Path nested = Files.createDirectories(scriptRoot.resolve("c"));
        Files.writeString(scriptRoot.resolve("test.usr"), "dummy");
        Files.writeString(nested.resolve("nested.jmx"), "dummy");

        WorkspaceScriptFolderScanner scanner = new WorkspaceScriptFolderScanner();
        List<ScriptFolder> folders = scanner.findScriptFolders(workspace);

        assertEquals(1, folders.size());
        assertEquals(workspace.relativize(scriptRoot.getParent()), folders.get(0).getRelativePath());
    }

    public void testRelativePathUsesScriptFolderWhenParentIsWorkspaceRoot() throws Exception {
        Path workspace = Files.createTempDirectory("lre-ws");
        Path scriptRoot = Files.createDirectories(workspace.resolve("root-script"));
        Files.writeString(scriptRoot.resolve("test.jmx"), "dummy");

        WorkspaceScriptFolderScanner scanner = new WorkspaceScriptFolderScanner();
        List<ScriptFolder> folders = scanner.findScriptFolders(workspace);

        assertEquals(1, folders.size());
        assertEquals(workspace.relativize(scriptRoot), folders.get(0).getRelativePath());
    }

    public void testDetectsDevWebScriptFolder() throws Exception {
        Path workspace = Files.createTempDirectory("lre-ws");
        Path scriptRoot = Files.createDirectories(workspace.resolve("devweb"));
        Files.writeString(scriptRoot.resolve("main.js"), "dummy");
        Files.writeString(scriptRoot.resolve("rts.yml"), "dummy");

        WorkspaceScriptFolderScanner scanner = new WorkspaceScriptFolderScanner();
        List<ScriptFolder> folders = scanner.findScriptFolders(workspace);

        assertEquals(1, folders.size());
        assertEquals(workspace.relativize(scriptRoot), folders.get(0).getRelativePath());
    }

    public void testDevWebRequiresBothMainAndRts() throws Exception {
        Path workspace = Files.createTempDirectory("lre-ws");
        Path mainOnly = Files.createDirectories(workspace.resolve("main-only"));
        Path rtsOnly = Files.createDirectories(workspace.resolve("rts-only"));
        Files.writeString(mainOnly.resolve("main.js"), "dummy");
        Files.writeString(rtsOnly.resolve("rts.yml"), "dummy");

        WorkspaceScriptFolderScanner scanner = new WorkspaceScriptFolderScanner();
        List<ScriptFolder> folders = scanner.findScriptFolders(workspace);

        assertEquals(0, folders.size());
    }

    public void testDetectsAdditionalScriptExtensions() throws Exception {
        Path workspace = Files.createTempDirectory("lre-ws");
        Path scalaScript = Files.createDirectories(workspace.resolve("scala-script"));
        Path javaScript = Files.createDirectories(workspace.resolve("java-script"));
        Files.writeString(scalaScript.resolve("test.scala"), "dummy");
        Files.writeString(javaScript.resolve("Test.java"), "dummy");

        WorkspaceScriptFolderScanner scanner = new WorkspaceScriptFolderScanner();
        List<ScriptFolder> folders = scanner.findScriptFolders(workspace);

        assertEquals(2, folders.size());
    }
}
