package com.sourceplusplus.marker.plugin.config

import com.sourceplusplus.marker.source.SourceFileMarkerProvider
import com.sourceplusplus.marker.source.mark.api.filter.CreateSourceMarkFilter
import com.sourceplusplus.marker.source.mark.gutter.config.GutterMarkConfiguration
import com.sourceplusplus.marker.source.mark.inlay.config.InlayMarkConfiguration

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SourceMarkerConfiguration {
    var createSourceMarkFilter: CreateSourceMarkFilter = CreateSourceMarkFilter.ALL
    var sourceFileMarkerProvider: SourceFileMarkerProvider = object : SourceFileMarkerProvider {}
    var defaultGutterMarkConfiguration: GutterMarkConfiguration = GutterMarkConfiguration() //todo: maybe incorrect location
    var defaultInlayMarkConfiguration: InlayMarkConfiguration = InlayMarkConfiguration() //todo: maybe incorrect location
}
