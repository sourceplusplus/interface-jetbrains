package com.sourceplusplus.sourcemarker.service

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiManager
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.breakpoints.XBreakpointListener
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import com.sourceplusplus.protocol.ProtocolErrors
import com.sourceplusplus.protocol.SourceMarkerServices.Instance.Tracing
import com.sourceplusplus.protocol.SourceMarkerServices.Provide
import com.sourceplusplus.protocol.artifact.debugger.HindsightBreakpoint
import com.sourceplusplus.protocol.artifact.debugger.SourceLocation
import com.sourceplusplus.protocol.artifact.debugger.event.BreakpointHit
import com.sourceplusplus.sourcemarker.discover.TCPServiceDiscoveryBackend
import com.sourceplusplus.sourcemarker.icons.SourceMarkerIcons
import com.sourceplusplus.sourcemarker.service.hindsight.BreakpointHitWindowService
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
        vertx.eventBus().consumer<JsonObject>("local." + Provide.Tracing.HINDSIGHT_BREAKPOINT_SUBSCRIBER) {
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
            Provide.Tracing.HINDSIGHT_BREAKPOINT_SUBSCRIBER,
            JsonObject(),
            TCPServiceDiscoveryBackend.socket
        )
    }

    override fun breakpointAdded(breakpoint: XLineBreakpoint<HindsightBreakpointProperties>) {
        Tracing.hindsightDebugger!!.addBreakpoint(HindsightBreakpoint(breakpoint.properties.getLocation()!!)) {
            if (it.succeeded()) {
                breakpoint.properties.setActive(true)
                breakpoint.properties.setBreakpointId(it.result().id!!)

                val project = ProjectManager.getInstance().openProjects[0]
                XDebuggerManager.getInstance(project).breakpointManager.updateBreakpointPresentation(
                    breakpoint, SourceMarkerIcons.EYE_ICON, null
                )
            } else {
                log.error("Failed to add hindsight breakpoint", it.cause())
                notifyError(it.cause() as ReplyException)

                breakpoint.properties.setActive(false)
                val project = ProjectManager.getInstance().openProjects[0]
                XDebuggerManager.getInstance(project).breakpointManager.updateBreakpointPresentation(
                    breakpoint, SourceMarkerIcons.EYE_SLASH_ICON, null
                )
            }
        }
    }

    override fun breakpointRemoved(breakpoint: XLineBreakpoint<HindsightBreakpointProperties>) {
        if (!breakpoint.properties.getActive()) {
            log.debug("Ignored removing inactive breakpoint")
            return
        }

        Tracing.hindsightDebugger!!.removeBreakpoint(
            HindsightBreakpoint(breakpoint.properties.getLocation()!!, id = breakpoint.properties.getBreakpointId())
        ) {
            if (it.succeeded()) {
                breakpoint.properties.setActive(false)

                val project = ProjectManager.getInstance().openProjects[0]
                XDebuggerManager.getInstance(project).breakpointManager.updateBreakpointPresentation(
                    breakpoint, SourceMarkerIcons.EYE_SLASH_ICON, null
                )
            } else {
                log.error("Failed to add hindsight breakpoint", it.cause())
                notifyError(it.cause() as ReplyException)

                breakpoint.properties.setActive(false)
                val project = ProjectManager.getInstance().openProjects[0]
                XDebuggerManager.getInstance(project).breakpointManager.updateBreakpointPresentation(
                    breakpoint, SourceMarkerIcons.EYE_SLASH_ICON, null
                )
            }
        }
    }

    override fun breakpointChanged(breakpoint: XLineBreakpoint<HindsightBreakpointProperties>) {
        Tracing.hindsightDebugger!!.removeBreakpoint(
            HindsightBreakpoint(breakpoint.properties.getLocation()!!, id = breakpoint.properties.getBreakpointId())
        ) {
            if (it.succeeded()) {
                ApplicationManager.getApplication().runReadAction {
                    val virtualFile = VirtualFileManager.getInstance().findFileByUrl(breakpoint.fileUrl)!!
                    val psiFile: PsiClassOwner = (PsiManager.getInstance(ProjectManager.getInstance().openProjects[0])
                        .findFile(virtualFile) as PsiClassOwner?)!!
                    val qualifiedName = psiFile.classes[0].qualifiedName!!

                    //todo: only copies location atm
                    breakpoint.properties.setLocation(SourceLocation(qualifiedName, breakpoint.line + 1))
                    Tracing.hindsightDebugger!!.addBreakpoint(HindsightBreakpoint(breakpoint.properties.getLocation()!!)) {
                        if (it.succeeded()) {
                            breakpoint.properties.setActive(true)
                            breakpoint.properties.setBreakpointId(it.result().id!!)
                        } else {
                            notifyError(it.cause() as ReplyException)
                        }
                    }
                }
            } else {
                notifyError(it.cause() as ReplyException)
            }
        }
    }

    private fun notifyError(replyException: ReplyException) {
        if (replyException.failureType() == ReplyFailure.TIMEOUT) {
            log.error("Timed out removing hindsight breakpoint")
            Notifications.Bus.notify(
                Notification(
                    "SourceMarker", "Hindsight Breakpoint Failed",
                    "Timed out removing hindsight breakpoint",
                    NotificationType.ERROR
                )
            )
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
                replyException.printStackTrace()
                log.error("Failed to add hindsight breakpoint", replyException)
                Notifications.Bus.notify(
                    Notification(
                        "SourceMarker", "Hindsight Breakpoint Failed",
                        "Failed to add hindsight breakpoint",
                        NotificationType.ERROR
                    )
                )
            }
        }
    }
}
