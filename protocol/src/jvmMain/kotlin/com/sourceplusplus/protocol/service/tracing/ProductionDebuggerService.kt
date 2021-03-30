package com.sourceplusplus.protocol.service.tracing

import com.sourceplusplus.protocol.artifact.debugger.Breakpoint
import io.vertx.codegen.annotations.ProxyGen
import io.vertx.codegen.annotations.VertxGen

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@ProxyGen
@VertxGen
interface ProductionDebuggerService {
    fun addBreakpoint(): Breakpoint
    fun removeBreakpoint(breakpoint: Breakpoint): Boolean
    fun getBreakpoints(): List<Breakpoint>
}
