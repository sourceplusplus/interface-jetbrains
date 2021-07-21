package com.sourceplusplus.sourcemarker.service

import com.intellij.debugger.engine.evaluation.TextWithImportsImpl
import com.intellij.debugger.impl.DebuggerUtilsEx
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiManager
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.breakpoints.*
import com.intellij.xdebugger.impl.breakpoints.XBreakpointBase
import com.intellij.xdebugger.impl.breakpoints.XLineBreakpointImpl
import com.sourceplusplus.marker.SourceMarker
import com.sourceplusplus.protocol.ProtocolAddress.Global.ArtifactLogUpdated
import com.sourceplusplus.protocol.SourceMarkerServices.Instance
import com.sourceplusplus.protocol.SourceMarkerServices.Provide
import com.sourceplusplus.protocol.error.AccessDenied
import com.sourceplusplus.protocol.instrument.LiveInstrumentEvent
import com.sourceplusplus.protocol.instrument.LiveInstrumentEventType
import com.sourceplusplus.protocol.instrument.LiveSourceLocation
import com.sourceplusplus.protocol.instrument.breakpoint.LiveBreakpoint
import com.sourceplusplus.protocol.instrument.breakpoint.event.LiveBreakpointHit
import com.sourceplusplus.protocol.instrument.breakpoint.event.LiveBreakpointRemoved
import com.sourceplusplus.protocol.instrument.log.LiveLog
import com.sourceplusplus.protocol.instrument.log.event.LiveLogHit
import com.sourceplusplus.protocol.instrument.log.event.LiveLogRemoved
import com.sourceplusplus.sourcemarker.PluginBundle.message
import com.sourceplusplus.sourcemarker.discover.TCPServiceDiscoveryBackend
import com.sourceplusplus.sourcemarker.icons.SourceMarkerIcons
import com.sourceplusplus.sourcemarker.mark.SourceMarkKeys
import com.sourceplusplus.sourcemarker.search.SourceMarkSearch
import com.sourceplusplus.sourcemarker.service.breakpoint.BreakpointConditionParser
import com.sourceplusplus.sourcemarker.service.breakpoint.BreakpointHitWindowService
import com.sourceplusplus.sourcemarker.service.breakpoint.BreakpointTriggerListener
import com.sourceplusplus.sourcemarker.service.breakpoint.model.LiveBreakpointProperties
import com.sourceplusplus.sourcemarker.status.LiveLogStatusManager
import io.vertx.core.eventbus.ReplyException
import io.vertx.core.eventbus.ReplyFailure
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.bridge.BridgeEventType
import io.vertx.ext.eventbus.bridge.tcp.impl.protocol.FrameHelper
import io.vertx.kotlin.coroutines.CoroutineVerticle
import org.slf4j.LoggerFactory
import javax.swing.Icon

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("UNCHECKED_CAST")
class LiveInstrumentManager(private val project: Project) : CoroutineVerticle(), XBreakpointListener<XBreakpoint<*>> {

    companion object {
        private val log = LoggerFactory.getLogger(LiveInstrumentManager::class.java)
    }

    override suspend fun start() {
        log.debug("LiveInstrumentManager started")
        EditorFactory.getInstance().eventMulticaster.addEditorMouseListener(BreakpointTriggerListener, project)

        vertx.eventBus().consumer<JsonObject>("local." + Provide.LIVE_INSTRUMENT_SUBSCRIBER) {
            val liveEvent = Json.decodeValue(it.body().toString(), LiveInstrumentEvent::class.java)
            log.debug("Received instrument event. Type: {}", liveEvent.eventType)

            when (liveEvent.eventType) {
                LiveInstrumentEventType.LOG_HIT -> handleLogHitEvent(liveEvent)
                LiveInstrumentEventType.BREAKPOINT_HIT -> handleBreakpointHitEvent(liveEvent)
                LiveInstrumentEventType.BREAKPOINT_REMOVED -> handleBreakpointRemovedEvent(liveEvent)
                LiveInstrumentEventType.LOG_ADDED -> handleLogAddedEvent(liveEvent)
                LiveInstrumentEventType.LOG_REMOVED -> handleLogRemovedEvent(liveEvent)
                else -> log.warn("Un-implemented event type: {}", liveEvent.eventType)
            }
        }

        //register listener
        FrameHelper.sendFrame(
            BridgeEventType.REGISTER.name.toLowerCase(),
            Provide.LIVE_INSTRUMENT_SUBSCRIBER,
            JsonObject(),
            TCPServiceDiscoveryBackend.socket!!
        )

        //todo: should run this logic on plugin shutdown as well
        //add/remove active/inactive breakpoints
        ApplicationManager.getApplication().runReadAction {
            val manager = XDebuggerManager.getInstance(project).breakpointManager
            val liveBreakpoints = manager.allBreakpoints.filter { it.type.id == "live-breakpoint" }
            val bpIds = liveBreakpoints.map {
                (it.properties as LiveBreakpointProperties?)?.getBreakpointId()
            }.filterNotNull()

            if (bpIds.isEmpty()) {
                removeInvalidBreakpoints(manager, liveBreakpoints, emptySet())
            } else {
                Instance.liveInstrument!!.getLiveInstrumentsByIds(bpIds) {
                    if (it.succeeded()) {
                        removeInvalidBreakpoints(manager, liveBreakpoints, it.result().map { it.id!! }.toSet())
                    } else {
                        log.error("Failed to get live instruments by ids", it.cause())
                    }
                }
            }
        }

        //show live log status bars
        Instance.liveInstrument!!.getLiveLogs {
            if (it.succeeded()) {
                log.info("Found {} active live logs", it.result().size)
                LiveLogStatusManager.addActiveLiveLogs(it.result())
            } else {
                log.error("Failed to get live logs", it.cause())
            }
        }
    }

