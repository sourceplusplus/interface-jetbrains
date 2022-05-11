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
import io.vertx.core.json.JsonObject
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import kotlinx.datetime.Instant
import org.slf4j.LoggerFactory
import spp.booster.PageType
import spp.booster.PortalServer
import spp.booster.SourcePortal
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventCode
import spp.jetbrains.marker.source.mark.guide.GuideMark
import spp.jetbrains.sourcemarker.command.LiveControlCommand
import spp.jetbrains.sourcemarker.command.LiveControlCommand.Companion.VIEW_OVERVIEW
import spp.jetbrains.sourcemarker.mark.SourceMarkKeys
import spp.jetbrains.sourcemarker.settings.SourceMarkerConfig
import spp.protocol.marshall.KSerializers
import javax.swing.UIManager

class PortalController(private val markerConfig: SourceMarkerConfig) : CoroutineVerticle() {

    private val log = LoggerFactory.getLogger(PortalController::class.java)

    override suspend fun start() {
        log.info("Initializing portal")

        val module = SimpleModule()
        module.addSerializer(Instant::class.java, KSerializers.KotlinInstantSerializer())
        module.addDeserializer(Instant::class.java, KSerializers.KotlinInstantDeserializer())
        DatabindCodec.mapper().registerModule(module)

        log.info("Initializing portal server")
        val portalServer = PortalServer(8081)
        vertx.deployVerticle(portalServer).await()
        vertx.deployVerticle(PortalEventListener(markerConfig)).await()
        log.info("Portal server initialized")

//        val componentProvider = SourceMarkSingleJcefComponentProvider().apply {
////            defaultConfiguration.initialUrl = "http://localhost:8080/general?portal=true&fullview=true"
//            defaultConfiguration.initialUrl = "https://google.com"
//            defaultConfiguration.zoomLevel = markerConfig.portalConfig.zoomLevel
//            defaultConfiguration.componentSizeEvaluator = object : ComponentSizeEvaluator() {
//                override fun getDynamicSize(
//                    editor: Editor,
//                    configuration: SourceMarkComponentConfiguration
//                ): Dimension {
//                    val widthDouble = 963 * markerConfig.portalConfig.zoomLevel
//                    val heightDouble = 350 * markerConfig.portalConfig.zoomLevel
//                    var width: Int = widthDouble.toInt()
//                    if (ceil(widthDouble) != floor(widthDouble)) {
//                        width = ceil(widthDouble).toInt() + 1
//                    }
//                    var height = heightDouble.toInt()
//                    if (ceil(heightDouble) != floor(heightDouble)) {
//                        height = ceil(heightDouble).toInt() + 1
//                    }
//                    return Dimension(width, height)
//                }
//            }
//        }
//        SourceMarker.configuration.guideMarkConfiguration.componentProvider = componentProvider
//        SourceMarker.configuration.inlayMarkConfiguration.componentProvider = componentProvider
//        log.info("Booting JCEF browser")
//        componentProvider.jcefComponent.initialize()
//        log.info("JCEF browser booted")

        SourceMarker.addGlobalSourceMarkEventListener {
            if (it.eventCode == SourceMarkEventCode.MARK_BEFORE_ADDED && it.sourceMark is GuideMark) {
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

                it.sourceMark.addEventListener {
                    if (it.eventCode == SourceMarkEventCode.UPDATE_PORTAL_CONFIG) {
                        if (it.params.first() is String && it.params.first() == "setPage") {
                            vertx.eventBus().publish(
                                "portal.SetCurrentPage",
                                JsonObject().put("page", it.params.get(1) as String)
                            )
                        } else {
                            val newPage = when (val command = it.params.first() as LiveControlCommand) {
                                VIEW_OVERVIEW -> PageType.OVERVIEW
                                else -> {
                                    log.error("Unknown command: $command")
                                    return@addEventListener
                                }
                            }

                            if (newPage != portal.configuration.config["currentPage"]) {
                                log.info("Setting portal page to $newPage")
                                portal.configuration.config["currentPage"] = newPage

                                val pageType =
                                    (portal.configuration.config["currentPage"] as PageType).name.toLowerCase()
                                        .capitalize()
                                val endpointId = it.sourceMark.getUserData(SourceMarkKeys.ENDPOINT_DETECTOR)!!
                                    .getEndpointId(it.sourceMark)!!
                                vertx.eventBus().publish(
                                    "portal.SetCurrentPage",
                                    JsonObject().put(
                                        "page",
                                        "/dashboard/GENERAL/Endpoint/${endpointId.substringBefore("_")}/$endpointId/Endpoint-$pageType?portal=true&fullview=true"
                                    )
                                )
                            }
                        }
                    } else if (it.eventCode == SourceMarkEventCode.PORTAL_OPENING) {
//                        SourcePortal.getPortals().filter { it.portalUuid != portal.portalUuid }.forEach {
//                            it.configuration.config["active"] = false
//                        }
//                        portal.configuration.config["active"] = true

                        portal.configuration.darkMode = UIManager.getLookAndFeel() !is IntelliJLaf
                        portal.configuration.config["portal_uuid"] = portal.portalUuid
//                        vertx.eventBus().publish(RenderPage, JsonObject.mapFrom(portal.configuration))
                        ApplicationManager.getApplication().invokeLater(it.sourceMark::displayPopup)
                    }
                }
            }
        }

        log.info("Portal initialized")
    }
}
