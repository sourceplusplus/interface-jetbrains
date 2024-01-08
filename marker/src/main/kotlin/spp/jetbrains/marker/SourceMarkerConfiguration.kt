/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2024 CodeBrig, Inc.
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
package spp.jetbrains.marker

import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.SourceFileMarkerProvider
import spp.jetbrains.marker.source.mark.guide.config.GuideMarkConfiguration
import spp.jetbrains.marker.source.mark.gutter.config.GutterMarkConfiguration
import spp.jetbrains.marker.source.mark.inlay.config.InlayMarkConfiguration

/**
 * Used to configure [SourceFileMarker]s.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SourceMarkerConfiguration {
    var sourceFileMarkerProvider: SourceFileMarkerProvider = object : SourceFileMarkerProvider {}
    var gutterMarkConfiguration: GutterMarkConfiguration = GutterMarkConfiguration()
    var inlayMarkConfiguration: InlayMarkConfiguration = InlayMarkConfiguration()
    var guideMarkConfiguration: GuideMarkConfiguration = GuideMarkConfiguration()
}
