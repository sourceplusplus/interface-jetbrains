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

/**
 * todo: description
 *
 * @version 0.2.0
 * @since 0.2.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
abstract class APMIntegration extends AbstractVerticle {

    abstract void getEndpointMetrics(String endpointId, ArtifactMetricQuery metricQuery,
                                     Handler<AsyncResult<ArtifactMetricResult>> handler)

    abstract void getTraces(TraceQuery traceQuery, Handler<AsyncResult<TraceQueryResult>> handler)

    abstract void getTraceStack(String appUuid, String traceId,
                                Handler<AsyncResult<TraceSpanStackQueryResult>> handler)

    abstract void getTraceStack(String appUuid, SourceArtifact artifact, TraceSpanStackQuery spanQuery,
                                Handler<AsyncResult<TraceSpanStackQueryResult>> handler)
}