    private fun handleLogRemovedEvent(liveEvent: LiveInstrumentEvent) {
        val logRemoved = Json.decodeValue(liveEvent.data, LiveLogRemoved::class.java)
        LiveLogStatusManager.removeActiveLiveLog(logRemoved.logId)

        if (logRemoved.cause != null) {
            log.error("Log remove error: {}", logRemoved.cause!!.message)

            Notifications.Bus.notify(
                Notification(
                    message("plugin_name"), "Live log failed",
                    "Log failed: " + logRemoved.cause!!.message,
                    NotificationType.ERROR
                )
            )
        }

        val removedMark = SourceMarkSearch.findByLogId(logRemoved.logId)
        if (removedMark != null) {
            ApplicationManager.getApplication().invokeLater {
                removedMark.dispose()
            }
        }
    }

    private fun handleLogAddedEvent(liveEvent: LiveInstrumentEvent) {
        if (!SourceMarker.enabled) {
            log.debug("SourceMarker disabled. Ignored log added")
            return
        }

        val logAdded = Json.decodeValue(liveEvent.data, LiveLog::class.java)
        ApplicationManager.getApplication().invokeLater {
            val fileMarker = SourceMarker.getSourceFileMarker(logAdded.location.source)
            if (fileMarker != null) {
                //add live log only if not already known
                if (SourceMarkSearch.findByLogId(logAdded.id!!) == null) {
                    LiveLogStatusManager.showStatusBar(logAdded, fileMarker)
                }
            } else {
                LiveLogStatusManager.addActiveLiveLog(logAdded)
            }
        }
    }

    private fun handleBreakpointRemovedEvent(liveEvent: LiveInstrumentEvent) {
        val bpRemoved = Json.decodeValue(liveEvent.data, LiveBreakpointRemoved::class.java)
        ApplicationManager.getApplication().invokeLater {
            val project = ProjectManager.getInstance().openProjects[0]
            BreakpointHitWindowService.getInstance(project).processRemoveBreakpoint(bpRemoved)
        }
    }

    private fun handleBreakpointHitEvent(liveEvent: LiveInstrumentEvent) {
        val bpHit = Json.decodeValue(liveEvent.data, LiveBreakpointHit::class.java)
        ApplicationManager.getApplication().invokeLater {
            val project = ProjectManager.getInstance().openProjects[0]
            BreakpointHitWindowService.getInstance(project).addBreakpointHit(bpHit)
        }
    }

    private fun handleLogHitEvent(liveEvent: LiveInstrumentEvent) {
        if (!SourceMarker.enabled) {
            log.debug("SourceMarker disabled. Ignored log hit")
            return
        }

        //todo: can get log hit without log added (race) try open
        val logHit = Json.decodeValue(liveEvent.data, LiveLogHit::class.java)
        val hitMark = SourceMarkSearch.findByLogId(logHit.logId)
        if (hitMark != null) {
            SourceMarkSearch.findInheritedSourceMarks(hitMark).forEach {
                val portal = it.getUserData(SourceMarkKeys.SOURCE_PORTAL)!!
                vertx.eventBus().send(
                    ArtifactLogUpdated,
                    logHit.logResult.copy(artifactQualifiedName = portal.viewingPortalArtifact)
                )
            }
        } else {
            log.debug("Could not find source mark. Ignored log hit.")
        }
    }

