package com.sourceplusplus.api.bridge;

import com.sourceplusplus.api.model.artifact.SourceArtifact;
import com.sourceplusplus.api.model.metric.ArtifactMetricResult;
import com.sourceplusplus.api.model.trace.ArtifactTraceResult;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.WebSocketFrame;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

/**
 * todo: this
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.0
 * @since 0.2.0
 */
public class SourceBridgeClient {

    private final Vertx vertx;
    private final String apiHost;
    private final int apiPort;

    public SourceBridgeClient(Vertx vertx, String apiHost, int apiPort) {
        this.vertx = vertx;
        this.apiHost = apiHost;
        this.apiPort = apiPort;
    }

    public void setupSubscriptions() {
        HttpClient client = vertx.createHttpClient();
        client.websocket(apiPort, apiHost, "/eventbus/websocket", ws -> {
            JsonObject pingMsg = new JsonObject().put("type", "ping");
            ws.writeFrame(WebSocketFrame.textFrame(pingMsg.encode(), true));
            vertx.setPeriodic(5000, it -> ws.writeFrame(WebSocketFrame.textFrame(pingMsg.encode(), true)));

            JsonObject msg = new JsonObject().put("type", "register")
                    .put("address", PluginBridgeEndpoints.ARTIFACT_CONFIG_UPDATED.getAddress());
            ws.writeFrame(WebSocketFrame.textFrame(msg.encode(), true));
            msg = new JsonObject().put("type", "register")
                    .put("address", PluginBridgeEndpoints.ARTIFACT_METRIC_UPDATED.getAddress());
            ws.writeFrame(WebSocketFrame.textFrame(msg.encode(), true));
            msg = new JsonObject().put("type", "register")
                    .put("address", PluginBridgeEndpoints.ARTIFACT_TRACE_UPDATED.getAddress());
            ws.writeFrame(WebSocketFrame.textFrame(msg.encode(), true));
            ws.handler(it -> {
                JsonObject ob = new JsonObject(it.toString());
                if (PluginBridgeEndpoints.ARTIFACT_CONFIG_UPDATED.getAddress().equals(ob.getString("address"))) {
                    handleArtifactConfigUpdated(ob);
                } else if (PluginBridgeEndpoints.ARTIFACT_METRIC_UPDATED.getAddress().equals(ob.getString("address"))) {
                    handleArtifactMetricUpdated(ob);
                } else if (PluginBridgeEndpoints.ARTIFACT_TRACE_UPDATED.getAddress().equals(ob.getString("address"))) {
                    handleArtifactTraceUpdated(ob);
                } else {
                    throw new IllegalArgumentException("Unsupported bridge address: " + ob.getString("address"));
                }
            });
        });
    }

    private void handleArtifactConfigUpdated(JsonObject msg) {
        SourceArtifact artifact = Json.decodeValue(msg.getJsonObject("body").toString(), SourceArtifact.class);
        vertx.eventBus().publish(PluginBridgeEndpoints.ARTIFACT_CONFIG_UPDATED.getAddress(), artifact);
    }

    private void handleArtifactMetricUpdated(JsonObject msg) {
        ArtifactMetricResult artifactMetricResult = Json.decodeValue(msg.getJsonObject("body").toString(), ArtifactMetricResult.class);
        vertx.eventBus().publish(PluginBridgeEndpoints.ARTIFACT_METRIC_UPDATED.getAddress(), artifactMetricResult);
    }

    private void handleArtifactTraceUpdated(JsonObject msg) {
        ArtifactTraceResult artifactTraceResult = Json.decodeValue(msg.getJsonObject("body").toString(), ArtifactTraceResult.class);
        vertx.eventBus().publish(PluginBridgeEndpoints.ARTIFACT_TRACE_UPDATED.getAddress(), artifactTraceResult);
    }
}
