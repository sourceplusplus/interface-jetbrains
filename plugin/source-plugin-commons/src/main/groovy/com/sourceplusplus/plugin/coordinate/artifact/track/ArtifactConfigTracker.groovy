package com.sourceplusplus.plugin.coordinate.artifact.track

import com.sourceplusplus.api.bridge.PluginBridgeEndpoints
import com.sourceplusplus.api.model.artifact.SourceArtifact
import com.sourceplusplus.api.model.metric.ArtifactMetricSubscribeRequest
import com.sourceplusplus.api.model.metric.MetricType
import com.sourceplusplus.api.model.trace.ArtifactTraceSubscribeRequest
import com.sourceplusplus.api.model.trace.TraceOrderType
import com.sourceplusplus.portal.coordinate.track.PortalViewTracker
import io.vertx.core.AbstractVerticle
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * todo: description
 *
 * @version 0.2.3
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class ArtifactConfigTracker extends AbstractVerticle {

    //public static final String GET_ARTIFACT_CONFIG = "GetArtifactConfig"

    private static final Logger log = LoggerFactory.getLogger(this.name)

    @Override
    void start() throws Exception {
//        vertx.eventBus().consumer(PluginBridgeEndpoints.ARTIFACT_CONFIG_UPDATED.address, {
//            def sourceArtifact = it.body() as SourceArtifact
//            if (sourceArtifact.config().subscribeAutomatically()) {
//                //subscribe to metrics
//                def metricTypes = [MetricType.Throughput_Average,
//                                   MetricType.ResponseTime_Average,
//                                   MetricType.ServiceLevelAgreement_Average,
//                                   MetricType.ResponseTime_99Percentile,
//                                   MetricType.ResponseTime_95Percentile,
//                                   MetricType.ResponseTime_90Percentile,
//                                   MetricType.ResponseTime_75Percentile,
//                                   MetricType.ResponseTime_50Percentile]
//                def request = ArtifactMetricSubscribeRequest.builder()
//                        .appUuid(sourceArtifact.appUuid())
//                        .artifactQualifiedName(sourceArtifact.artifactQualifiedName())
//                        .timeFrame(PortalViewTracker.currentMetricTimeFrame)
//                        .metricTypes(metricTypes).build()
//                vertx.eventBus().send(PluginArtifactSubscriptionTracker.SUBSCRIBE_TO_ARTIFACT, request)
//
//                //subscribe to traces
//                def traceSubscribeRequest = ArtifactTraceSubscribeRequest.builder()
//                        .appUuid(sourceArtifact.appUuid())
//                        .artifactQualifiedName(sourceArtifact.artifactQualifiedName())
//                        .addOrderTypes(TraceOrderType.LATEST_TRACES, TraceOrderType.SLOWEST_TRACES)
//                        .build()
//                vertx.eventBus().send(PluginArtifactSubscriptionTracker.SUBSCRIBE_TO_ARTIFACT, traceSubscribeRequest)
//            }
//        })
//        log.info("{} started", getClass().getSimpleName())
    }
}
