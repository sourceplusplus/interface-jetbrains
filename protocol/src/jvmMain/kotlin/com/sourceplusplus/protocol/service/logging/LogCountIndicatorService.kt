package com.sourceplusplus.protocol.service.logging

import com.sourceplusplus.protocol.artifact.log.LogCountSummary
import io.vertx.codegen.annotations.ProxyGen
import io.vertx.codegen.annotations.VertxGen
import io.vertx.core.AsyncResult
import io.vertx.core.Handler

@ProxyGen
@VertxGen
interface LogCountIndicatorService {
    fun getLogCountSummary(handler: Handler<AsyncResult<LogCountSummary>>)
}
