/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package spp.jetbrains.sourcemarker.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.impl.jose.JWT
import io.vertx.ext.bridge.BridgeEventType
import io.vertx.ext.eventbus.bridge.tcp.impl.protocol.FrameHelper
import io.vertx.kotlin.coroutines.CoroutineVerticle
import org.slf4j.LoggerFactory
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.sourcemarker.discover.TCPServiceDiscoveryBackend
import spp.jetbrains.sourcemarker.mark.SourceMarkKeys
import spp.jetbrains.sourcemarker.search.SourceMarkSearch
import spp.jetbrains.sourcemarker.service.breakpoint.BreakpointHitWindowService
import spp.jetbrains.sourcemarker.service.breakpoint.BreakpointTriggerListener
import spp.jetbrains.sourcemarker.settings.SourceMarkerConfig
import spp.jetbrains.sourcemarker.status.LiveStatusManager
import spp.protocol.ProtocolAddress.Global.ArtifactLogUpdated
import spp.protocol.ProtocolMarshaller.deserializeLiveInstrumentRemoved
import spp.protocol.SourceServices.Instance
import spp.protocol.SourceServices.Provide.toLiveInstrumentSubscriberAddress
import spp.protocol.instrument.LiveBreakpoint
import spp.protocol.instrument.LiveLog
import spp.protocol.instrument.event.LiveBreakpointHit
import spp.protocol.instrument.event.LiveInstrumentEvent
import spp.protocol.instrument.event.LiveInstrumentEventType
import spp.protocol.instrument.event.LiveLogHit

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("UNCHECKED_CAST")
class LiveInstrumentManager(
    private val project: Project,
    private val pluginConfig: SourceMarkerConfig
) : CoroutineVerticle() {

    companion object {
        private val log = LoggerFactory.getLogger(LiveInstrumentManager::class.java)
    }

    override suspend fun start() {
        var developer = "system"
        if (pluginConfig.serviceToken != null) {
            val json = JWT.parse(pluginConfig.serviceToken)
            developer = json.getJsonObject("payload").getString("developer_id")
        }
        EditorFactory.getInstance().eventMulticaster.addEditorMouseListener(BreakpointTriggerListener, project)

        vertx.eventBus().consumer<JsonObject>(toLiveInstrumentSubscriberAddress(developer)) {
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
            toLiveInstrumentSubscriberAddress(developer), null,
            JsonObject().apply { pluginConfig.serviceToken?.let { put("auth-token", it) } },
            null, null, TCPServiceDiscoveryBackend.socket!!
        )

        //show live status bars
        Instance.liveInstrument!!.getLiveInstruments(null).onComplete {
            if (it.succeeded()) {
                log.info("Found {} active live status bars", it.result().size)
                LiveStatusManager.addActiveLiveInstruments(it.result())
            } else {
                log.error("Failed to get live status bars", it.cause())
            }
        }
    }

    private fun handleLogRemovedEvent(liveEvent: LiveInstrumentEvent) {
        val logRemoved = deserializeLiveInstrumentRemoved(JsonObject(liveEvent.data))
        ApplicationManager.getApplication().invokeLater {
            val inlayMark = SourceMarkSearch.findByLogId(logRemoved.liveInstrument.id!!)
            if (inlayMark != null) {
                val eventListeners = inlayMark.getUserData(SourceMarkKeys.INSTRUMENT_EVENT_LISTENERS)
                if (eventListeners?.isNotEmpty() == true) {
                    eventListeners.forEach { it.accept(liveEvent) }
                }
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
                val smId = logAdded.meta["original_source_mark"] as String? ?: return@invokeLater
                val inlayMark = SourceMarker.getSourceMark(smId) ?: return@invokeLater
                inlayMark.putUserData(SourceMarkKeys.LOG_ID, logAdded.id)
                inlayMark.getUserData(SourceMarkKeys.STATUS_BAR)!!.setLiveInstrument(logAdded)
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
                val smId = bpAdded.meta["original_source_mark"] as String? ?: return@invokeLater
                val inlayMark = SourceMarker.getSourceMark(smId) ?: return@invokeLater
                inlayMark.putUserData(SourceMarkKeys.BREAKPOINT_ID, bpAdded.id)
                inlayMark.getUserData(SourceMarkKeys.STATUS_BAR)!!.setLiveInstrument(bpAdded)
            }
        }
    }

    private fun handleBreakpointRemovedEvent(liveEvent: LiveInstrumentEvent) {
        val bpRemoved = deserializeLiveInstrumentRemoved(JsonObject(liveEvent.data))
        ApplicationManager.getApplication().invokeLater {
            val inlayMark = SourceMarkSearch.findByBreakpointId(bpRemoved.liveInstrument.id!!)
            if (inlayMark != null) {
                val eventListeners = inlayMark.getUserData(SourceMarkKeys.INSTRUMENT_EVENT_LISTENERS)
                if (eventListeners?.isNotEmpty() == true) {
                    eventListeners.forEach { it.accept(liveEvent) }
                }
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
                }
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
                }

                SourceMarkSearch.findInheritedSourceMarks(inlayMark).forEach {
                    val portal = it.getUserData(SourceMarkKeys.SOURCE_PORTAL)!!
                    vertx.eventBus().send(
                        ArtifactLogUpdated,
                        logHit.logResult.copy(artifactQualifiedName = portal.viewingArtifact)
                    )
                }
            }
        }
    }
}
