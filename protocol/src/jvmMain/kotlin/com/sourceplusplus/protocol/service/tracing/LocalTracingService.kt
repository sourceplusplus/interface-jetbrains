package com.sourceplusplus.protocol.service.tracing

import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName
import com.sourceplusplus.protocol.artifact.trace.TraceOrderType
import com.sourceplusplus.protocol.artifact.trace.TraceResult
import io.vertx.codegen.annotations.ProxyGen
import io.vertx.codegen.annotations.VertxGen
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import kotlinx.datetime.Instant

@ProxyGen
@VertxGen
interface LocalTracingService {

    fun getTraceResult(
        artifactQualifiedName: ArtifactQualifiedName,
        start: Instant,
        stop: Instant,
        orderType: TraceOrderType,
        pageSize: Int,
        pageNumber: Int,
        handler: Handler<AsyncResult<TraceResult>>
    )
}
