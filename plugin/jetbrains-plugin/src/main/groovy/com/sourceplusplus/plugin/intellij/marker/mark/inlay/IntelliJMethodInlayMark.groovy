package com.sourceplusplus.plugin.intellij.marker.mark.inlay

import com.sourceplusplus.api.bridge.PluginBridgeEndpoints
import com.sourceplusplus.api.model.QueryTimeFrame
import com.sourceplusplus.api.model.artifact.SourceArtifact
import com.sourceplusplus.api.model.config.SourcePortalConfig
import com.sourceplusplus.api.model.trace.ArtifactTraceResult
import com.sourceplusplus.api.model.trace.ArtifactTraceSubscribeRequest
import com.sourceplusplus.api.model.trace.TraceOrderType
import com.sourceplusplus.marker.source.SourceFileMarker
import com.sourceplusplus.marker.source.mark.gutter.GutterMark
import com.sourceplusplus.marker.source.mark.inlay.MethodInlayMark
import com.sourceplusplus.marker.source.mark.inlay.config.InlayMarkVirtualText
import com.sourceplusplus.plugin.SourcePlugin
import com.sourceplusplus.plugin.intellij.marker.mark.gutter.IntelliJGutterMark
import groovy.util.logging.Slf4j
import org.jetbrains.uast.UMethod

/**
 * Extension of the MethodInlayMark for handling IntelliJ.
 *
 * @version 0.3.0
 * @since 0.3.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class IntelliJMethodInlayMark extends MethodInlayMark implements IntelliJInlayMark {

    IntelliJMethodInlayMark(SourceFileMarker sourceFileMarker, UMethod psiMethod) {
        super(sourceFileMarker, psiMethod)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void apply(GutterMark gutterMark) {
        super.apply(gutterMark)

        def intelliGutterMark = gutterMark as IntelliJGutterMark
        def subscribeRequest = ArtifactTraceSubscribeRequest.builder()
                .appUuid(intelliGutterMark.portal.appUuid)
                .artifactQualifiedName(intelliGutterMark.portal.portalUI.viewingPortalArtifact)
                .addOrderTypes(TraceOrderType.LATEST_TRACES) //todo: total traces
                .timeFrame(QueryTimeFrame.LAST_5_MINUTES)
                .build()
        SourcePortalConfig.current.getCoreClient(intelliGutterMark.portal.appUuid).subscribeToArtifact(subscribeRequest, {
            if (it.succeeded()) {
                log.info("Successfully subscribed to traces with request: {}", subscribeRequest)
            } else {
                log.error("Failed to subscribe to artifact traces", it.cause())
            }
        })
        SourcePlugin.vertx.eventBus().consumer(PluginBridgeEndpoints.ARTIFACT_TRACE_UPDATED.address, {
            def artifactTraceResult = it.body() as ArtifactTraceResult
            if (gutterMark.artifactQualifiedName == artifactTraceResult.artifactQualifiedName()
                    && artifactTraceResult.orderType() == TraceOrderType.LATEST_TRACES) {
                def virtualTextResult = "    "
                virtualTextResult += artifactTraceResult.total() + " requests, todo failed - Last 5 Minutes"

                if (configuration.virtualText == null) {
                    configuration.virtualText = new InlayMarkVirtualText(this, virtualTextResult)
                }
                configuration.virtualText.updateVirtualText(virtualTextResult)
            }
        })
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void updateSourceArtifact(SourceArtifact sourceArtifact) {
        println("here")
    }
}
