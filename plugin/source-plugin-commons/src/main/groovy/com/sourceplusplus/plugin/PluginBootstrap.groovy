package com.sourceplusplus.plugin

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import com.sourceplusplus.api.bridge.PluginBridgeEndpoints
import com.sourceplusplus.api.model.SourceMessage
import com.sourceplusplus.api.model.application.SourceApplication
import com.sourceplusplus.api.model.artifact.SourceArtifact
import com.sourceplusplus.api.model.artifact.SourceArtifactUnsubscribeRequest
import com.sourceplusplus.api.model.artifact.SourceArtifactVersion
import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.api.model.config.SourceTooltipConfig
import com.sourceplusplus.api.model.metric.ArtifactMetricResult
import com.sourceplusplus.api.model.metric.ArtifactMetricSubscribeRequest
import com.sourceplusplus.api.model.metric.ArtifactMetrics
import com.sourceplusplus.api.model.metric.TimeFramedMetricType
import com.sourceplusplus.api.model.trace.*
import com.sourceplusplus.plugin.coordinate.PluginCoordinator
import com.sourceplusplus.tooltip.TooltipBootstrap
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.Json
import org.modellwerkstatt.javaxbus.EventBus
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Used to bootstrap the Source++ Plugin.
 *
 * @version 0.1.1
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class PluginBootstrap extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(this.name)
    private static SourcePlugin sourcePlugin

    PluginBootstrap(SourcePlugin sourcePlugin) {
        PluginBootstrap.sourcePlugin = sourcePlugin
    }

    @Override
    void start() throws Exception {
        log.info("Source++ Plugin activated. App UUID: " + SourcePluginConfig.current.appUuid)
        registerCodecs()
        vertx.deployVerticle(new PluginCoordinator())
        vertx.deployVerticle(new TooltipBootstrap(sourcePlugin.coreClient, true))

        def pluginEventBus = EventBus.create(SourcePluginConfig.current.apiHost, SourcePluginConfig.current.apiBridgePort)
        pluginEventBus.consumer(PluginBridgeEndpoints.ARTIFACT_CONFIG_UPDATED.address, {
            def artifact = Json.decodeValue(it.bodyAsMJson.toString(), SourceArtifact.class)
            vertx.eventBus().publish(PluginBridgeEndpoints.ARTIFACT_CONFIG_UPDATED.address, artifact)
        })
        pluginEventBus.consumer(PluginBridgeEndpoints.ARTIFACT_METRIC_UPDATED.address, {
            def artifactMetricResult = Json.decodeValue(it.bodyAsMJson.toString(), ArtifactMetricResult.class)
            vertx.eventBus().publish(PluginBridgeEndpoints.ARTIFACT_METRIC_UPDATED.address, artifactMetricResult)
        })
        pluginEventBus.consumer(PluginBridgeEndpoints.ARTIFACT_TRACE_UPDATED.address, {
            def artifactTraceResult = Json.decodeValue(it.bodyAsMJson.toString(), ArtifactTraceResult.class)
            vertx.eventBus().publish(PluginBridgeEndpoints.ARTIFACT_TRACE_UPDATED.address, artifactTraceResult)
        })

        //start plugin ui bridge
        sourcePlugin.startTooltipUIBridge({
            if (it.failed()) {
                log.error("Failed to start tooltip ui bridge", it.cause())
                throw new RuntimeException(it.cause())
            } else {
                log.info("PluginBootstrap started")
                SourceTooltipConfig.current.pluginUIPort = it.result().actualPort()
                log.info("Using tooltip ui bridge port: " + SourceTooltipConfig.current.pluginUIPort)
            }
        })
    }

    @Override
    void stop() throws Exception {
        log.info("{} stopped", getClass().getSimpleName())
    }

    static SourcePlugin getSourcePlugin() {
        return sourcePlugin
    }

    private void registerCodecs() {
        Json.mapper.findAndRegisterModules()
        Json.mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
        Json.mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
        Json.mapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)

        //api
        vertx.eventBus().registerDefaultCodec(PluginSourceFile.class, new PluginSourceFile())
        vertx.eventBus().registerDefaultCodec(SourceApplication.class, SourceMessage.messageCodec(SourceApplication.class))
        vertx.eventBus().registerDefaultCodec(SourceArtifact.class, SourceMessage.messageCodec(SourceArtifact.class))
        vertx.eventBus().registerDefaultCodec(ArtifactMetrics.class, SourceMessage.messageCodec(ArtifactMetrics.class))
        vertx.eventBus().registerDefaultCodec(ArtifactMetricResult.class, SourceMessage.messageCodec(ArtifactMetricResult.class))
        vertx.eventBus().registerDefaultCodec(SourceArtifactVersion.class, SourceMessage.messageCodec(SourceArtifactVersion.class))
        vertx.eventBus().registerDefaultCodec(ArtifactMetricSubscribeRequest.class, SourceMessage.messageCodec(ArtifactMetricSubscribeRequest.class))
        vertx.eventBus().registerDefaultCodec(ArtifactTraceSubscribeRequest.class, SourceMessage.messageCodec(ArtifactTraceSubscribeRequest.class))
        vertx.eventBus().registerDefaultCodec(SourceArtifactUnsubscribeRequest.class, SourceMessage.messageCodec(SourceArtifactUnsubscribeRequest.class))
        vertx.eventBus().registerDefaultCodec(ArtifactTraceResult.class, SourceMessage.messageCodec(ArtifactTraceResult.class))
        vertx.eventBus().registerDefaultCodec(TraceQuery.class, SourceMessage.messageCodec(TraceQuery.class))
        vertx.eventBus().registerDefaultCodec(TraceQueryResult.class, SourceMessage.messageCodec(TraceQueryResult.class))
        vertx.eventBus().registerDefaultCodec(Trace.class, SourceMessage.messageCodec(Trace.class))
        vertx.eventBus().registerDefaultCodec(TraceSpanStackQuery.class, SourceMessage.messageCodec(TraceSpanStackQuery.class))
        vertx.eventBus().registerDefaultCodec(TraceSpanStackQueryResult.class, SourceMessage.messageCodec(TraceSpanStackQueryResult.class))
        vertx.eventBus().registerDefaultCodec(TraceSpan.class, SourceMessage.messageCodec(TraceSpan.class))
        vertx.eventBus().registerDefaultCodec(TimeFramedMetricType.class, SourceMessage.messageCodec(TimeFramedMetricType.class))
    }
}
