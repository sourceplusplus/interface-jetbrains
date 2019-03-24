package com.sourceplusplus.api.model.config;

import java.util.Objects;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.0
 * @since 0.1.0
 */
public class SourceTooltipConfig {

    public static final SourceTooltipConfig current = new SourceTooltipConfig();
    public volatile int apiBridgePort = 7000;
    public volatile String appUuid = null;
    public volatile int pluginUIPort = -1;

    private SourceTooltipConfig() {
    }

    public void applyConfig(SourceTooltipConfig config) {
        Objects.requireNonNull(config);
        appUuid = config.appUuid;
        pluginUIPort = config.pluginUIPort;
    }
}
