package com.sourceplusplus.sourcemarker.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.ProjectManager
import com.sourceplusplus.protocol.SourceMarkerServices
import com.sourceplusplus.protocol.artifact.debugger.BreakpointHit
import com.sourceplusplus.protocol.artifact.exception.JvmStackTrace
import com.sourceplusplus.sourcemarker.SourceMarkerPlugin
import com.sourceplusplus.sourcemarker.service.hindsight.BreakpointHitWindowService
import com.sourceplusplus.sourcemarker.discover.TCPServiceDiscoveryBackend
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.bridge.BridgeEventType
import io.vertx.ext.eventbus.bridge.tcp.impl.protocol.FrameHelper
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class HindsightBreakpointListener : CoroutineVerticle() {

    companion object {
        private val log = LoggerFactory.getLogger(LogCountIndicators::class.java)
    }

    override suspend fun start() {
        log.debug("HindsightBreakpointListener started")
        vertx.eventBus().consumer<JsonObject>("local." + SourceMarkerServices.Provider.Tracing.Event.BREAKPOINT_HIT) {
            log.info("Received breakpoint hit")

            val bpHit = Json.decodeValue(it.body().toString(), BreakpointHit::class.java)
            ApplicationManager.getApplication().invokeLater {
                val project = ProjectManager.getInstance().openProjects[0]
                BreakpointHitWindowService.getInstance(project).addBreakpointHit(bpHit)
            }
        }

        //register listener
        FrameHelper.sendFrame(
            BridgeEventType.REGISTER.name.toLowerCase(),
            SourceMarkerServices.Provider.Tracing.Event.BREAKPOINT_HIT,
            JsonObject(),
            TCPServiceDiscoveryBackend.socket
        )
    }
}
