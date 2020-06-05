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
import org.jetbrains.annotations.NotNull

import java.time.Instant

/**
 * Represents integration with an APM.
 *
 * @version 0.2.6
 * @since 0.2.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
abstract class APMIntegration extends AbstractVerticle {

    abstract void getAllServices(@NotNull Instant start, @NotNull Instant end, @NotNull String step,
                                 @NotNull Handler<AsyncResult<JsonArray>> handler)

    abstract void getServiceInstances(@NotNull Instant start, @NotNull Instant end, @NotNull String step,
                                      @NotNull String serviceId,
                                      @NotNull Handler<AsyncResult<JsonArray>> handler)

    abstract void getActiveServiceInstances(@NotNull Handler<AsyncResult<JsonArray>> handler)

    abstract void getEndpointMetrics(@NotNull String endpointId, @NotNull ArtifactMetricQuery metricQuery,
                                     @NotNull Handler<AsyncResult<ArtifactMetricResult>> handler)

    abstract void getTraces(@NotNull TraceQuery traceQuery,
                            @NotNull Handler<AsyncResult<TraceQueryResult>> handler)

    abstract void getTraceStack(@NotNull String appUuid, @NotNull String traceId,
                                @NotNull Handler<AsyncResult<TraceSpanStackQueryResult>> handler)

    abstract void getTraceStack(@NotNull String appUuid, @NotNull SourceArtifact artifact,
                                @NotNull TraceSpanStackQuery spanQuery,
                                @NotNull Handler<AsyncResult<TraceSpanStackQueryResult>> handler)
}
