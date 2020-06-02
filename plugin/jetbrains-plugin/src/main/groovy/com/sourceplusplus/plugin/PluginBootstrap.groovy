package com.sourceplusplus.plugin

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import com.intellij.psi.PsiFile
import com.sourceplusplus.api.model.SourceMessage
import com.sourceplusplus.api.model.application.SourceApplication
import com.sourceplusplus.api.model.artifact.SourceArtifact
import com.sourceplusplus.api.model.artifact.SourceArtifactUnsubscribeRequest
import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.api.model.metric.ArtifactMetricResult
import com.sourceplusplus.api.model.metric.ArtifactMetricSubscribeRequest
import com.sourceplusplus.api.model.metric.ArtifactMetrics
import com.sourceplusplus.api.model.metric.TimeFramedMetricType
import com.sourceplusplus.api.model.trace.*
import com.sourceplusplus.marker.SourceFileMarker
import com.sourceplusplus.marker.SourceFileMarkerProvider
import com.sourceplusplus.marker.plugin.SourceMarkerPlugin
import com.sourceplusplus.marker.source.mark.gutter.component.jcef.GutterMarkJcefComponentProvider
import com.sourceplusplus.plugin.coordinate.PluginCoordinator
import com.sourceplusplus.plugin.intellij.marker.IntelliJSourceFileMarker
import com.sourceplusplus.portal.PortalBootstrap
import groovy.util.logging.Slf4j
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.jackson.DatabindCodec
import org.jetbrains.annotations.NotNull

import java.awt.*

/**
 * Used to bootstrap the Source++ Plugin.
 *
 * @version 0.2.6
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class PluginBootstrap extends AbstractVerticle {

    private static SourcePlugin sourcePlugin

    PluginBootstrap(SourcePlugin sourcePlugin) {
        PluginBootstrap.sourcePlugin = sourcePlugin

        //setup SourceMarker
        SourceMarkerPlugin.INSTANCE.enabled = true
        SourceMarkerPlugin.configuration.defaultGutterMarkConfiguration.activateOnMouseHover = false
        SourceMarkerPlugin.configuration.defaultGutterMarkConfiguration.activateOnMouseClick = true
        SourceMarkerPlugin.configuration.defaultGutterMarkConfiguration.activateOnKeyboardShortcut = true
        SourceMarkerPlugin.configuration.defaultGutterMarkConfiguration.componentProvider = new GutterMarkJcefComponentProvider()
        SourceMarkerPlugin.configuration.defaultGutterMarkConfiguration.componentProvider.with {
            defaultConfiguration.preloadJcefBrowser = false
            defaultConfiguration.setComponentSize(new Dimension(775, 250))
            //todo: measure size of editor and make short if it will extend past IDE
            //defaultConfiguration.setComponentSize(new Dimension(620, 250))
        }
        SourceMarkerPlugin.configuration.sourceFileMarkerProvider = new SourceFileMarkerProvider() {
            @Override
            SourceFileMarker createSourceFileMarker(@NotNull PsiFile psiFile) {
                log.debug("Creating source file marker for file: " + psiFile)
                return new IntelliJSourceFileMarker(psiFile)
            }
        }
    }

    @Override
    void start() throws Exception {
        log.info("Source++ Plugin activated. App UUID: " + SourcePluginConfig.current.activeEnvironment.appUuid)
        registerCodecs()
        vertx.deployVerticle(new PluginCoordinator())
        vertx.deployVerticle(new PortalBootstrap(true))
    }

    @Override
    void stop() throws Exception {
        log.info("{} stopped", getClass().getSimpleName())
    }

    static SourcePlugin getSourcePlugin() {
        return sourcePlugin
    }

    private void registerCodecs() {
        DatabindCodec.mapper().setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
        DatabindCodec.mapper().enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
        DatabindCodec.mapper().enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)

        //api
        vertx.eventBus().registerDefaultCodec(SourceApplication.class, SourceMessage.messageCodec(SourceApplication.class))
        vertx.eventBus().registerDefaultCodec(SourceArtifact.class, SourceMessage.messageCodec(SourceArtifact.class))
        vertx.eventBus().registerDefaultCodec(ArtifactMetrics.class, SourceMessage.messageCodec(ArtifactMetrics.class))
        vertx.eventBus().registerDefaultCodec(ArtifactMetricResult.class, SourceMessage.messageCodec(ArtifactMetricResult.class))
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
