/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2023 CodeBrig, Inc.
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
package spp.jetbrains.sourcemarker

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.impl.jose.JWT
import io.vertx.ext.bridge.BridgeEventType
import io.vertx.ext.eventbus.bridge.tcp.impl.protocol.FrameHelper
import io.vertx.kotlin.coroutines.CoroutineVerticle
import spp.jetbrains.UserData
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.SourceMarkerKeys
import spp.jetbrains.plugin.LiveStatusManager
import spp.jetbrains.sourcemarker.config.SourceMarkerConfig
import spp.jetbrains.sourcemarker.mark.SourceMarkSearch
import spp.jetbrains.sourcemarker.discover.TCPServiceDiscoveryBackend
import spp.jetbrains.sourcemarker.instrument.breakpoint.BreakpointHitWindowService
import spp.protocol.instrument.LiveBreakpoint
import spp.protocol.instrument.LiveLog
import spp.protocol.instrument.event.LiveBreakpointHit
import spp.protocol.instrument.event.LiveInstrumentRemoved
import spp.protocol.instrument.event.LiveLogHit
import spp.protocol.service.SourceServices.Subscribe.toLiveInstrumentSubscriberAddress
import spp.protocol.service.listen.LiveInstrumentListener
import spp.protocol.service.listen.addLiveInstrumentListener

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LiveInstrumentManager(
    private val project: Project,
    private val pluginConfig: SourceMarkerConfig
) : CoroutineVerticle(), LiveInstrumentListener {

    companion object {
        private val log = logger<LiveInstrumentManager>()
    }

    override suspend fun start() {
        var developer = "system"
        if (pluginConfig.serviceToken != null) {
            val json = JWT.parse(pluginConfig.serviceToken)
            developer = json.getJsonObject("payload").getString("developer_id")
        }

        vertx.addLiveInstrumentListener(developer, this)

        //register listener
        FrameHelper.sendFrame(
            BridgeEventType.REGISTER.name.lowercase(),
            toLiveInstrumentSubscriberAddress(developer), null,
            JsonObject().apply { pluginConfig.serviceToken?.let { put("auth-token", it) } },
            null, null, TCPServiceDiscoveryBackend.getSocket(project)
        )

        //show live status bars
        UserData.liveInstrumentService(project)!!.getLiveInstruments(null).onComplete {
            if (it.succeeded()) {
                log.info("Found ${it.result().size} active live status bars")
                LiveStatusManager.getInstance(project).addActiveLiveInstruments(it.result())
            } else {
                log.warn("Failed to get live status bars", it.cause())
            }
        }
    }

    override fun onLogAddedEvent(event: LiveLog) {
        ApplicationManager.getApplication().invokeLater {
            val fileMarker = SourceMarker.getInstance(project).getSourceFileMarker(event.location.source)
            if (fileMarker != null) {
                val smId = event.meta["original_source_mark"] as String? ?: return@invokeLater
                val inlayMark = SourceMarker.getInstance(project).getSourceMark(smId) ?: return@invokeLater
                inlayMark.putUserData(SourceMarkerKeys.INSTRUMENT_ID, event.id)
                inlayMark.getUserData(SourceMarkerKeys.STATE_BAR)!!.setLiveInstrument(event)
            } else {
                LiveStatusManager.getInstance(project).addActiveLiveInstrument(event)
            }
        }
    }

    override fun onBreakpointAddedEvent(event: LiveBreakpoint) {
        ApplicationManager.getApplication().invokeLater {
            log.debug("Breakpoint added: $event")
            val fileMarker = SourceMarker.getInstance(project).getSourceFileMarker(event.location.source)
            if (fileMarker != null) {
                val smId = event.meta["original_source_mark"] as String? ?: return@invokeLater
                val inlayMark = SourceMarker.getInstance(project).getSourceMark(smId) ?: return@invokeLater
                inlayMark.putUserData(SourceMarkerKeys.INSTRUMENT_ID, event.id)
                inlayMark.getUserData(SourceMarkerKeys.STATE_BAR)!!.setLiveInstrument(event)
            } else {
                log.debug("No file marker found for ${event.location.source}")
            }
        }
    }

    override fun onInstrumentRemovedEvent(event: LiveInstrumentRemoved) {
        ApplicationManager.getApplication().invokeLater {
            val inlayMark = SourceMarkSearch.findByInstrumentId(project, event.liveInstrument.id!!)
            if (inlayMark != null) {
                val eventListeners = inlayMark.getUserData(SourceMarkerKeys.INSTRUMENT_EVENT_LISTENERS)
                if (eventListeners?.isNotEmpty() == true) {
                    eventListeners.forEach { it.onInstrumentRemovedEvent(event) }
                }
            }
        }
    }

    override fun onBreakpointHitEvent(event: LiveBreakpointHit) {
        ApplicationManager.getApplication().invokeLater {
            BreakpointHitWindowService.getInstance(project).addBreakpointHit(event)

            SourceMarkSearch.findByInstrumentId(project, event.breakpointId)
                ?.getUserData(SourceMarkerKeys.INSTRUMENT_EVENT_LISTENERS)?.forEach { it.onBreakpointHitEvent(event) }
        }
    }

    override fun onLogHitEvent(event: LiveLogHit) {
        SourceMarkSearch.findByInstrumentId(project, event.logId)
            ?.getUserData(SourceMarkerKeys.INSTRUMENT_EVENT_LISTENERS)?.forEach { it.onLogHitEvent(event) }
    }
}
