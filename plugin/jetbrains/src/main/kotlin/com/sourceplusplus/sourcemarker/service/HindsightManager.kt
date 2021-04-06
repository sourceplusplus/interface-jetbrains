package com.sourceplusplus.sourcemarker.service

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.breakpoints.XBreakpointListener
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import com.sourceplusplus.protocol.ProtocolErrors
import com.sourceplusplus.protocol.SourceMarkerServices
import com.sourceplusplus.protocol.SourceMarkerServices.Instance.Tracing
import com.sourceplusplus.protocol.artifact.debugger.BreakpointHit
import com.sourceplusplus.protocol.artifact.debugger.HindsightBreakpoint
import com.sourceplusplus.sourcemarker.service.hindsight.BreakpointHitWindowService
import com.sourceplusplus.sourcemarker.discover.TCPServiceDiscoveryBackend
import com.sourceplusplus.sourcemarker.icons.SourceMarkerIcons
import com.sourceplusplus.sourcemarker.service.hindsight.breakpoint.HindsightBreakpointProperties
import io.vertx.core.eventbus.ReplyException
import io.vertx.core.eventbus.ReplyFailure
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.bridge.BridgeEventType
import io.vertx.ext.eventbus.bridge.tcp.impl.protocol.FrameHelper
import io.vertx.kotlin.coroutines.CoroutineVerticle
import org.slf4j.LoggerFactory

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class HindsightManager : CoroutineVerticle(),
    XBreakpointListener<XLineBreakpoint<HindsightBreakpointProperties>> {

    companion object {
        private val log = LoggerFactory.getLogger(LogCountIndicators::class.java)
    }

    override suspend fun start() {
        log.debug("HindsightManager started")
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

    override fun breakpointAdded(breakpoint: XLineBreakpoint<HindsightBreakpointProperties>) {
        Tracing.hindsightDebugger!!.addBreakpoint(HindsightBreakpoint(breakpoint.properties.getLocation())) {
            if (it.succeeded()) {
                breakpoint.properties.setActive(true)

                val project = ProjectManager.getInstance().openProjects[0]
                XDebuggerManager.getInstance(project).breakpointManager.updateBreakpointPresentation(
                    breakpoint, SourceMarkerIcons.EYE_ICON, null
                )
            } else {
                log.error("Failed to add hindsight breakpoint", it.cause())
                val replyException = it.cause() as ReplyException
                if (replyException.failureType() == ReplyFailure.TIMEOUT) {
                    println("here")
                } else {
                    val rawFailure = JsonObject(replyException.message)
                    val debugInfo = rawFailure.getJsonObject("debugInfo")
                    if (debugInfo.getString("type") == ProtocolErrors.ServiceUnavailable.name) {
                        log.warn("Unable to connect to service: " + debugInfo.getString("name"))
                        Notifications.Bus.notify(
                            Notification(
                                "SourceMarker", "Hindsight Breakpoint Failed",
                                "Unable to connect to service: " + debugInfo.getString("name"),
                                NotificationType.ERROR
                            )
                        )
                    } else {
                        it.cause().printStackTrace()
                        log.error("Failed to add hindsight breakpoint", it.cause())
                        Notifications.Bus.notify(
                            Notification(
                                "SourceMarker", "Hindsight Breakpoint Failed",
                                "Failed to add hindsight breakpoint",
                                NotificationType.ERROR
                            )
                        )
                    }
                }

                breakpoint.properties.setActive(false)
                val project = ProjectManager.getInstance().openProjects[0]
                XDebuggerManager.getInstance(project).breakpointManager.updateBreakpointPresentation(
                    breakpoint, SourceMarkerIcons.EYE_SLASH_ICON, null
                )
            }
        }
    }

    override fun breakpointRemoved(breakpoint: XLineBreakpoint<HindsightBreakpointProperties>) {
        Tracing.hindsightDebugger!!.removeBreakpoints(breakpoint.properties.getLocation()) {
            if (it.succeeded()) {
                breakpoint.properties.setActive(false)

                val project = ProjectManager.getInstance().openProjects[0]
                XDebuggerManager.getInstance(project).breakpointManager.updateBreakpointPresentation(
                    breakpoint, SourceMarkerIcons.EYE_SLASH_ICON, null
                )
            } else {
                log.error("Failed to add hindsight breakpoint", it.cause())
                val replyException = it.cause() as ReplyException
                if (replyException.failureType() == ReplyFailure.TIMEOUT) {
                    println("here")
                } else {
                    val rawFailure = JsonObject(replyException.message)
                    val debugInfo = rawFailure.getJsonObject("debugInfo")
                    if (debugInfo.getString("type") == ProtocolErrors.ServiceUnavailable.name) {
                        log.warn("Unable to connect to service: " + debugInfo.getString("name"))
                        Notifications.Bus.notify(
                            Notification(
                                "SourceMarker", "Hindsight Breakpoint Failed",
                                "Unable to connect to service: " + debugInfo.getString("name"),
                                NotificationType.ERROR
                            )
                        )
                    } else {
                        it.cause().printStackTrace()
                        log.error("Failed to add hindsight breakpoint", it.cause())
                        Notifications.Bus.notify(
                            Notification(
                                "SourceMarker", "Hindsight Breakpoint Failed",
                                "Failed to add hindsight breakpoint",
                                NotificationType.ERROR
                            )
                        )
                    }
                }

                breakpoint.properties.setActive(false)
                val project = ProjectManager.getInstance().openProjects[0]
                XDebuggerManager.getInstance(project).breakpointManager.updateBreakpointPresentation(
                    breakpoint, SourceMarkerIcons.EYE_SLASH_ICON, null
                )
            }
        }
    }

    override fun breakpointChanged(breakpoint: XLineBreakpoint<HindsightBreakpointProperties>) {
    }
}
