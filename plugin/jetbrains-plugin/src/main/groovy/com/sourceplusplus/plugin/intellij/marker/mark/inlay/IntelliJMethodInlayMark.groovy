package com.sourceplusplus.plugin.intellij.marker.mark.inlay

import com.sourceplusplus.api.bridge.PluginBridgeEndpoints
import com.sourceplusplus.api.model.QueryTimeFrame
import com.sourceplusplus.api.model.artifact.SourceArtifact
import com.sourceplusplus.api.model.config.SourcePortalConfig
import com.sourceplusplus.api.model.metric.ArtifactMetricResult
import com.sourceplusplus.api.model.metric.ArtifactMetricSubscribeRequest
import com.sourceplusplus.api.model.trace.ArtifactTraceResult
import com.sourceplusplus.api.model.trace.ArtifactTraceSubscribeRequest
import com.sourceplusplus.api.model.trace.TraceOrderType
import com.sourceplusplus.api.model.trace.TraceQuery
import com.sourceplusplus.api.model.trace.TraceSpanStackQuery
import com.sourceplusplus.marker.plugin.SourceInlayProvider
import com.sourceplusplus.marker.plugin.SourceMarkerPlugin
import com.sourceplusplus.marker.source.SourceFileMarker
import com.sourceplusplus.marker.source.mark.api.component.api.SourceMarkComponent
import com.sourceplusplus.marker.source.mark.gutter.GutterMark
import com.sourceplusplus.marker.source.mark.inlay.MethodInlayMark
import com.sourceplusplus.marker.source.mark.inlay.config.InlayMarkConfiguration
import com.sourceplusplus.marker.source.mark.inlay.config.InlayMarkVirtualText
import com.sourceplusplus.plugin.SourcePlugin
import com.sourceplusplus.plugin.intellij.marker.mark.gutter.IntelliJGutterMark
import com.sourceplusplus.portal.display.tabs.OverviewTab
import groovy.util.logging.Slf4j
import org.jetbrains.annotations.NotNull
import org.jetbrains.uast.UMethod

import java.awt.*
import java.text.DecimalFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

import static com.sourceplusplus.api.bridge.PluginBridgeEndpoints.ARTIFACT_STATUS_UPDATED
import static com.sourceplusplus.api.model.metric.MetricType.*

