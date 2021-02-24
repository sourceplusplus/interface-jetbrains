package com.sourceplusplus.mentor.service

import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName
import io.vertx.codegen.annotations.ProxyGen
import io.vertx.codegen.annotations.VertxGen
import io.vertx.core.AsyncResult
import io.vertx.core.Handler

@ProxyGen
@VertxGen
interface LogCountIndicatorService {
    fun getOccurredCount(logPattern: String, handler: Handler<AsyncResult<Int>>)
    fun getOccurredCount(artifactQualifiedName: ArtifactQualifiedName, handler: Handler<AsyncResult<List<Int>>>)
}
