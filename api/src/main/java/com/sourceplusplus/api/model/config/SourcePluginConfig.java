package com.sourceplusplus.api.model.config;

import java.util.Objects;

/**
 * Holds the current configuration used by the plugin
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.1.3
 * @since 0.1.0
 */
public final class SourcePluginConfig {

    public static final SourcePluginConfig current = new SourcePluginConfig();
    public volatile String apiHost = System.getenv().getOrDefault(
            "SPP_API_HOST", System.getProperty("SPP_API_HOST", "localhost"));
    public volatile int apiPort = Integer.parseInt(System.getenv().getOrDefault(
            "SPP_API_PORT", "" + System.getProperty("SPP_API_PORT", "8080")));
    public volatile boolean apiSslEnabled = Boolean.parseBoolean(System.getenv().getOrDefault(
            "SPP_API_SSL_ENABLED", System.getProperty("SPP_API_SSL_ENABLED", "false")));
    public volatile int apiBridgePort = 7000;
    public volatile String appUuid = null;
    public volatile boolean classVirtualTextMarksEnabled = false;
    public volatile boolean methodVirtualTextMarksEnabled = false;
    public volatile boolean classGutterMarksEnabled = false;
    public volatile boolean methodGutterMarksEnabled = true;
    public volatile String remoteAgentHost = "localhost";
    public volatile int remoteAgentPort = -1;
    public volatile String apiKey = null;
    public volatile boolean agentPatcherEnabled = true;

    private SourcePluginConfig() {
    }

    public void applyConfig(SourcePluginConfig config) {
        Objects.requireNonNull(config);
        apiHost = config.apiHost;
        apiPort = config.apiPort;
        apiSslEnabled = config.apiSslEnabled;
        appUuid = config.appUuid;
        classVirtualTextMarksEnabled = config.classVirtualTextMarksEnabled;
        methodVirtualTextMarksEnabled = config.methodVirtualTextMarksEnabled;
        classGutterMarksEnabled = config.classGutterMarksEnabled;
        methodGutterMarksEnabled = config.methodGutterMarksEnabled;
        remoteAgentPort = config.remoteAgentPort;
        apiKey = config.apiKey;
        agentPatcherEnabled = config.agentPatcherEnabled;
    }

    public String getSppUrl() {
        if (apiSslEnabled) {
            return "https://" + apiHost + ":" + apiPort;
        } else {
            return "http://" + apiHost + ":" + apiPort;
        }
    }
}
