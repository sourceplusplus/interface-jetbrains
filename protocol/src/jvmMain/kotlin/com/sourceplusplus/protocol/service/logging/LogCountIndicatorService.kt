package com.sourceplusplus.protocol.service.logging

import com.sourceplusplus.protocol.artifact.log.LogCountSummary
import io.vertx.codegen.annotations.ProxyGen
import io.vertx.codegen.annotations.VertxGen
import io.vertx.core.AsyncResult
import io.vertx.core.Handler

/**
 * todo: description.
 *
 * @since 0.2.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@ProxyGen
@VertxGen
interface LogCountIndicatorService {

    fun getLogCountSummary(handler: Handler<AsyncResult<LogCountSummary>>)
}
