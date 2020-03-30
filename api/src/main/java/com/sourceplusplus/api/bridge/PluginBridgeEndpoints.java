package com.sourceplusplus.api.bridge;

/**
 * Includes the endpoints which can be bridged from Source++ Core to Source++ Plugin.
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.4
 * @since 0.1.0
 */
public enum PluginBridgeEndpoints {

    ARTIFACT_CONFIG_UPDATED("public-events.ARTIFACT_CONFIG_UPDATED"),
    ARTIFACT_METRIC_UPDATED("public-events.ARTIFACT_METRIC_UPDATED"),
    ARTIFACT_TRACE_UPDATED("public-events.ARTIFACT_TRACE_UPDATED"),
    CAN_NAVIGATE_TO_ARTIFACT("public-events.CAN_NAVIGATE_TO_ARTIFACT"),
    NAVIGATE_TO_ARTIFACT("public-events.NAVIGATE_TO_ARTIFACT");

    private final String address;

    PluginBridgeEndpoints(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }
}
