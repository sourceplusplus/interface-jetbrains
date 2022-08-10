/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package spp.jetbrains.sourcemarker.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.impl.jose.JWT
import io.vertx.ext.bridge.BridgeEventType
import io.vertx.ext.eventbus.bridge.tcp.impl.protocol.FrameHelper
import io.vertx.kotlin.coroutines.CoroutineVerticle
import org.slf4j.LoggerFactory
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.sourcemarker.mark.SourceMarkKeys
import spp.jetbrains.sourcemarker.mark.SourceMarkSearch
import spp.jetbrains.sourcemarker.service.discover.TCPServiceDiscoveryBackend
import spp.jetbrains.sourcemarker.service.instrument.breakpoint.BreakpointHitWindowService
import spp.jetbrains.sourcemarker.settings.SourceMarkerConfig
import spp.jetbrains.sourcemarker.status.LiveStatusManager
import spp.protocol.SourceServices.Instance
import spp.protocol.SourceServices.Provide.toLiveInstrumentSubscriberAddress
import spp.protocol.instrument.LiveBreakpoint
import spp.protocol.instrument.LiveLog
import spp.protocol.instrument.event.LiveInstrumentEvent
import spp.protocol.instrument.event.LiveInstrumentEventType
import spp.protocol.marshall.ProtocolMarshaller
import spp.protocol.marshall.ProtocolMarshaller.deserializeLiveInstrumentRemoved

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

        vertx.eventBus().consumer<JsonObject>(toLiveInstrumentSubscriberAddress(developer)) {
            val liveEvent = Json.decodeValue(it.body().toString(), LiveInstrumentEvent::class.java)
            log.debug("Received instrument event. Type: {}", liveEvent.eventType)

            when (liveEvent.eventType) {
                LiveInstrumentEventType.LOG_HIT -> handleLogHitEvent(liveEvent)
                LiveInstrumentEventType.BREAKPOINT_HIT -> handleBreakpointHitEvent(liveEvent)
                LiveInstrumentEventType.BREAKPOINT_ADDED -> handleBreakpointAddedEvent(liveEvent)
                LiveInstrumentEventType.BREAKPOINT_REMOVED -> handleInstrumentRemovedEvent(liveEvent)
                LiveInstrumentEventType.LOG_ADDED -> handleLogAddedEvent(liveEvent)
                LiveInstrumentEventType.LOG_REMOVED -> handleInstrumentRemovedEvent(liveEvent)
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

    private fun handleLogAddedEvent(liveEvent: LiveInstrumentEvent) {
        if (!SourceMarker.getInstance(project).enabled) {
            log.debug("SourceMarker disabled. Ignored log added")
            return
        }

        val logAdded = Json.decodeValue(liveEvent.data, LiveLog::class.java)
        ApplicationManager.getApplication().invokeLater {
            val fileMarker = SourceMarker.getInstance(project).getSourceFileMarker(logAdded.location.source)
            if (fileMarker != null) {
                val smId = logAdded.meta["original_source_mark"] as String? ?: return@invokeLater
                val inlayMark = SourceMarker.getInstance(project).getSourceMark(smId) ?: return@invokeLater
                inlayMark.putUserData(SourceMarkKeys.INSTRUMENT_ID, logAdded.id)
                inlayMark.getUserData(SourceMarkKeys.STATUS_BAR)!!.setLiveInstrument(logAdded)
            } else {
                LiveStatusManager.addActiveLiveInstrument(logAdded)
            }
        }
    }

    private fun handleBreakpointAddedEvent(liveEvent: LiveInstrumentEvent) {
        val bpAdded = Json.decodeValue(liveEvent.data, LiveBreakpoint::class.java)
        ApplicationManager.getApplication().invokeLater {
            val fileMarker = SourceMarker.getInstance(project).getSourceFileMarker(bpAdded.location.source)
            if (fileMarker != null) {
                val smId = bpAdded.meta["original_source_mark"] as String? ?: return@invokeLater
                val inlayMark = SourceMarker.getInstance(project).getSourceMark(smId) ?: return@invokeLater
                inlayMark.putUserData(SourceMarkKeys.INSTRUMENT_ID, bpAdded.id)
                inlayMark.getUserData(SourceMarkKeys.STATUS_BAR)!!.setLiveInstrument(bpAdded)
            }
        }
    }

    private fun handleInstrumentRemovedEvent(liveEvent: LiveInstrumentEvent) {
        val instrumentRemoved = deserializeLiveInstrumentRemoved(JsonObject(liveEvent.data))
        ApplicationManager.getApplication().invokeLater {
            val inlayMark = SourceMarkSearch.findByInstrumentId(project, instrumentRemoved.liveInstrument.id!!)
            if (inlayMark != null) {
                val eventListeners = inlayMark.getUserData(SourceMarkKeys.INSTRUMENT_EVENT_LISTENERS)
                if (eventListeners?.isNotEmpty() == true) {
                    eventListeners.forEach { it.accept(liveEvent) }
                }
            }
        }
    }

    private fun handleBreakpointHitEvent(liveEvent: LiveInstrumentEvent) {
        if (!SourceMarker.getInstance(project).enabled) {
            log.debug("SourceMarker disabled. Ignored breakpoint hit")
            return
        }

        val bpHit = ProtocolMarshaller.deserializeLiveBreakpointHit(JsonObject(liveEvent.data))
        ApplicationManager.getApplication().invokeLater {
            BreakpointHitWindowService.getInstance(project).addBreakpointHit(bpHit)

            SourceMarkSearch.findByInstrumentId(project, bpHit.breakpointId)
                ?.getUserData(SourceMarkKeys.INSTRUMENT_EVENT_LISTENERS)?.forEach { it.accept(liveEvent) }
        }
    }

    private fun handleLogHitEvent(liveEvent: LiveInstrumentEvent) {
        if (!SourceMarker.getInstance(project).enabled) {
            log.debug("SourceMarker disabled. Ignored log hit")
            return
        }

        val logHit = ProtocolMarshaller.deserializeLiveLogHit(JsonObject(liveEvent.data))
        SourceMarkSearch.findByInstrumentId(project, logHit.logId)
            ?.getUserData(SourceMarkKeys.INSTRUMENT_EVENT_LISTENERS)?.forEach { it.accept(liveEvent) }
    }
}
