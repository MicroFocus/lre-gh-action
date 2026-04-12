package com.opentext.lre.actions.workspacesync;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ZipFolderCompressor {
    public Path compressFolder(ScriptFolder folder) throws IOException {
        Objects.requireNonNull(folder, "folder");
        Path baseDir = folder.getFullPath();
        Path zipPath = baseDir.getParent().resolve(folder.getZipFileName());

        try (OutputStream outputStream = Files.newOutputStream(zipPath);
             ZipOutputStream zipStream = new ZipOutputStream(outputStream)) {
            Files.walkFileTree(baseDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Path relative = baseDir.relativize(dir);
                    if (!relative.toString().isEmpty()) {
                        ZipEntry entry = new ZipEntry(relative.toString().replace("\\", "/") + "/");
                        zipStream.putNextEntry(entry);
                        zipStream.closeEntry();
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path relative = baseDir.relativize(file);
                    ZipEntry entry = new ZipEntry(relative.toString().replace("\\", "/"));
                    zipStream.putNextEntry(entry);
                    Files.copy(file, zipStream);
                    zipStream.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        return zipPath;
    }
}

