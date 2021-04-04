package com.sourceplusplus.sourcemarker.service

import com.sourceplusplus.protocol.SourceMarkerServices
import com.sourceplusplus.protocol.artifact.debugger.BreakpointHit
import com.sourceplusplus.sourcemarker.discover.TCPServiceDiscoveryBackend
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.bridge.BridgeEventType
import io.vertx.ext.eventbus.bridge.tcp.impl.protocol.FrameHelper
import io.vertx.kotlin.coroutines.CoroutineVerticle

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class ProductionBreakpointListener : CoroutineVerticle() {

    override suspend fun start() {
        vertx.eventBus().consumer<JsonObject>("local." + SourceMarkerServices.Provider.Tracing.Event.BREAKPOINT_HIT) {
            val bpHit = Json.decodeValue(it.body().toString(), BreakpointHit::class.java)
            println(bpHit)
        }
        FrameHelper.sendFrame(
            BridgeEventType.REGISTER.name.toLowerCase(),
            SourceMarkerServices.Provider.Tracing.Event.BREAKPOINT_HIT,
            JsonObject(),
            TCPServiceDiscoveryBackend.socket
        )
    }
}
