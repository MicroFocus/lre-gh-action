package com.opentext.lre.actions.workspacesync;

import com.opentext.lre.actions.common.model.LreBaseModel;

public final class LreWorkspaceSyncModel extends LreBaseModel {
    private final boolean runtimeOnly;

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
                                 boolean authenticateWithToken,
                                 boolean enableStacktrace,
                                 String description) {
        super(lreServerAndPort, httpsProtocol, username, password, domain, project,
              proxyOutURL, usernameProxy, passwordProxy, authenticateWithToken,
              enableStacktrace, workspacePath, description);
        this.runtimeOnly = runtimeOnly;
    }


    public boolean isRuntimeOnly() {
        return runtimeOnly;
    }
}
