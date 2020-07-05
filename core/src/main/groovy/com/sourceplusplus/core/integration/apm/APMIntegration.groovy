package com.sourceplusplus.core.integration.apm

import com.sourceplusplus.api.model.artifact.SourceArtifact
import com.sourceplusplus.api.model.metric.ArtifactMetricQuery
import com.sourceplusplus.api.model.metric.ArtifactMetricResult
import com.sourceplusplus.api.model.trace.TraceQuery
import com.sourceplusplus.api.model.trace.TraceQueryResult
import com.sourceplusplus.api.model.trace.TraceSpanStackQuery
import com.sourceplusplus.api.model.trace.TraceSpanStackQueryResult
import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.json.JsonArray

import java.time.Instant

/**
 * Represents integration with an APM.
 *
 * @version 0.3.1
 * @since 0.2.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
abstract class APMIntegration extends AbstractVerticle {

    abstract void getAllServices(Instant start, Instant end, String step, Handler<AsyncResult<JsonArray>> handler)

    abstract void getServiceInstances(Instant start, Instant end, String step, String serviceId,
                                      Handler<AsyncResult<JsonArray>> handler)

    abstract void getActiveServiceInstances(Handler<AsyncResult<JsonArray>> handler)

    abstract void getEndpointMetrics(String endpointId, ArtifactMetricQuery metricQuery,
                                     Handler<AsyncResult<ArtifactMetricResult>> handler)

    abstract void getTraces(TraceQuery traceQuery, Handler<AsyncResult<TraceQueryResult>> handler)

    abstract void getTraceStack(String appUuid, String traceId,
                                Handler<AsyncResult<TraceSpanStackQueryResult>> handler)

    abstract void getTraceStack(String appUuid, SourceArtifact artifact, TraceSpanStackQuery spanQuery,
                                Handler<AsyncResult<TraceSpanStackQueryResult>> handler)
}
