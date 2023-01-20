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
package spp.jetbrains.sourcemarker.instrument

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.impl.jose.JWT
import io.vertx.ext.bridge.BridgeEventType
import io.vertx.ext.eventbus.bridge.tcp.impl.protocol.FrameHelper
import io.vertx.kotlin.coroutines.CoroutineVerticle
import spp.jetbrains.UserData
import spp.jetbrains.icons.PluginIcons
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.SourceMarkerKeys
import spp.jetbrains.marker.SourceMarkerKeys.INSTRUMENT_ID
import spp.jetbrains.marker.service.ArtifactCreationService.createExpressionGutterMark
import spp.jetbrains.marker.source.mark.gutter.GutterMark
import spp.jetbrains.plugin.LiveStatusBarManager
import spp.jetbrains.sourcemarker.config.SourceMarkerConfig
import spp.jetbrains.sourcemarker.discover.TCPServiceDiscoveryBackend
import spp.protocol.instrument.LiveBreakpoint
import spp.protocol.instrument.LiveLog
import spp.protocol.instrument.event.*
import spp.protocol.service.SourceServices.Subscribe.toLiveInstrumentSubscriberAddress
import spp.protocol.service.listen.LiveInstrumentListener
import spp.protocol.service.listen.addLiveInstrumentListener

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LiveInstrumentEventListener(
    private val project: Project,
    private val pluginConfig: SourceMarkerConfig
) : CoroutineVerticle(), LiveInstrumentListener {

    companion object {
        private val log = logger<LiveInstrumentEventListener>()
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
        UserData.liveInstrumentService(project)!!.getLiveInstruments().onComplete {
            if (it.succeeded()) {
                log.info("Found ${it.result().size} active live status bars")
                LiveStatusBarManager.getInstance(project).addActiveLiveInstruments(it.result())
            } else {
                log.warn("Failed to get live status bars", it.cause())
            }
        }
    }

    override fun onInstrumentAddedEvent(event: LiveInstrumentAdded) {
        ApplicationManager.getApplication().invokeLater {
            log.debug("Instrument added: $event")
            InstrumentEventWindowService.getInstance(project).addInstrumentEvent(event)

            val fileMarker = SourceMarker.getInstance(project).getSourceFileMarker(event.instrument.location.source)
            if (fileMarker != null) {
                if (SourceMarker.getInstance(project).findByInstrumentId(event.instrument.id!!) != null) {
                    return@invokeLater
                }

                val gutterMark = createExpressionGutterMark(fileMarker, event.instrument.location.line)
                gutterMark.putUserData(INSTRUMENT_ID, event.instrument.id)
                if (event.instrument is LiveBreakpoint) {
                    if (event.instrument.meta["created_by"] != UserData.selfInfo(project)?.developer?.id) {
                        gutterMark.configuration.icon = PluginIcons.Breakpoint.foreign
                        gutterMark.configuration.navigationHandler = InstrumentNavigationHandler(gutterMark, false)
                    } else {
                        gutterMark.configuration.icon = PluginIcons.Breakpoint.active
                        gutterMark.configuration.navigationHandler = InstrumentNavigationHandler(gutterMark, true)

                        InstrumentEventWindowService.getInstance(gutterMark.project)
                            .selectInOverviewTab(event.instrument.id!!)
                    }
                } else if (event.instrument is LiveLog) {
                    gutterMark.configuration.icon = PluginIcons.Log.foreign
                    gutterMark.configuration.navigationHandler = InstrumentNavigationHandler(gutterMark, false)
                }
                gutterMark.applyIfMissing()
            } else {
                log.debug("No file marker found for ${event.instrument.location.source}")
            }
        }
    }

    override fun onInstrumentRemovedEvent(event: LiveInstrumentRemoved) {
        ApplicationManager.getApplication().invokeLater {
            log.debug("Instrument removed: $event")
            InstrumentEventWindowService.getInstance(project).addInstrumentEvent(event)

            val inlayMark = SourceMarker.getInstance(project).findByInstrumentId(event.instrument.id!!)
            if (inlayMark != null) {
                if (inlayMark is GutterMark) {
                    if (event.instrument.meta["created_by"] != UserData.selfInfo(project)?.developer?.id) {
                        //just remove foreign instrument icons
                        inlayMark.dispose()
                    } else {
                        if (event.cause == null) {
                            inlayMark.configuration.icon = PluginIcons.Breakpoint.complete
                        } else {
                            inlayMark.configuration.icon = PluginIcons.Breakpoint.error
                        }
                        inlayMark.sourceFileMarker.refresh()
                    }
                }

                val eventListeners = inlayMark.getUserData(SourceMarkerKeys.INSTRUMENT_EVENT_LISTENERS)
                if (eventListeners?.isNotEmpty() == true) {
                    eventListeners.forEach { it.onInstrumentRemovedEvent(event) }
                }
            }
        }
    }

    override fun onInstrumentHitEvent(event: LiveInstrumentHit) {
        ApplicationManager.getApplication().invokeLater {
            InstrumentEventWindowService.getInstance(project).addInstrumentEvent(event)

            if (event is LiveBreakpointHit) {
                SourceMarker.getInstance(project).findByInstrumentId(event.instrument.id!!)
                    ?.getUserData(SourceMarkerKeys.INSTRUMENT_EVENT_LISTENERS)
                    ?.forEach { it.onBreakpointHitEvent(event) }
            } else if (event is LiveLogHit) {
                SourceMarker.getInstance(project).findByInstrumentId(event.instrument.id!!)
                    ?.getUserData(SourceMarkerKeys.INSTRUMENT_EVENT_LISTENERS)?.forEach { it.onLogHitEvent(event) }
            }
        }
    }

    override fun onInstrumentAppliedEvent(event: LiveInstrumentApplied) {
        ApplicationManager.getApplication().invokeLater {
            InstrumentEventWindowService.getInstance(project).addInstrumentEvent(event)
        }
    }
}