    private fun removeInvalidBreakpoints(
        manager: XBreakpointManager, liveBreakpoints: List<XBreakpoint<*>>, validInstrumentIds: Set<String>
    ) {
        ApplicationManager.getApplication().invokeLater {
            ApplicationManager.getApplication().runWriteAction {
                liveBreakpoints.forEach {
                    val bp = it as XLineBreakpointImpl<LiveBreakpointProperties>
                    val bpProps = it.properties
                    if (bpProps?.getBreakpointId() == null) {
                        bp.dispose()
                        manager.removeBreakpoint(bp)
                    } else if (bpProps.getBreakpointId() !in validInstrumentIds) {
                        breakpointRemoved(bp)
                    }
                }
            }
        }
    }

    override fun breakpointAdded(wildBreakpoint: XBreakpoint<*>) {
        if (wildBreakpoint.type.id != "live-breakpoint") return
        val breakpoint = wildBreakpoint as XLineBreakpoint<LiveBreakpointProperties>
        if (!breakpoint.properties.getSuspend()) {
            log.debug("Ignoring un-suspended breakpoint")
            val setIconMethod = XBreakpointBase::class.java.getDeclaredMethod("setIcon", Icon::class.java)
            setIconMethod.isAccessible = true
            setIconMethod.invoke(breakpoint, SourceMarkerIcons.LIVE_BREAKPOINT_PENDING_ICON)
            return //don't publish breakpoint till it suspends
        }
        val virtualFile = VirtualFileManager.getInstance().findFileByUrl(breakpoint.fileUrl)!!

        if (breakpoint.conditionExpression != null) {
            val context = XDebuggerUtil.getInstance().findContextElement(
                virtualFile, breakpoint.sourcePosition!!.offset, project, false
            )
            val text = TextWithImportsImpl.fromXExpression(breakpoint.conditionExpression)
            val codeFragment = DebuggerUtilsEx.findAppropriateCodeFragmentFactory(text, context)
                .createCodeFragment(text, context, project)
            val breakpointCondition = BreakpointConditionParser.toBreakpointConditional(codeFragment)
            breakpoint.properties.setBreakpointCondition(breakpointCondition)
        }

        Instance.liveInstrument!!.addLiveInstrument(
            LiveBreakpoint(
                breakpoint.properties.getLocation()!!,
                condition = breakpoint.properties.getBreakpointCondition()
            )
        ) {
            if (it.succeeded()) {
                breakpoint.properties.setFinished(false)
                breakpoint.properties.setActive(true)
                breakpoint.properties.setBreakpointId(it.result().id!!)

                XDebuggerManager.getInstance(project).breakpointManager.updateBreakpointPresentation(
                    breakpoint, SourceMarkerIcons.LIVE_BREAKPOINT_ACTIVE_ICON, null
                )
            } else {
                log.error("Failed to add live breakpoint", it.cause())
                notifyError(it.cause() as ReplyException)

                breakpoint.properties.setFinished(false)
                breakpoint.properties.setActive(false)
                XDebuggerManager.getInstance(project).breakpointManager.updateBreakpointPresentation(
                    breakpoint, SourceMarkerIcons.LIVE_BREAKPOINT_ERROR_ICON, null
                )
            }
        }
    }

    override fun breakpointRemoved(wildBreakpoint: XBreakpoint<*>) {
        if (wildBreakpoint.type.id != "live-breakpoint") return
        val breakpoint = wildBreakpoint as XLineBreakpoint<LiveBreakpointProperties>
        if (breakpoint.properties == null) {
            log.warn("Ignored removing breakpoint without properties")
            return
        } else if (breakpoint.properties.getBreakpointId() == null) {
            log.debug("Ignored removing un-published breakpoint")
            return
        }

        Instance.liveInstrument!!.removeLiveInstrument(breakpoint.properties.getBreakpointId()!!) {
            if (it.succeeded()) {
                breakpoint.properties.setFinished(false)
                breakpoint.properties.setActive(false)

                if (breakpoint.properties.getSuspend()) {
                    val project = ProjectManager.getInstance().openProjects[0]
                    XDebuggerManager.getInstance(project).breakpointManager.updateBreakpointPresentation(
                        breakpoint, SourceMarkerIcons.LIVE_BREAKPOINT_ERROR_ICON, null
                    )
                } else {
                    val setIconMethod = XBreakpointBase::class.java.getDeclaredMethod("setIcon", Icon::class.java)
                    setIconMethod.isAccessible = true
                    setIconMethod.invoke(breakpoint, SourceMarkerIcons.LIVE_BREAKPOINT_PENDING_ICON)
                }
            } else {
                log.error("Failed to add live breakpoint", it.cause())
                notifyError(it.cause() as ReplyException)

                breakpoint.properties.setFinished(false)
                breakpoint.properties.setActive(false)
                val project = ProjectManager.getInstance().openProjects[0]
                XDebuggerManager.getInstance(project).breakpointManager.updateBreakpointPresentation(
                    breakpoint, SourceMarkerIcons.LIVE_BREAKPOINT_ERROR_ICON, null
                )
            }
        }
    }

