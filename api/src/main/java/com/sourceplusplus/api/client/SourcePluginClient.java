package com.sourceplusplus.api.client;

import mjson.Json;
import org.modellwerkstatt.javaxbus.EventBus;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.1.0
 * @since 0.1.0
 */
public class SourcePluginClient implements SourceClient {

    public final static String GET_AGENT_WORK_ORDER_ADDRESS = "plugin.get_agent_work_order";
    public final static String TRACES_UPDATED_ADDRESS = "plugin.traces_updated";
    private EventBus pluginEventBus;
    private boolean eventBusBridgeConnected;
    //private String subscriberUuid;

    public SourcePluginClient(String host, int port) {
        pluginEventBus = EventBus.create(host, port);
        SourceClient.initMappers();
    }

    public void tracesUpdated() {
        pluginEventBus.send(TRACES_UPDATED_ADDRESS, Json.object());
    }

    public boolean isEventBusBridgeConnected() {
        return eventBusBridgeConnected;
    }
}
