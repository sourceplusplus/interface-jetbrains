package com.sourceplusplus.protocol.service

import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName
import io.vertx.codegen.annotations.ProxyGen
import io.vertx.codegen.annotations.VertxGen
import io.vertx.core.AsyncResult
import io.vertx.core.Handler

@ProxyGen
@VertxGen
interface LogCountIndicatorService {
    fun getOccurredCount1(logPattern: String, handler: Handler<AsyncResult<Int>>)
    fun getOccurredCount2(artifactQualifiedName: ArtifactQualifiedName, handler: Handler<AsyncResult<List<Int>>>)
}