    override fun breakpointChanged(wildBreakpoint: XBreakpoint<*>) {
        if (wildBreakpoint.type.id != "live-breakpoint") return
        val breakpoint = wildBreakpoint as XLineBreakpoint<LiveBreakpointProperties>
        if (breakpoint.properties == null) {
            log.warn("Ignored changing breakpoint without properties")
            return
        } else if (!breakpoint.properties.getSuspend() && breakpoint.properties.getBreakpointId() == null
            && breakpoint.suspendPolicy == SuspendPolicy.NONE
        ) {
            log.debug("Ignored changing un-published breakpoint")
            val setIconMethod = XBreakpointBase::class.java.getDeclaredMethod("setIcon", Icon::class.java)
            setIconMethod.isAccessible = true
            setIconMethod.invoke(breakpoint, SourceMarkerIcons.LIVE_BREAKPOINT_PENDING_ICON)
            return
        } else if (breakpoint.properties.getBreakpointId() == null) {
            log.debug("Breakpoint changed from un-suspended to suspended")
            breakpoint.properties.setSuspend(true)
            breakpointAdded(breakpoint) //redirect changed event to added event
            return
        } else if (breakpoint.suspendPolicy == SuspendPolicy.NONE) {
            breakpoint.properties.setSuspend(false)
            breakpointRemoved(breakpoint)
            return
        }
        val virtualFile = VirtualFileManager.getInstance().findFileByUrl(breakpoint.fileUrl)!!

        if (breakpoint.conditionExpression != null) {
            val context = XDebuggerUtil.getInstance().findContextElement(
                virtualFile, breakpoint.sourcePosition!!.offset, project, false
            )
            val text = TextWithImportsImpl.fromXExpression(breakpoint.conditionExpression)
            val codeFragment = DebuggerUtilsEx.findAppropriateCodeFragmentFactory(text, context)
                .createCodeFragment(text, context, project)
            val breakpointCondition = BreakpointConditionParser.toBreakpointConditional(codeFragment)
            breakpoint.properties.setBreakpointCondition(breakpointCondition)
        }

        Instance.liveInstrument!!.removeLiveInstrument(breakpoint.properties.getBreakpointId()!!) {
            if (it.succeeded()) {
                ApplicationManager.getApplication().runReadAction {
                    val psiFile: PsiClassOwner = (PsiManager.getInstance(ProjectManager.getInstance().openProjects[0])
                        .findFile(virtualFile) as PsiClassOwner?)!!
                    val qualifiedName = psiFile.classes[0].qualifiedName!!

                    //only need to copy over location
                    breakpoint.properties.setLocation(LiveSourceLocation(qualifiedName, breakpoint.line + 1))

                    Instance.liveInstrument!!.addLiveInstrument(
                        LiveBreakpoint(
                            breakpoint.properties.getLocation()!!,
                            condition = breakpoint.properties.getBreakpointCondition()
                        )
                    ) {
                        if (it.succeeded()) {
                            breakpoint.properties.setFinished(false)
                            breakpoint.properties.setActive(true)
                            breakpoint.properties.setBreakpointId(it.result().id!!)

                            XDebuggerManager.getInstance(project).breakpointManager.updateBreakpointPresentation(
                                breakpoint, SourceMarkerIcons.LIVE_BREAKPOINT_ACTIVE_ICON, null
                            )
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
            log.error("Timed out removing live breakpoint")
            Notifications.Bus.notify(
                Notification(
                    message("plugin_name"), "Live breakpoint failed",
                    "Timed out removing live breakpoint",
                    NotificationType.ERROR
                )
            )
        } else {
            val actualException = replyException.cause!!
            if (actualException is AccessDenied) {
                log.error("Access denied. Reason: " + actualException.reason)
                Notifications.Bus.notify(
                    Notification(
                        message("plugin_name"), "Live breakpoint failed",
                        "Access denied. Reason: " + actualException.reason,
                        NotificationType.ERROR
                    )
                )
            } else {
                replyException.printStackTrace()
                log.error("Failed to add live breakpoint", replyException)
                Notifications.Bus.notify(
                    Notification(
                        message("plugin_name"), "Live breakpoint failed",
                        "Failed to add/remove live breakpoint",
                        NotificationType.ERROR
                    )
                )
            }
        }
    }
}
