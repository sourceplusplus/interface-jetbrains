package com.sourceplusplus.protocol.service.tracing

import com.sourceplusplus.protocol.artifact.debugger.Breakpoint
import com.sourceplusplus.protocol.artifact.debugger.Location
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
    fun addBreakpoint(breakpoint: Breakpoint, handler: Handler<AsyncResult<Boolean>>)
    fun removeBreakpoint(breakpoint: Breakpoint, handler: Handler<AsyncResult<Boolean>>)
    fun removeBreakpoints(location: Location, handler: Handler<AsyncResult<List<Breakpoint>>>)
    fun getBreakpoints(handler: Handler<AsyncResult<List<Breakpoint>>>)
}
