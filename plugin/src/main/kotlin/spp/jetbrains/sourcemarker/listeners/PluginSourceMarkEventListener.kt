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
package spp.jetbrains.sourcemarker.listeners

import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import spp.jetbrains.marker.jvm.psi.EndpointDetector
import spp.jetbrains.marker.jvm.psi.LoggerDetector
import spp.jetbrains.marker.source.mark.api.ClassSourceMark
import spp.jetbrains.marker.source.mark.api.MethodSourceMark
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEvent
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventCode
import spp.jetbrains.marker.source.mark.api.event.SynchronousSourceMarkEventListener
import spp.jetbrains.portal.SourcePortal
import spp.jetbrains.sourcemarker.SourceMarkerPlugin.vertx
import spp.jetbrains.sourcemarker.mark.SourceMarkKeys
import spp.jetbrains.sourcemarker.mark.SourceMarkKeys.ENDPOINT_DETECTOR
import spp.jetbrains.sourcemarker.mark.SourceMarkKeys.LOGGER_DETECTOR
import spp.protocol.ProtocolAddress.Global.OpenPortal
import spp.protocol.artifact.ArtifactType
import spp.protocol.portal.PageType

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class PluginSourceMarkEventListener : SynchronousSourceMarkEventListener {

    companion object {
        private val log = LoggerFactory.getLogger(PluginSourceMarkEventListener::class.java)
        private val endpointDetector = EndpointDetector(vertx)
        private val loggerDetector = LoggerDetector(vertx)
    }

    override fun handleEvent(event: SourceMarkEvent) {
        if (log.isTraceEnabled) {
            log.trace("Handling event: $event")
        }

        if (event.eventCode == SourceMarkEventCode.PORTAL_OPENED) {
            val sourceMark = event.sourceMark
            val sourcePortal = sourceMark.getUserData(SourceMarkKeys.SOURCE_PORTAL)!!
            sourcePortal.visible = true
        } else if (event.eventCode == SourceMarkEventCode.PORTAL_CLOSED) {
            val sourceMark = event.sourceMark
            val sourcePortal = sourceMark.getUserData(SourceMarkKeys.SOURCE_PORTAL)!!
            sourcePortal.visible = false
        } else if (event.eventCode == SourceMarkEventCode.MARK_BEFORE_ADDED) {
            val sourceMark = event.sourceMark

            //register portal for source mark
            val sourcePortal = SourcePortal.getPortal(SourcePortal.register(
                sourceMark.artifactQualifiedName, false
            ))
            sourceMark.putUserData(SourceMarkKeys.SOURCE_PORTAL, sourcePortal!!)
            sourceMark.addEventListener {
                if (it.eventCode == SourceMarkEventCode.PORTAL_OPENING) {
                    vertx.eventBus().publish(OpenPortal, sourcePortal)
                }
            }

            if (sourceMark is ClassSourceMark) {
                //class-based portals only have overview page
                sourcePortal.configuration.currentPage = PageType.OVERVIEW
                sourcePortal.configuration.visibleActivity = false
                sourcePortal.configuration.visibleTraces = false
                sourcePortal.configuration.artifactType = ArtifactType.CLASS
            } else if (sourceMark is MethodSourceMark) {
                //method-based portals don't have overview page
                sourcePortal.configuration.visibleOverview = false
                sourcePortal.configuration.artifactType = ArtifactType.METHOD
            } else {
                sourcePortal.configuration.artifactType = ArtifactType.EXPRESSION
            }

            if (sourceMark is MethodSourceMark) {
                //setup endpoint detector and attempt detection
                sourceMark.putUserData(ENDPOINT_DETECTOR, endpointDetector)
                GlobalScope.launch(vertx.dispatcher()) {
                    endpointDetector.getOrFindEndpointId(sourceMark)
                }
            }

            //setup logger detector
            sourceMark.putUserData(LOGGER_DETECTOR, loggerDetector)
        } else if (event.eventCode == SourceMarkEventCode.MARK_REMOVED) {
            event.sourceMark.getUserData(SourceMarkKeys.SOURCE_PORTAL)!!.close()
        }
    }
}
