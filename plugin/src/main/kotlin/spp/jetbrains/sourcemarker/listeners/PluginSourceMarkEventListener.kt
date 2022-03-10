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
import spp.jetbrains.marker.source.mark.api.MethodSourceMark
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEvent
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventCode
import spp.jetbrains.marker.source.mark.api.event.SynchronousSourceMarkEventListener
import spp.jetbrains.sourcemarker.SourceMarkerPlugin.vertx
import spp.jetbrains.sourcemarker.mark.SourceMarkKeys.ENDPOINT_DETECTOR
import spp.jetbrains.sourcemarker.mark.SourceMarkKeys.LOGGER_DETECTOR

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

        if (event.eventCode == SourceMarkEventCode.MARK_BEFORE_ADDED) {
            val sourceMark = event.sourceMark

            if (sourceMark is MethodSourceMark) {
                //setup endpoint detector and attempt detection
                sourceMark.putUserData(ENDPOINT_DETECTOR, endpointDetector)
                GlobalScope.launch(vertx.dispatcher()) {
                    endpointDetector.getOrFindEndpointId(sourceMark)
                }
            }

            //setup logger detector
            sourceMark.putUserData(LOGGER_DETECTOR, loggerDetector)
        }
    }
}
