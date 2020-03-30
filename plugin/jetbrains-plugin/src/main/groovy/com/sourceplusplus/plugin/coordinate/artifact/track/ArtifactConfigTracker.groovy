package com.sourceplusplus.plugin.coordinate.artifact.track

import groovy.util.logging.Slf4j
import io.vertx.core.AbstractVerticle

/**
 * todo: description
 *
 * @version 0.2.4
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class ArtifactConfigTracker extends AbstractVerticle {

    //public static final String GET_ARTIFACT_CONFIG = "GetArtifactConfig"

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
