package com.sourceplusplus.api.model.config;

import com.sourceplusplus.api.client.SourceCoreClient;

import java.util.Objects;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.0
 * @since 0.1.0
 */
public class SourcePortalConfig {

    public static final SourcePortalConfig current = new SourcePortalConfig();
    public volatile String appUuid = null;
    public volatile int pluginUIPort = -1;
    public volatile transient SourceCoreClient coreClient;

    private SourcePortalConfig() {
    }

    public void applyConfig(SourcePortalConfig config) {
        Objects.requireNonNull(config);
        appUuid = config.appUuid;
        pluginUIPort = config.pluginUIPort;
    }
}