/**
 * Extension of the MethodInlayMark for handling IntelliJ.
 *
 * @version 0.3.0
 * @since 0.3.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class IntelliJMethodInlayMark extends MethodInlayMark implements IntelliJInlayMark {

    private static DateTimeFormatter dateTimeFormatter =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())
    private static DecimalFormat decimalFormat = new DecimalFormat(".#")
    private static Color SPP_RED = Color.decode("#e1483b")

    IntelliJMethodInlayMark(SourceFileMarker sourceFileMarker, UMethod psiMethod) {
        super(sourceFileMarker, psiMethod)

//        SourcePlugin.vertx.eventBus().consumer(ARTIFACT_STATUS_UPDATED.address, {
//            updateSourceArtifact(it.body() as SourceArtifact)
//        })
    }

    /**
     * {@inheritDoc}
     */
    @Override
    synchronized void apply(@NotNull SourceMarkComponent sourceMarkComponent, boolean addToMarker) {
        super.apply(sourceMarkComponent, addToMarker)

//        def intelliGutterMark = gutterMark as IntelliJGutterMark
//        def subscribeRequest = ArtifactMetricSubscribeRequest.builder()
//                .appUuid(intelliGutterMark.portal.appUuid)
//                .artifactQualifiedName(intelliGutterMark.portal.portalUI.viewingPortalArtifact)
//                .addMetricTypes(ResponseTime_Average, Throughput_Average, ServiceLevelAgreement_Average)
//                .timeFrame(QueryTimeFrame.LAST_5_MINUTES)
//                .build()
//        SourcePortalConfig.current.getCoreClient(intelliGutterMark.portal.appUuid).subscribeToArtifact(subscribeRequest, {
//            if (it.succeeded()) {
//                log.info("Successfully subscribed to metrics with request: {}", subscribeRequest)
//            } else {
//                log.error("Failed to subscribe to artifact metrics", it.cause())
//            }
//        })
//        SourcePlugin.vertx.eventBus().consumer(PluginBridgeEndpoints.ARTIFACT_METRIC_UPDATED.address, {
//            def metricResult = it.body() as ArtifactMetricResult
//            if (gutterMark.artifactQualifiedName == metricResult.artifactQualifiedName()
//                    && metricResult.timeFrame() == QueryTimeFrame.LAST_5_MINUTES) {
//                //todo: no import of OverviewTab
//                def virtualTextResult = "    "
//                def throughput = "n/a"
//                def response = "n/a RESP"
//                def sla = "n/a SLA"
//                metricResult.artifactMetrics().each {
//                    if (it.metricType() == Throughput_Average) {
//                        double avg = OverviewTab.calculateAverage(it)
//                        throughput = OverviewTab.toPrettyFrequency(avg / 60.0).toUpperCase()
//                    } else if (it.metricType() == ResponseTime_Average) {
//                        double avg = OverviewTab.calculateAverage(it)
//                        response = OverviewTab.toPrettyDuration(avg as int).toUpperCase() + " RESP"
//                    } else if (it.metricType() == ServiceLevelAgreement_Average) {
//                        double avg = OverviewTab.calculateAverage(it)
//                        sla = (avg == 0 ? "0%" : decimalFormat.format(avg / 100.0) + "%") + " SLA"
//                    }
//                }
//                virtualTextResult += "$throughput | $response | $sla"
//
//                def inlayMark = SourceMarkerPlugin.INSTANCE.getSourceMark(metricResult.artifactQualifiedName(), Type.INLAY)
//                if (inlayMark.configuration.virtualText == null) {
//                    inlayMark.configuration.virtualText = new InlayMarkVirtualText(this, virtualTextResult)
//                    inlayMark.configuration.virtualText.textAttributes.setForegroundColor(SPP_RED)
//                }
//                inlayMark.configuration.virtualText.updateVirtualText(virtualTextResult)
//            }
//        })
//
//        def traceSubscribeRequest = ArtifactTraceSubscribeRequest.builder()
//                .appUuid(intelliGutterMark.portal.appUuid)
//                .artifactQualifiedName(intelliGutterMark.portal.portalUI.viewingPortalArtifact)
//                .addOrderTypes(TraceOrderType.FAILED_TRACES)
//                .timeFrame(QueryTimeFrame.LAST_5_MINUTES)
//                .build()
//        SourcePortalConfig.current.getCoreClient(intelliGutterMark.portal.appUuid).subscribeToArtifact(traceSubscribeRequest, {
//            if (it.succeeded()) {
//                log.info("Successfully subscribed to traces with request: {}", subscribeRequest)
//            } else {
//                log.error("Failed to subscribe to artifact traces", it.cause())
//            }
//        })
//        SourcePlugin.vertx.eventBus().consumer(PluginBridgeEndpoints.ARTIFACT_TRACE_UPDATED.address, {
//            def traceResult = it.body() as ArtifactTraceResult
//            def latestFailedTrace = traceResult.traces()[0]
//            def traceStackQuery = TraceSpanStackQuery.builder()
//                    .oneLevelDeep(true)
//                    .traceId(latestFailedTrace.traceIds()[0]).build()
//            SourcePortalConfig.current.getCoreClient(traceResult.appUuid())
//                    .getTraceSpans(traceResult.appUuid(), traceResult.artifactQualifiedName(), traceStackQuery, {
//                        if (it.succeeded()) {
//                            def errorLogs = it.result().traceSpans()[0].logs()[0]
//                            def errorKind = errorLogs.data().get("error.kind").replaceAll("\\w+(\\.)", '')
//                            def errorText = "    $errorKind @ ${dateTimeFormatter.format(errorLogs.time())}"
//
//                            def inlayMark = SourceMarkerPlugin.INSTANCE.getSourceMark(traceResult.artifactQualifiedName(), Type.INLAY)
//                            if (inlayMark.configuration.virtualText == null) {
//                                inlayMark.configuration.virtualText = new InlayMarkVirtualText(this, errorText)
//                                inlayMark.configuration.virtualText.textAttributes.setForegroundColor(SPP_RED)
//                            }
//                            inlayMark.configuration.virtualText.updateVirtualText(errorText)
//                        } else {
//                            log.error("Failed to get trace spans", it.cause())
//                        }
//                    })
//        })
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void updateSourceArtifact(SourceArtifact sourceArtifact) {
//        def gutterMark = SourceMarkerPlugin.INSTANCE.getSourceMark(
//                sourceArtifact.artifactQualifiedName(), Type.GUTTER) as IntelliJGutterMark
//        if (gutterMark) {
//            def inlayMark = gutterMark.getUserData(SourceInlayProvider.tiedInlayMarkKey)
//
//            def traceQuery = TraceQuery.builder()
//                    .orderType(TraceOrderType.FAILED_TRACES)
//                    .pageSize(1)
//                    .appUuid(sourceArtifact.appUuid())
//                    .artifactQualifiedName(sourceArtifact.artifactQualifiedName())
//                    .serviceInstanceId(sourceArtifact.status().latestFailedServiceInstance())
//                    .durationStart(Instant.now().minus(30, ChronoUnit.DAYS))
//                    .durationStop(Instant.now()) //todo: don't look 30 days back
//                    .durationStep("SECOND").build()
//            SourcePortalConfig.current.getCoreClient(sourceArtifact.appUuid()).getTraces(traceQuery, {
//                if (it.succeeded()) {
//                    if (it.result().traces()) {
//                        def latestFailedTrace = it.result().traces()[0]
//                        def traceStackQuery = TraceSpanStackQuery.builder()
//                                .oneLevelDeep(true)
//                                .traceId(latestFailedTrace.traceIds()[0]).build()
//                        SourcePortalConfig.current.getCoreClient(sourceArtifact.appUuid())
//                                .getTraceSpans(sourceArtifact.appUuid(), sourceArtifact.artifactQualifiedName(), traceStackQuery, {
//                                    if (it.succeeded()) {
//                                        def errorLogs = it.result().traceSpans()[0].logs()[0]
//                                        def errorKind = errorLogs.data().get("error.kind").replaceAll("\\w+(\\.)", '')
//                                        def errorText = "    $errorKind @ ${dateTimeFormatter.format(errorLogs.time())}"
//
//                                        if (inlayMark.configuration.virtualText == null) {
//                                            inlayMark.configuration.virtualText = new InlayMarkVirtualText(this, errorText)
//                                            inlayMark.configuration.virtualText.textAttributes.setForegroundColor(SPP_RED)
//                                        }
//                                        inlayMark.configuration.virtualText.updateVirtualText(errorText)
//                                    } else {
//                                        log.error("Failed to get trace spans", it.cause())
//                                    }
//                                })
//                    }
//                } else {
//                    log.error("Failed to get traces", it.cause())
//                }
//            })
//        }
    }
}
