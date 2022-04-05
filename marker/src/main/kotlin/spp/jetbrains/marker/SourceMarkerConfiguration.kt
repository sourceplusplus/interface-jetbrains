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
package spp.jetbrains.marker

import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.SourceFileMarkerProvider
import spp.jetbrains.marker.source.mark.api.filter.CreateSourceMarkFilter
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
    var createSourceMarkFilter: CreateSourceMarkFilter = CreateSourceMarkFilter.ALL
    var sourceFileMarkerProvider: SourceFileMarkerProvider = object : SourceFileMarkerProvider {}
    var gutterMarkConfiguration: GutterMarkConfiguration = GutterMarkConfiguration()
    var inlayMarkConfiguration: InlayMarkConfiguration = InlayMarkConfiguration()
    var guideMarkConfiguration: GuideMarkConfiguration = GuideMarkConfiguration()
}
