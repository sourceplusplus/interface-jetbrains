package com.sourceplusplus.api.bridge;

/**
 * Includes the endpoints which can be bridged to the Source++ Plugin.
 * Public event endpoints represent endpoints which should only be subscribed to.
 * //todo: rename public-events to subscribe-events
 *
 * @version 0.3.1
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
public enum PluginBridgeEndpoints {

    INTEGRATION_INFO_UPDATED("public-events.INTEGRATION_INFO_UPDATED"),
    ARTIFACT_CONFIG_UPDATED("public-events.ARTIFACT_CONFIG_UPDATED"),
    ARTIFACT_STATUS_UPDATED("public-events.ARTIFACT_STATUS_UPDATED"),
    ARTIFACT_METRIC_UPDATED("public-events.ARTIFACT_METRIC_UPDATED"),
    ARTIFACT_TRACE_UPDATED("public-events.ARTIFACT_TRACE_UPDATED"),
    CAN_NAVIGATE_TO_ARTIFACT("CAN_NAVIGATE_TO_ARTIFACT"),
    NAVIGATE_TO_ARTIFACT("NAVIGATE_TO_ARTIFACT");

    private final String address;

    PluginBridgeEndpoints(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public boolean isPublicEvent() {
        return address.startsWith("public-events.");
    }
}
