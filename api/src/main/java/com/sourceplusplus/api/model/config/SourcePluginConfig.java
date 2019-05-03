package com.sourceplusplus.api.model.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Holds the current configuration used by the plugin
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.0
 * @since 0.1.0
 */
public final class SourcePluginConfig {

    public static final SourcePluginConfig current = new SourcePluginConfig();
    public volatile List<SourceEnvironmentConfig> environments = new ArrayList<>();
    public volatile int apiBridgePort = 7000;
    public volatile String appUuid = null;
    public volatile boolean classVirtualTextMarksEnabled = false;
    public volatile boolean methodVirtualTextMarksEnabled = false;
    public volatile boolean classGutterMarksEnabled = false;
    public volatile boolean methodGutterMarksEnabled = true;
    public volatile String remoteAgentHost = "localhost";
    public volatile int remoteAgentPort = -1;
    public volatile boolean agentPatcherEnabled = true;

    private SourcePluginConfig() {
    }

    public void applyConfig(SourcePluginConfig config) {
        Objects.requireNonNull(config);
        appUuid = config.appUuid;
        classVirtualTextMarksEnabled = config.classVirtualTextMarksEnabled;
        methodVirtualTextMarksEnabled = config.methodVirtualTextMarksEnabled;
        classGutterMarksEnabled = config.classGutterMarksEnabled;
        methodGutterMarksEnabled = config.methodGutterMarksEnabled;
        remoteAgentPort = config.remoteAgentPort;
        agentPatcherEnabled = config.agentPatcherEnabled;
    }

    public SourceEnvironmentConfig getEnvironment() {
        return null;
    }
}
