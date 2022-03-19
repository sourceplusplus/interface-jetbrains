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
package spp.jetbrains.sourcemarker.portal

import com.fasterxml.jackson.databind.module.SimpleModule
import com.intellij.ide.ui.laf.IntelliJLaf
import com.intellij.openapi.application.ApplicationManager
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import kotlinx.datetime.Instant
import org.slf4j.LoggerFactory
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.source.mark.api.component.jcef.SourceMarkJcefComponent
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventCode
import spp.jetbrains.marker.source.mark.gutter.GutterMark
import spp.jetbrains.portal.SourcePortal
import spp.jetbrains.portal.backend.PortalServer
import spp.jetbrains.portal.protocol.portal.PageType
import spp.jetbrains.sourcemarker.command.LiveControlCommand
import spp.jetbrains.sourcemarker.command.LiveControlCommand.*
import spp.jetbrains.sourcemarker.mark.SourceMarkKeys
import spp.jetbrains.sourcemarker.settings.SourceMarkerConfig
import spp.protocol.artifact.ArtifactQualifiedName
import spp.protocol.marshall.KSerializers
import spp.protocol.marshall.LocalMessageCodec
import javax.swing.UIManager

class PortalController(private val markerConfig: SourceMarkerConfig) : CoroutineVerticle() {

    private val log = LoggerFactory.getLogger(PortalController::class.java)

    override suspend fun start() {
        log.info("Initializing portal")

        val module = SimpleModule()
        module.addSerializer(Instant::class.java, KSerializers.KotlinInstantSerializer())
        module.addDeserializer(Instant::class.java, KSerializers.KotlinInstantDeserializer())
        DatabindCodec.mapper().registerModule(module)

        val portalServer = PortalServer(0)
        vertx.deployVerticle(portalServer).await()
        vertx.deployVerticle(PortalEventListener(markerConfig)).await()

//        //todo: remove after v0.4.3
//        vertx.eventBus().registerDefaultCodec(ArtifactQualifiedName::class.java, LocalMessageCodec())

        SourceMarker.addGlobalSourceMarkEventListener {
            if (it.eventCode == SourceMarkEventCode.MARK_BEFORE_ADDED && it.sourceMark is GutterMark) {
                //register portal for source mark
                val portal = SourcePortal.getPortal(
                    SourcePortal.register(it.sourceMark.artifactQualifiedName, false)
                )!!
                it.sourceMark.putUserData(SourceMarkKeys.PORTAL_CONFIGURATION, portal.configuration)
                portal.configuration.config["visibleOverview"] = it.sourceMark.isClassMark
                portal.configuration.config["visibleActivity"] = true
                portal.configuration.config["visibleTraces"] = true
                portal.configuration.config["visibleLogs"] = true
                portal.configuration.config["visibleConfiguration"] = false

                val genUrl = "http://localhost:${portalServer.serverPort}?portalUuid=${portal.portalUuid}"
                it.sourceMark.addEventListener {
                    if (it.eventCode == SourceMarkEventCode.UPDATE_PORTAL_CONFIG) {
                        when (val command = it.params.first() as LiveControlCommand) {
                            VIEW_OVERVIEW -> portal.configuration.config["currentPage"] = PageType.OVERVIEW
                            VIEW_ACTIVITY -> portal.configuration.config["currentPage"] = PageType.ACTIVITY
                            VIEW_TRACES -> portal.configuration.config["currentPage"] = PageType.TRACES
                            VIEW_LOGS -> portal.configuration.config["currentPage"] = PageType.LOGS
                            else -> throw UnsupportedOperationException("Command input: $command")
                        }

                        val jcefComponent = it.sourceMark.sourceMarkComponent as SourceMarkJcefComponent
                        jcefComponent.configuration.currentUrl = "#"
                    } else if (it.eventCode == SourceMarkEventCode.PORTAL_OPENING) {
                        val jcefComponent = it.sourceMark.sourceMarkComponent as SourceMarkJcefComponent
                        portal.configuration.darkMode = UIManager.getLookAndFeel() !is IntelliJLaf

                        if (jcefComponent.configuration.currentUrl != genUrl) {
                            jcefComponent.configuration.initialUrl = genUrl
                            jcefComponent.configuration.currentUrl = genUrl
                            jcefComponent.getBrowser().cefBrowser.executeJavaScript(
                                "window.location.href = '$genUrl';", genUrl, 0
                            )
                        }
                        ApplicationManager.getApplication().invokeLater(it.sourceMark::displayPopup)
                    }
                }
            }
        }
    }
}
