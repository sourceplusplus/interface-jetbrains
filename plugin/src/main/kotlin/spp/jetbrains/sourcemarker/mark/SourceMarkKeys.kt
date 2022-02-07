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
package spp.jetbrains.sourcemarker.mark

import spp.jetbrains.marker.jvm.psi.EndpointDetector
import spp.jetbrains.marker.jvm.psi.LoggerDetector
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.api.key.SourceKey
import spp.jetbrains.portal.SourcePortal
import spp.jetbrains.sourcemarker.service.InstrumentEventListener
import spp.jetbrains.sourcemarker.status.StatusBar

/**
 * Used to associate custom data to [SourceMark]s.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object SourceMarkKeys {
    val SOURCE_PORTAL = SourceKey<SourcePortal>("SOURCE_PORTAL")
    val ENDPOINT_DETECTOR = SourceKey<EndpointDetector>("ENDPOINT_DETECTOR")
    val LOGGER_DETECTOR = SourceKey<LoggerDetector>("LOGGER_DETECTOR")
    val BREAKPOINT_ID = SourceKey<String>("BREAKPOINT_ID")
    val LOG_ID = SourceKey<String>("LOG_ID")
    val METER_ID = SourceKey<String>("METER_ID")
    val GROUPED_MARKS = SourceKey<MutableList<SourceMark>>("GROUPED_MARKS")
    val INSTRUMENT_EVENT_LISTENERS = SourceKey<MutableSet<InstrumentEventListener>>("INSTRUMENT_EVENT_LISTENERS")
    val STATUS_BAR = SourceKey<StatusBar>("STATUS_BAR")
}
