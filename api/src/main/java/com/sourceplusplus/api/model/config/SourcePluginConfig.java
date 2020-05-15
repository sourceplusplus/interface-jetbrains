package com.sourceplusplus.api.model.config;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.*;

/**
 * Holds the current configuration used by the plugin.
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.5
 * @since 0.1.0
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public final class SourcePluginConfig {

    public static final transient SourcePluginConfig current = defaultConfiguration();
    private volatile Set<SourceEnvironmentConfig> environments = new HashSet<>();
    public volatile SourceEnvironmentConfig activeEnvironment = null;
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
        classVirtualTextMarksEnabled = config.classVirtualTextMarksEnabled;
        methodVirtualTextMarksEnabled = config.methodVirtualTextMarksEnabled;
        classGutterMarksEnabled = config.classGutterMarksEnabled;
        methodGutterMarksEnabled = config.methodGutterMarksEnabled;
        remoteAgentHost = config.remoteAgentHost;
        remoteAgentPort = config.remoteAgentPort;
        agentPatcherEnabled = config.agentPatcherEnabled;
    }

    public List<SourceEnvironmentConfig> getEnvironments() {
        return new ArrayList<>(environments);
    }

    public void setEnvironments(List<SourceEnvironmentConfig> environments) {
        this.environments = new HashSet<>(environments);
    }

    @Override
    public SourcePluginConfig clone() {
        SourcePluginConfig config = new SourcePluginConfig();
        config.applyConfig(this);
        return config;
    }

    public static SourcePluginConfig defaultConfiguration() {
        return new SourcePluginConfig();
    }
}
