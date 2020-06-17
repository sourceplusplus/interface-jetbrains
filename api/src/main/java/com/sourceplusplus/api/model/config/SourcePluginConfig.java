package com.sourceplusplus.api.model.config;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Holds the current configuration used by the plugin.
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.6
 * @since 0.1.0
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public final class SourcePluginConfig {

    public static final transient SourcePluginConfig current = defaultConfiguration();
    private volatile Set<SourceEnvironmentConfig> environments = new HashSet<>(); //todo: environments.contains(activeEnvironment) should be true by reference
    public volatile SourceEnvironmentConfig activeEnvironment = null; //todo: could add active flag to SourceEnvironmentConfig and remove this
    public volatile boolean classVirtualTextMarksEnabled = false;
    public volatile boolean methodVirtualTextMarksEnabled = false;
    public volatile boolean classGutterMarksEnabled = false;
    public volatile boolean methodGutterMarksEnabled = true;
    public volatile String remoteAgentHost = "localhost";
    public volatile int remoteAgentPort = -1;
    public volatile boolean agentPatcherEnabled = true;
    public volatile boolean embeddedCoreServer = true;

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
        embeddedCoreServer = config.embeddedCoreServer;
    }

    public List<SourceEnvironmentConfig> getEnvironments() {
        return new ArrayList<>(environments);
    }

    public void setEnvironments(List<SourceEnvironmentConfig> environments) {
        this.environments = new HashSet<>(environments);
    }

    public SourceEnvironmentConfig getEnvironment(@NotNull String environmentName) {
        for (SourceEnvironmentConfig envConfig : environments) {
            if (environmentName.equals(envConfig.environmentName)) {
                return envConfig;
            }
        }
        return null;
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
