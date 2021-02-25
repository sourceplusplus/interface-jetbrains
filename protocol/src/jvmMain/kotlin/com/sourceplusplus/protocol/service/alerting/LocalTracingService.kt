package com.sourceplusplus.protocol.service.alerting

import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName
import com.sourceplusplus.protocol.artifact.QueryTimeFrame
import com.sourceplusplus.protocol.artifact.trace.TraceResult
import io.vertx.codegen.annotations.ProxyGen
import io.vertx.codegen.annotations.VertxGen
import io.vertx.core.AsyncResult
import io.vertx.core.Handler

@ProxyGen
@VertxGen
interface LocalTracingService {

    fun getTraceResult(
        artifactQualifiedName: ArtifactQualifiedName,
        queryTimeFrame: QueryTimeFrame,
        handler: Handler<AsyncResult<TraceResult>>
    )
}
