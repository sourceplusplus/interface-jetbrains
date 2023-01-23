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
import com.intellij.psi.PsiDocumentManager
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.impl.jose.JWT
import io.vertx.ext.bridge.BridgeEventType
import io.vertx.ext.eventbus.bridge.tcp.impl.protocol.FrameHelper
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import spp.jetbrains.UserData
import spp.jetbrains.artifact.service.ArtifactTypeService
import spp.jetbrains.icons.PluginIcons
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.SourceMarkerKeys
import spp.jetbrains.marker.SourceMarkerKeys.INSTRUMENT_ID
import spp.jetbrains.marker.service.ArtifactCreationService.createExpressionGutterMark
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEvent
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventCode
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventListener
import spp.jetbrains.marker.source.mark.guide.MethodGuideMark
import spp.jetbrains.marker.source.mark.gutter.GutterMark
import spp.jetbrains.sourcemarker.config.SourceMarkerConfig
import spp.jetbrains.sourcemarker.discover.TCPServiceDiscoveryBackend
import spp.protocol.instrument.LiveBreakpoint
import spp.protocol.instrument.LiveInstrument
import spp.protocol.instrument.LiveLog
import spp.protocol.instrument.event.*
import spp.protocol.service.SourceServices.Subscribe.toLiveInstrumentSubscriberAddress
import spp.protocol.service.listen.LiveInstrumentListener
import spp.protocol.service.listen.addLiveInstrumentListener
import java.util.concurrent.CopyOnWriteArrayList

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LiveInstrumentEventListener(
    private val project: Project,
    private val pluginConfig: SourceMarkerConfig
) : CoroutineVerticle(), LiveInstrumentListener, SourceMarkEventListener {

    private val log = logger<LiveInstrumentEventListener>()
    private val activeInstruments = CopyOnWriteArrayList<LiveInstrument>()

    override suspend fun start() {
        var developer = "system"
        if (pluginConfig.serviceToken != null) {
            val json = JWT.parse(pluginConfig.serviceToken)
            developer = json.getJsonObject("payload").getString("developer_id")
        }
        vertx.addLiveInstrumentListener(developer, this).await()

        //register instrument event listener
        FrameHelper.sendFrame(
            BridgeEventType.REGISTER.name.lowercase(),
            toLiveInstrumentSubscriberAddress(developer), null,
            JsonObject().apply { pluginConfig.serviceToken?.let { put("auth-token", it) } },
            null, null, TCPServiceDiscoveryBackend.getSocket(project)
        )

        //fetch currently active live instruments
        UserData.liveInstrumentService(project)!!.getLiveInstruments().onSuccess {
            activeInstruments.addAllAbsent(it)
        }.onFailure {
            log.error("Failed to get active instruments", it)
        }
    }

    override fun handleEvent(event: SourceMarkEvent) {
        when (event.eventCode) {
            SourceMarkEventCode.MARK_ADDED -> {
                //wait for method guide marks as they indicate a method currently is or may become visible
                if (event.sourceMark !is MethodGuideMark) return

                //display status icon(s) for method
                ApplicationManager.getApplication().runReadAction {
                    val methodSourceMark = event.sourceMark as MethodGuideMark
                    val fileMarker = event.sourceMark.sourceFileMarker

                    val textRange = methodSourceMark.getPsiElement().textRange
                    val document = PsiDocumentManager.getInstance(methodSourceMark.project)
                        .getDocument(methodSourceMark.sourceFileMarker.psiFile) ?: return@runReadAction
                    val startLine = document.getLineNumber(textRange.startOffset) + 1
                    val endLine = document.getLineNumber(textRange.endOffset) + 1

                    val locationSource = if (ArtifactTypeService.isJvm(methodSourceMark.getPsiElement())) {
                        methodSourceMark.artifactQualifiedName.toClass()?.identifier
                    } else {
                        fileMarker.psiFile.virtualFile.name
                    }
                    if (locationSource == null) {
                        log.error("Unable to determine location source of: ${methodSourceMark.artifactQualifiedName}")
                        return@runReadAction
                    }

                    ApplicationManager.getApplication().invokeLater {
                        activeInstruments.forEach {
                            if (locationSource == it.location.source && it.location.line in startLine..endLine) {
                                when (it) {
                                    is LiveLog -> addGutterMark(fileMarker, it)
                                    is LiveBreakpoint -> addGutterMark(fileMarker, it)
                                    else -> Unit
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onInstrumentAddedEvent(event: LiveInstrumentAdded) {
        ApplicationManager.getApplication().invokeLater {
            log.debug("Instrument added: $event")
            activeInstruments.add(event.instrument)
            InstrumentEventWindowService.getInstance(project).addInstrumentEvent(event)

            val fileMarker = SourceMarker.getInstance(project).getSourceFileMarker(event.instrument.location.source)
            if (fileMarker != null) {
                addGutterMark(fileMarker, event.instrument)

                InstrumentEventWindowService.getInstance(project)
                    .selectInOverviewTab(event.instrument.id!!)
            } else {
                log.debug("No file marker found for ${event.instrument.location.source}")
            }
        }
    }

    private fun addGutterMark(fileMarker: SourceFileMarker, instrument: LiveInstrument) {
        if (fileMarker.getGutterMarks().any { it.getUserData(INSTRUMENT_ID) == instrument.id }) {
            return
        }

        val gutterMark = createExpressionGutterMark(fileMarker, instrument.location.line)
        gutterMark.putUserData(INSTRUMENT_ID, instrument.id)
        if (instrument is LiveBreakpoint) {
            if (instrument.meta["created_by"] != UserData.selfInfo(project)?.developer?.id) {
                gutterMark.configuration.icon = PluginIcons.Breakpoint.foreign
                gutterMark.configuration.navigationHandler = InstrumentNavigationHandler(gutterMark, false)
            } else {
                gutterMark.configuration.icon = PluginIcons.Breakpoint.active
                gutterMark.configuration.navigationHandler = InstrumentNavigationHandler(gutterMark, true)
            }
        } else if (instrument is LiveLog) {
            if (instrument.meta["created_by"] != UserData.selfInfo(project)?.developer?.id) {
                gutterMark.configuration.icon = PluginIcons.Log.foreign
                gutterMark.configuration.navigationHandler = InstrumentNavigationHandler(gutterMark, false)
            } else {
                gutterMark.configuration.icon = PluginIcons.Log.active
                gutterMark.configuration.navigationHandler = InstrumentNavigationHandler(gutterMark, true)
            }
        }
        gutterMark.applyIfMissing()
    }

    override fun onInstrumentRemovedEvent(event: LiveInstrumentRemoved) {
        ApplicationManager.getApplication().invokeLater {
            log.debug("Instrument removed: $event")
            activeInstruments.remove(event.instrument)
            InstrumentEventWindowService.getInstance(project).addInstrumentEvent(event)

            val inlayMark = SourceMarker.getInstance(project).findByInstrumentId(event.instrument.id!!)
            if (inlayMark != null) {
                if (inlayMark is GutterMark) {
                    if (event.instrument.meta["created_by"] != UserData.selfInfo(project)?.developer?.id) {
                        //just remove foreign instrument icons
                        inlayMark.dispose()
                    } else if (event.instrument !is LiveBreakpoint) {
                        //just remove non-breakpoint icons
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
