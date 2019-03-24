package com.sourceplusplus.api.model.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.Objects;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.1.2
 * @since 0.1.0
 */
@JsonAutoDetect
public final class SourceCoreConfig {

    public static final transient SourceCoreConfig current = new SourceCoreConfig();
    public volatile boolean pingEndpointEnabled = true;
    public volatile boolean secureApi = true;
    //String skywalkingVersion();

    private SourceCoreConfig() {
    }

    public void applyConfig(SourceCoreConfig config) {
        Objects.requireNonNull(config);
        pingEndpointEnabled = config.pingEndpointEnabled;
        secureApi = config.secureApi;
    }
}
