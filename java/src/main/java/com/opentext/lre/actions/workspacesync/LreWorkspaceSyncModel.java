package com.opentext.lre.actions.workspacesync;

import com.opentext.lre.actions.common.model.LreBaseModel;

import java.util.Collections;
import java.util.List;

public final class LreWorkspaceSyncModel extends LreBaseModel {
    private final boolean runtimeOnly;
    private final int workspaceSyncSuccessThresholdPercent;
    private final boolean workspaceSyncIncremental;
    private final boolean workspaceSyncDeleteRemovedScripts;
    private final boolean workspaceSyncChangesDetermined;
    private final List<String> workspaceSyncChangedFiles;
    private final List<String> workspaceSyncDeletedFiles;

    public LreWorkspaceSyncModel(String lreServerAndPort,
                                 boolean httpsProtocol,
                                 String username,
                                 String password,
                                 String domain,
                                 String project,
                                 String proxyOutURL,
                                 String usernameProxy,
                                 String passwordProxy,
                                 String workspacePath,
                                 boolean runtimeOnly,
                                 int workspaceSyncSuccessThresholdPercent,
                                 boolean workspaceSyncIncremental,
                                 boolean workspaceSyncDeleteRemovedScripts,
                                 boolean workspaceSyncChangesDetermined,
                                 List<String> workspaceSyncChangedFiles,
                                 List<String> workspaceSyncDeletedFiles,
                                 boolean authenticateWithToken,
                                 boolean enableStacktrace,
                                 String description) {
        super(lreServerAndPort, httpsProtocol, username, password, domain, project,
              proxyOutURL, usernameProxy, passwordProxy, authenticateWithToken,
              enableStacktrace, workspacePath, description);
        this.runtimeOnly = runtimeOnly;
        this.workspaceSyncSuccessThresholdPercent = workspaceSyncSuccessThresholdPercent;
        this.workspaceSyncIncremental = workspaceSyncIncremental;
        this.workspaceSyncDeleteRemovedScripts = workspaceSyncDeleteRemovedScripts;
        this.workspaceSyncChangesDetermined = workspaceSyncChangesDetermined;
        this.workspaceSyncChangedFiles = workspaceSyncChangedFiles == null
                ? List.of()
                : List.copyOf(workspaceSyncChangedFiles);
        this.workspaceSyncDeletedFiles = workspaceSyncDeletedFiles == null
                ? List.of()
                : List.copyOf(workspaceSyncDeletedFiles);
    }

    public LreWorkspaceSyncModel(String lreServerAndPort,
                                 boolean httpsProtocol,
                                 String username,
                                 String password,
                                 String domain,
                                 String project,
                                 String proxyOutURL,
                                 String usernameProxy,
                                 String passwordProxy,
                                 String workspacePath,
                                 boolean runtimeOnly,
                                 int workspaceSyncSuccessThresholdPercent,
                                 boolean authenticateWithToken,
                                 boolean enableStacktrace,
                                 String description) {
        this(lreServerAndPort,
                httpsProtocol,
                username,
                password,
                domain,
                project,
                proxyOutURL,
                usernameProxy,
                passwordProxy,
                workspacePath,
                runtimeOnly,
                workspaceSyncSuccessThresholdPercent,
                false,
                false,
                false,
                List.of(),
                List.of(),
                authenticateWithToken,
                enableStacktrace,
                description);
    }


    public boolean isRuntimeOnly() {
        return runtimeOnly;
    }

    public int getWorkspaceSyncSuccessThresholdPercent() {
        return workspaceSyncSuccessThresholdPercent;
    }

    public boolean isWorkspaceSyncIncremental() {
        return workspaceSyncIncremental;
    }

    public boolean isWorkspaceSyncChangesDetermined() {
        return workspaceSyncChangesDetermined;
    }

    public boolean isWorkspaceSyncDeleteRemovedScripts() {
        return workspaceSyncDeleteRemovedScripts;
    }

    public List<String> getWorkspaceSyncChangedFiles() {
        return Collections.unmodifiableList(workspaceSyncChangedFiles);
    }

    public List<String> getWorkspaceSyncDeletedFiles() {
        return Collections.unmodifiableList(workspaceSyncDeletedFiles);
    }
}
