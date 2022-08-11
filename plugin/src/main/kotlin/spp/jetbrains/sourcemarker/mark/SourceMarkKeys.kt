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
package spp.jetbrains.sourcemarker.mark

import spp.jetbrains.marker.jvm.psi.LoggerDetector
import spp.jetbrains.marker.source.info.EndpointDetector
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.api.key.SourceKey
import spp.jetbrains.sourcemarker.status.StatusBar
import spp.protocol.portal.PortalConfiguration
import spp.protocol.service.listen.LiveInstrumentEventListener
import spp.protocol.service.listen.LiveViewEventListener

/**
 * Used to associate custom data to [SourceMark]s.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object SourceMarkKeys {
    val PORTAL_CONFIGURATION = SourceKey<PortalConfiguration>("PORTAL_CONFIGURATION")
    val ENDPOINT_DETECTOR = SourceKey<EndpointDetector<*>>("ENDPOINT_DETECTOR")
    val LOGGER_DETECTOR = SourceKey<LoggerDetector>("LOGGER_DETECTOR")
    val INSTRUMENT_ID = SourceKey<String>("INSTRUMENT_ID")
    val VIEW_SUBSCRIPTION_ID = SourceKey<String>("VIEW_SUBSCRIPTION_ID")
    val GROUPED_MARKS = SourceKey<MutableList<SourceMark>>("GROUPED_MARKS")
    val INSTRUMENT_EVENT_LISTENERS = SourceKey<MutableSet<LiveInstrumentEventListener>>("INSTRUMENT_EVENT_LISTENERS")
    val VIEW_EVENT_LISTENERS = SourceKey<MutableSet<LiveViewEventListener>>("LIVE_VIEW_EVENT_LISTENERS")
    val STATUS_BAR = SourceKey<StatusBar>("STATUS_BAR")
}
