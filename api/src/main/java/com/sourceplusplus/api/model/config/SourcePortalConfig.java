package com.sourceplusplus.api.model.config;

import com.google.common.collect.Maps;
import com.sourceplusplus.api.client.SourceCoreClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Holds the current configuration used by the portal.
 *
 * @version 0.3.1
 * @since 0.2.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
public class SourcePortalConfig {

    public static final SourcePortalConfig current = new SourcePortalConfig();
    public volatile int pluginUIPort = -1;
    private final transient Map<String, SourceCoreClient> coreClients = Maps.newConcurrentMap();

    private SourcePortalConfig() {
    }

    public void applyConfig(SourcePortalConfig config) {
        Objects.requireNonNull(config);
        pluginUIPort = config.pluginUIPort;
    }

    public SourceCoreClient getCoreClient(String appUuid) {
        return coreClients.get(appUuid);
    }

    public void addCoreClient(String appUuid, SourceCoreClient coreClient) {
        coreClients.put(Objects.requireNonNull(appUuid), Objects.requireNonNull(coreClient));
    }

    public Map<String, SourceCoreClient> getCoreClients() {
        return new HashMap<>(coreClients);
    }
}
