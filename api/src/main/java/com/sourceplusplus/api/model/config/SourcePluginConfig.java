package com.sourceplusplus.api.model.config;

import java.util.*;

/**
 * Holds the current configuration used by the plugin
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.0
 * @since 0.1.0
 */
public final class SourcePluginConfig {

    public static final SourcePluginConfig current = new SourcePluginConfig();
    private volatile Set<SourceEnvironmentConfig> environments = new HashSet<>();
    private volatile SourceEnvironmentConfig activeEnvironment = null;
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
        environments = new HashSet<>(config.environments);
        activeEnvironment = config.activeEnvironment;
        appUuid = config.appUuid;
        classVirtualTextMarksEnabled = config.classVirtualTextMarksEnabled;
        methodVirtualTextMarksEnabled = config.methodVirtualTextMarksEnabled;
        classGutterMarksEnabled = config.classGutterMarksEnabled;
        methodGutterMarksEnabled = config.methodGutterMarksEnabled;
        remoteAgentPort = config.remoteAgentPort;
        agentPatcherEnabled = config.agentPatcherEnabled;
    }

    public SourceEnvironmentConfig getEnvironment() {
        if (activeEnvironment == null && environments.size() == 1) {
            activeEnvironment = environments.iterator().next();
        }
        return activeEnvironment;
    }

    public List<SourceEnvironmentConfig> getEnvironments() {
        return new ArrayList<>(environments);
    }

    public void setEnvironments(List<SourceEnvironmentConfig> environments) {
        this.environments = new HashSet<>(environments);
    }
}
