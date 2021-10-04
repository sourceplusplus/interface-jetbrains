package com.sourceplusplus.sourcemarker.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.sourceplusplus.marker.SourceMarker
import com.sourceplusplus.protocol.ProtocolAddress.Global.ArtifactLogUpdated
import com.sourceplusplus.protocol.SourceMarkerServices.Instance
import com.sourceplusplus.protocol.SourceMarkerServices.Provide
import com.sourceplusplus.protocol.instrument.LiveInstrumentEvent
import com.sourceplusplus.protocol.instrument.LiveInstrumentEventType
import com.sourceplusplus.protocol.instrument.breakpoint.LiveBreakpoint
import com.sourceplusplus.protocol.instrument.breakpoint.event.LiveBreakpointHit
import com.sourceplusplus.protocol.instrument.breakpoint.event.LiveBreakpointRemoved
import com.sourceplusplus.protocol.instrument.log.LiveLog
import com.sourceplusplus.protocol.instrument.log.event.LiveLogHit
import com.sourceplusplus.protocol.instrument.log.event.LiveLogRemoved
import com.sourceplusplus.sourcemarker.discover.TCPServiceDiscoveryBackend
import com.sourceplusplus.sourcemarker.mark.SourceMarkKeys
import com.sourceplusplus.sourcemarker.search.SourceMarkSearch
import com.sourceplusplus.sourcemarker.service.breakpoint.BreakpointHitWindowService
import com.sourceplusplus.sourcemarker.service.breakpoint.BreakpointTriggerListener
import com.sourceplusplus.sourcemarker.status.LiveStatusManager
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.bridge.BridgeEventType
import io.vertx.ext.eventbus.bridge.tcp.impl.protocol.FrameHelper
import io.vertx.kotlin.coroutines.CoroutineVerticle
import org.slf4j.LoggerFactory

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("UNCHECKED_CAST")
class LiveInstrumentManager(private val project: Project) : CoroutineVerticle() {

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
                LiveInstrumentEventType.BREAKPOINT_ADDED -> handleBreakpointAddedEvent(liveEvent)
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

        //show live log status bars
        Instance.liveInstrument!!.getLiveLogs {
            if (it.succeeded()) {
                log.info("Found {} active live logs", it.result().size)
                LiveStatusManager.addActiveLiveInstruments(it.result())
            } else {
                log.error("Failed to get live logs", it.cause())
            }
        }
    }

    private fun handleLogRemovedEvent(liveEvent: LiveInstrumentEvent) {
        val logRemoved = Json.decodeValue(liveEvent.data, LiveLogRemoved::class.java)
        ApplicationManager.getApplication().invokeLater {
            val inlayMark = SourceMarkSearch.findByLogId(logRemoved.logId)
            if (inlayMark != null) {
                val eventListeners = inlayMark.getUserData(SourceMarkKeys.INSTRUMENT_EVENT_LISTENERS)
                if (eventListeners?.isNotEmpty() == true) {
                    eventListeners.forEach { it.accept(liveEvent) }
                } else {
                    LiveStatusManager.addPendingEvent(liveEvent, logRemoved.logId)
                }
            } else {
                LiveStatusManager.addPendingEvent(liveEvent, logRemoved.logId)
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
                    LiveStatusManager.showLogStatusBar(logAdded, fileMarker)
                }
            } else {
                LiveStatusManager.addActiveLiveInstrument(logAdded)
            }
        }
    }

    private fun handleBreakpointAddedEvent(liveEvent: LiveInstrumentEvent) {
        val bpAdded = Json.decodeValue(liveEvent.data, LiveBreakpoint::class.java)
        ApplicationManager.getApplication().invokeLater {
            val fileMarker = SourceMarker.getSourceFileMarker(bpAdded.location.source)
            if (fileMarker != null) {
                //add live breakpoint only if not already known
                if (SourceMarkSearch.findByLogId(bpAdded.id!!) == null) {
                    LiveStatusManager.showBreakpointStatusBar(bpAdded, fileMarker)
                }
            } else {
                LiveStatusManager.addActiveLiveInstrument(bpAdded)
            }
        }
    }

    private fun handleBreakpointRemovedEvent(liveEvent: LiveInstrumentEvent) {
        val bpRemoved = Json.decodeValue(liveEvent.data, LiveBreakpointRemoved::class.java)
        ApplicationManager.getApplication().invokeLater {
            val project = ProjectManager.getInstance().openProjects[0]

            val inlayMark = SourceMarkSearch.findByBreakpointId(bpRemoved.breakpointId)
            if (inlayMark != null) {
                val eventListeners = inlayMark.getUserData(SourceMarkKeys.INSTRUMENT_EVENT_LISTENERS)
                if (eventListeners?.isNotEmpty() == true) {
                    eventListeners.forEach { it.accept(liveEvent) }
                } else {
                    LiveStatusManager.addPendingEvent(liveEvent, bpRemoved.breakpointId)
                }
            } else {
                LiveStatusManager.addPendingEvent(liveEvent, bpRemoved.breakpointId)
            }
        }
    }

    private fun handleBreakpointHitEvent(liveEvent: LiveInstrumentEvent) {
        val bpHit = Json.decodeValue(liveEvent.data, LiveBreakpointHit::class.java)
        ApplicationManager.getApplication().invokeLater {
            val project = ProjectManager.getInstance().openProjects[0]
            BreakpointHitWindowService.getInstance(project).addBreakpointHit(bpHit)

            val inlayMark = SourceMarkSearch.findByBreakpointId(bpHit.breakpointId)
            if (inlayMark != null) {
                val eventListeners = inlayMark.getUserData(SourceMarkKeys.INSTRUMENT_EVENT_LISTENERS)
                if (eventListeners?.isNotEmpty() == true) {
                    eventListeners.forEach { it.accept(liveEvent) }
                } else {
                    LiveStatusManager.addPendingEvent(liveEvent, bpHit.breakpointId)
                }
            } else {
                LiveStatusManager.addPendingEvent(liveEvent, bpHit.breakpointId)
            }
        }
    }

    private fun handleLogHitEvent(liveEvent: LiveInstrumentEvent) {
        if (!SourceMarker.enabled) {
            log.debug("SourceMarker disabled. Ignored log hit")
            return
        }

        //todo: can get log hit without log added (race) try open
        val logHit = Json.decodeValue(liveEvent.data, LiveLogHit::class.java)
        ApplicationManager.getApplication().invokeLater {
            val inlayMark = SourceMarkSearch.findByLogId(logHit.logId)
            if (inlayMark != null) {
                val eventListeners = inlayMark.getUserData(SourceMarkKeys.INSTRUMENT_EVENT_LISTENERS)
                if (eventListeners?.isNotEmpty() == true) {
                    eventListeners.forEach { it.accept(liveEvent) }
                } else {
                    LiveStatusManager.addPendingEvent(liveEvent, logHit.logId)
                }

                SourceMarkSearch.findInheritedSourceMarks(inlayMark).forEach {
                    val portal = it.getUserData(SourceMarkKeys.SOURCE_PORTAL)!!
                    vertx.eventBus().send(
                        ArtifactLogUpdated,
                        logHit.logResult.copy(artifactQualifiedName = portal.viewingPortalArtifact)
                    )
                }
            } else {
                LiveStatusManager.addPendingEvent(liveEvent, logHit.logId)
            }
        }
    }
}
