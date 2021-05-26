package com.sourceplusplus.protocol.service.live

import com.sourceplusplus.protocol.instrument.LiveInstrument
import com.sourceplusplus.protocol.instrument.LiveSourceLocation
import com.sourceplusplus.protocol.instrument.breakpoint.LiveBreakpoint
import com.sourceplusplus.protocol.instrument.log.LiveLog
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
interface LiveInstrumentService {
    fun addLiveInstrument(instrument: LiveInstrument, handler: Handler<AsyncResult<LiveInstrument>>)
    fun removeLiveInstrument(id: String, handler: Handler<AsyncResult<Boolean>>)
    fun removeLiveInstruments(location: LiveSourceLocation, handler: Handler<AsyncResult<Boolean>>)
    fun getLiveInstruments(handler: Handler<AsyncResult<List<LiveInstrument>>>)
    fun getLiveBreakpoints(handler: Handler<AsyncResult<List<LiveBreakpoint>>>)
    fun getLiveLogs(handler: Handler<AsyncResult<List<LiveLog>>>)
}
