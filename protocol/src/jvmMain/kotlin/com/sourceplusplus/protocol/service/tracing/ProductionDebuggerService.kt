package com.sourceplusplus.protocol.service.tracing

import com.sourceplusplus.protocol.artifact.debugger.LiveBreakpoint
import com.sourceplusplus.protocol.artifact.debugger.SourceLocation
import io.vertx.codegen.annotations.ProxyGen
import io.vertx.codegen.annotations.VertxGen
import io.vertx.core.AsyncResult
import io.vertx.core.Handler

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@ProxyGen
@VertxGen
interface ProductionDebuggerService {
    fun addBreakpoint(breakpoint: LiveBreakpoint, handler: Handler<AsyncResult<LiveBreakpoint>>)
    fun removeBreakpoint(breakpoint: LiveBreakpoint, handler: Handler<AsyncResult<Boolean>>)
    fun removeBreakpoints(location: SourceLocation, handler: Handler<AsyncResult<Boolean>>)
    fun getBreakpoints(handler: Handler<AsyncResult<List<LiveBreakpoint>>>)
}
