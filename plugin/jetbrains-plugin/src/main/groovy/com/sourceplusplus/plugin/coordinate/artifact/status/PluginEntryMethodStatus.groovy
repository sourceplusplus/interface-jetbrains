package com.sourceplusplus.plugin.coordinate.artifact.status

import com.intellij.openapi.application.ApplicationManager
import com.sourceplusplus.api.bridge.PluginBridgeEndpoints
import com.sourceplusplus.api.model.QueryTimeFrame
import com.sourceplusplus.api.model.artifact.SourceArtifact
import com.sourceplusplus.api.model.config.SourcePortalConfig
import com.sourceplusplus.api.model.integration.IntegrationInfo
import com.sourceplusplus.api.model.metric.ArtifactMetricResult
import com.sourceplusplus.api.model.metric.ArtifactMetricSubscribeRequest
import com.sourceplusplus.marker.MarkerUtils
import com.sourceplusplus.marker.plugin.SourceMarkerPlugin
import com.sourceplusplus.marker.source.mark.api.SourceMark
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEvent
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventListener
import com.sourceplusplus.marker.source.mark.gutter.MethodGutterMark
import com.sourceplusplus.marker.source.mark.gutter.event.GutterMarkEventCode
import com.sourceplusplus.plugin.SourcePlugin
import com.sourceplusplus.plugin.coordinate.integration.IntegrationInfoTracker
import com.sourceplusplus.plugin.intellij.marker.mark.gutter.IntelliJGutterMark
import com.sourceplusplus.plugin.intellij.marker.mark.inlay.IntelliJVirtualText
import com.sourceplusplus.portal.display.tabs.OverviewTab
import groovy.util.logging.Slf4j
import io.vertx.core.AbstractVerticle
import org.jetbrains.annotations.NotNull

import java.text.DecimalFormat

import static com.sourceplusplus.api.bridge.PluginBridgeEndpoints.ARTIFACT_CONFIG_UPDATED
import static com.sourceplusplus.api.bridge.PluginBridgeEndpoints.INTEGRATION_INFO_UPDATED
import static com.sourceplusplus.api.model.metric.MetricType.*

/**
 * Keeps track of entry method inlay marks.
 *
 * @version 0.3.2
 * @since 0.3.1
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class PluginEntryMethodStatus extends AbstractVerticle {

    private static final DecimalFormat decimalFormat = new DecimalFormat(".#")
    private static final Stack<SourceArtifact> PENDING_ARTIFACTS = new Stack<>()

    @Override
    void start() throws Exception {
        SourcePlugin.vertx.eventBus().consumer(INTEGRATION_INFO_UPDATED.address, {
            def integration = it.body() as IntegrationInfo
            if ((integration.id() == "apache_skywalking" && integration.enabled())
                    || IntegrationInfoTracker.getActiveIntegrationInfo("apache_skywalking")) {
                while (!PENDING_ARTIFACTS.isEmpty()) {
                    subscribeToEntryMethodArtifact(PENDING_ARTIFACTS.pop())
                }
            }
        })

        //listen for entry methods to add inlay marks to
        SourceMarkerPlugin.INSTANCE.addGlobalSourceMarkEventListener(new SourceMarkEventListener() {
            @Override
            void handleEvent(@NotNull SourceMarkEvent sourceMarkEvent) {
                if (sourceMarkEvent.eventCode == GutterMarkEventCode.GUTTER_MARK_VISIBLE) {
                    def gutterMark = sourceMarkEvent.sourceMark as IntelliJGutterMark
                    if (!IntegrationInfoTracker.getActiveIntegrationInfo("apache_skywalking")) {
                        PENDING_ARTIFACTS.push(gutterMark.sourceArtifact)
                    } else if (gutterMark.sourceArtifact.config().endpoint()) {
                        subscribeToEntryMethodArtifact(gutterMark.sourceArtifact)
                    }
                }
            }
        })
        SourcePlugin.vertx.eventBus().consumer(ARTIFACT_CONFIG_UPDATED.address, {
            def artifact = it.body() as SourceArtifact
            if (!IntegrationInfoTracker.getActiveIntegrationInfo("apache_skywalking")) {
                PENDING_ARTIFACTS.push(artifact)
            } else if (artifact.config().endpoint()) {
                subscribeToEntryMethodArtifact(artifact)
            }
        })
        SourcePlugin.vertx.eventBus().consumer(PluginBridgeEndpoints.ARTIFACT_METRIC_UPDATED.address, {
            def metricResult = it.body() as ArtifactMetricResult
            if (metricResult.timeFrame() == QueryTimeFrame.LAST_5_MINUTES) {
                //todo: no import of OverviewTab
                def throughput = "n/a"
                def response = "n/a RESP"
                def sla = "n/a SLA"
                metricResult.artifactMetrics().each {
                    if (it.metricType() == Throughput_Average) {
                        double avg = OverviewTab.calculateAverage(it)
                        throughput = OverviewTab.toPrettyFrequency(avg / 60.0).toUpperCase()
                    } else if (it.metricType() == ResponseTime_Average) {
                        double avg = OverviewTab.calculateAverage(it)
                        response = OverviewTab.toPrettyDuration(avg as int).toUpperCase() + " RESP"
                    } else if (it.metricType() == ServiceLevelAgreement_Average) {
                        double avg = OverviewTab.calculateAverage(it)
                        sla = (avg == 0 ? "0%" : decimalFormat.format(avg / 100.0) + "%") + " SLA"
                    }
                }

                def gutterMark = SourceMarkerPlugin.INSTANCE.getSourceMark(metricResult.artifactQualifiedName(),
                        SourceMark.Type.GUTTER) as MethodGutterMark
                if (gutterMark) {
                    updateEntryMethodVirtualText(gutterMark, "$throughput | $response | $sla")
                }
            }
        })
    }

    private static void updateEntryMethodVirtualText(MethodGutterMark gutterMark, String virtualTextResult) {
        ApplicationManager.getApplication().invokeLater {
            def inlayMark = MarkerUtils.getOrCreateMethodInlayMark(
                    gutterMark.sourceFileMarker, gutterMark.psiElement.nameIdentifier)
            if (!inlayMark.sourceFileMarker.containsSourceMark(inlayMark)) inlayMark.apply(true)
            if (inlayMark.configuration.virtualText == null) {
                inlayMark.configuration.virtualText = new IntelliJVirtualText(inlayMark, virtualTextResult, false)
            }

            def virtualText = (IntelliJVirtualText) inlayMark.configuration.virtualText
            virtualText.updateEntryMethodStatus(virtualTextResult)
        }
    }

    private static void subscribeToEntryMethodArtifact(SourceArtifact sourceArtifact) {
        def subscribeRequest = ArtifactMetricSubscribeRequest.builder()
                .appUuid(sourceArtifact.appUuid())
                .artifactQualifiedName(sourceArtifact.artifactQualifiedName())
                .addMetricTypes(ResponseTime_Average, Throughput_Average, ServiceLevelAgreement_Average)
                .timeFrame(QueryTimeFrame.LAST_5_MINUTES)
                .build()
        SourcePortalConfig.current.getCoreClient(sourceArtifact.appUuid()).subscribeToArtifact(subscribeRequest, {
            if (it.succeeded()) {
                log.info("Successfully subscribed to metrics with request: {}", subscribeRequest)
            } else {
                log.error("Failed to subscribe to artifact metrics", it.cause())
            }
        })
    }
}
