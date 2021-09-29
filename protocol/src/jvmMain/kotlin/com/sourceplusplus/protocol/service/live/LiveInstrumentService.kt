package com.sourceplusplus.protocol.service.live

import com.sourceplusplus.protocol.instrument.LiveInstrument
import com.sourceplusplus.protocol.instrument.LiveInstrumentBatch
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
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@ProxyGen
@VertxGen
interface LiveInstrumentService {
    fun addLiveInstrument(instrument: LiveInstrument, handler: Handler<AsyncResult<LiveInstrument>>)
    fun addLiveInstruments(batch: LiveInstrumentBatch, handler: Handler<AsyncResult<List<LiveInstrument>>>)
    fun removeLiveInstrument(id: String, handler: Handler<AsyncResult<LiveInstrument?>>)
    fun removeLiveInstruments(location: LiveSourceLocation, handler: Handler<AsyncResult<List<LiveInstrument>>>)
    fun getLiveInstrumentById(id: String, handler: Handler<AsyncResult<LiveInstrument?>>)
    fun getLiveInstrumentsByIds(ids: List<String>, handler: Handler<AsyncResult<List<LiveInstrument>>>)
    fun getLiveInstruments(handler: Handler<AsyncResult<List<LiveInstrument>>>)
    fun getLiveBreakpoints(handler: Handler<AsyncResult<List<LiveBreakpoint>>>)
    fun getLiveLogs(handler: Handler<AsyncResult<List<LiveLog>>>)
    fun clearLiveInstruments(handler: Handler<AsyncResult<Boolean>>)
    fun clearLiveBreakpoints(handler: Handler<AsyncResult<Boolean>>)
    fun clearLiveLogs(handler: Handler<AsyncResult<Boolean>>)
}
