package com.sourceplusplus.api.model.config;

import com.google.common.collect.Maps;
import com.sourceplusplus.api.client.SourceCoreClient;

import java.util.Map;
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
    private volatile transient Map<String, SourceCoreClient> coreClients = Maps.newConcurrentMap();

    private SourcePortalConfig() {
    }

    public void applyConfig(SourcePortalConfig config) {
        Objects.requireNonNull(config);
        appUuid = config.appUuid;
        pluginUIPort = config.pluginUIPort;
    }

    public SourceCoreClient getCoreClient(String appUuid) {
        return coreClients.get(appUuid);
    }

    public void addCoreClient(String appUuid, SourceCoreClient coreClient) {
        coreClients.put(Objects.requireNonNull(appUuid), Objects.requireNonNull(coreClient));
    }
}
