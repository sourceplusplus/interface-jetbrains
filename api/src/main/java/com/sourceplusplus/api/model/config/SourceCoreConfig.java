package com.sourceplusplus.api.model.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.Objects;

/**
 * Holds the current configuration used by the core.
 *
 * @version 0.3.1
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@JsonAutoDetect
public final class SourceCoreConfig {

    public static final transient SourceCoreConfig current = new SourceCoreConfig();
    public volatile boolean pingEndpointAvailable = true;
    public volatile boolean secureApi = true;
    //String skywalkingVersion();

    private SourceCoreConfig() {
    }

    public void applyConfig(SourceCoreConfig config) {
        Objects.requireNonNull(config);
        pingEndpointAvailable = config.pingEndpointAvailable;
        secureApi = config.secureApi;
    }
}
