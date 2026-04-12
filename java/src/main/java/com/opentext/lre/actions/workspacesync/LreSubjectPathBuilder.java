package com.opentext.lre.actions.workspacesync;

import java.io.File;
import java.nio.file.Path;

public final class LreSubjectPathBuilder {
    private static final String SUBJECT_ROOT = "Subject";

    private LreSubjectPathBuilder() {
    }

    public static String toSubjectPath(Path relativeFolder) {
        if (relativeFolder == null) {
            return SUBJECT_ROOT;
        }
        String relative = relativeFolder.toString().replace(File.separatorChar, '\\');
        if (relative.isEmpty() || ".".equals(relative)) {
            return SUBJECT_ROOT;
        }
        return SUBJECT_ROOT + "\\" + relative;
    }
}

