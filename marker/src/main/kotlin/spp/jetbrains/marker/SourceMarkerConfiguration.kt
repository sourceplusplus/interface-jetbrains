package spp.jetbrains.marker

import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.SourceFileMarkerProvider
import spp.jetbrains.marker.source.mark.api.filter.CreateSourceMarkFilter
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
}
