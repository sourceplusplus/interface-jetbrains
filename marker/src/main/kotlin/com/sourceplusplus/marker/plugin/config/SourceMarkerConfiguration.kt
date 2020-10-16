package com.sourceplusplus.marker.plugin.config

import com.sourceplusplus.marker.source.SourceFileMarker
import com.sourceplusplus.marker.source.SourceFileMarkerProvider
import com.sourceplusplus.marker.source.mark.api.filter.CreateSourceMarkFilter
import com.sourceplusplus.marker.source.mark.gutter.config.GutterMarkConfiguration
import com.sourceplusplus.marker.source.mark.inlay.config.InlayMarkConfiguration

/**
 * Used to configure [SourceFileMarker]s.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SourceMarkerConfiguration {
    var createSourceMarkFilter: CreateSourceMarkFilter = CreateSourceMarkFilter.ALL
    var sourceFileMarkerProvider: SourceFileMarkerProvider = object : SourceFileMarkerProvider {}
    var defaultGutterMarkConfiguration: GutterMarkConfiguration = GutterMarkConfiguration()
    var defaultInlayMarkConfiguration: InlayMarkConfiguration = InlayMarkConfiguration()
}
