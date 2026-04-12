package com.opentext.lre.actions.workspacesync;

import java.nio.file.Path;
import java.util.Objects;

public final class ScriptFolder {
    private final Path fullPath;
    private final Path relativePath;

    public ScriptFolder(Path fullPath, Path workspaceRoot) {
        this.fullPath = Objects.requireNonNull(fullPath, "fullPath");
        Path root = Objects.requireNonNull(workspaceRoot, "workspaceRoot").normalize();
        Path parent = fullPath.getParent();
        if (parent != null && parent.normalize().equals(root)) {
            this.relativePath = root.relativize(fullPath);
        } else if (parent != null) {
            this.relativePath = root.relativize(parent);
        } else {
            this.relativePath = root.relativize(fullPath);
        }
    }

    public Path getFullPath() {
        return fullPath;
    }

    public Path getRelativePath() {
        return relativePath;
    }

    public String getZipFileName() {
        Path fileName = fullPath.getFileName();
        String base = fileName == null ? "script" : fileName.toString();
        return base + ".zip";
    }
}
