package com.opentext.lre.actions.common.model;

/**
 * Base model containing common parameters shared by all LRE action models.
 * This includes authentication, server connection, and proxy configuration.
 */
public abstract class LreBaseModel {

    // Common fields shared by all LRE models
    private final String lreServerAndPort;
    private final boolean httpsProtocol;
    private final String username;
    private final String password;
    private final String domain;
    private final String project;
    private final String proxyOutURL;
    private final String usernameProxy;
    private final String passwordProxy;
    private final boolean authenticateWithToken;
    private final boolean enableStacktrace;
    private final String workspace;
    private final String description;

    /**
     * Constructor for base LRE model with common parameters
     *
     * @param lreServerAndPort Server URL with port and tenant (e.g., "server:443/LoadTest/tenant")
     * @param httpsProtocol Whether to use HTTPS protocol
     * @param username LRE username
     * @param password LRE password
     * @param domain LRE domain
     * @param project LRE project
     * @param proxyOutURL Proxy URL (optional)
     * @param usernameProxy Proxy username (optional)
     * @param passwordProxy Proxy password (optional)
     * @param authenticateWithToken Whether to use token-based authentication
     * @param enableStacktrace Whether to enable stack trace output
     * @param workspace Workspace path (optional)
     * @param description Description (optional)
     */
    protected LreBaseModel(String lreServerAndPort,
                           boolean httpsProtocol,
                           String username,
                           String password,
                           String domain,
                           String project,
                           String proxyOutURL,
                           String usernameProxy,
                           String passwordProxy,
                           boolean authenticateWithToken,
                           boolean enableStacktrace,
                           String workspace,
                           String description) {
        this.lreServerAndPort = getCorrectLreServerAndPort(lreServerAndPort);
        this.httpsProtocol = httpsProtocol;
        this.username = username;
        this.password = password;
        this.domain = domain;
        this.project = project;
        this.proxyOutURL = proxyOutURL == null ? "" : proxyOutURL;
        this.usernameProxy = usernameProxy == null ? "" : usernameProxy;
        this.passwordProxy = passwordProxy == null ? "" : passwordProxy;
        this.authenticateWithToken = authenticateWithToken;
        this.enableStacktrace = enableStacktrace;
        this.workspace = workspace == null ? "" : workspace;
        this.description = description == null ? "" : description;
    }

    /**
     * Ensures the server URL has proper format for query parameters
     */
    private String getCorrectLreServerAndPort(String lreServerAndPort) {
        if(lreServerAndPort != null && lreServerAndPort.contains("?") && !lreServerAndPort.contains("/?")) {
            return lreServerAndPort.replace("?", "/?");
        }
        return lreServerAndPort;
    }

    // Getters for common fields

    public String getLreServerAndPort() {
        return lreServerAndPort;
    }

    public boolean isHttpsProtocol() {
        return httpsProtocol;
    }

    public String getProtocol() {
        return httpsProtocol ? "https" : "http";
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getDomain() {
        return domain;
    }

    public String getProject() {
        return project;
    }

    public String getProxyOutURL() {
        return proxyOutURL;
    }

    public String getUsernameProxy() {
        return usernameProxy;
    }

    public String getPasswordProxy() {
        return passwordProxy;
    }

    public boolean isAuthenticateWithToken() {
        return authenticateWithToken;
    }

    public boolean isEnableStacktrace() {
        return enableStacktrace;
    }

    public String getWorkspace() {
        return workspace;
    }

    public String getDescription() {
        return description;
    }
}
