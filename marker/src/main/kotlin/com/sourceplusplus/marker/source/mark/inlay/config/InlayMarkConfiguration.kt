package com.sourceplusplus.marker.source.mark.inlay.config

import com.sourceplusplus.marker.source.mark.api.component.api.SourceMarkComponentProvider
import com.sourceplusplus.marker.source.mark.api.component.jcef.SourceMarkJcefComponentProvider
import com.sourceplusplus.marker.source.mark.api.config.SourceMarkConfiguration
import com.sourceplusplus.marker.source.mark.api.filter.ApplySourceMarkFilter

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
data class InlayMarkConfiguration(
    override var applySourceMarkFilter: ApplySourceMarkFilter = ApplySourceMarkFilter.NONE,
    var virtualText: InlayMarkVirtualText? = null,
    var activateOnMouseClick: Boolean = true,
    override var activateOnKeyboardShortcut: Boolean = false, //todo: remove
    override var componentProvider: SourceMarkComponentProvider = SourceMarkJcefComponentProvider()
) : SourceMarkConfiguration
