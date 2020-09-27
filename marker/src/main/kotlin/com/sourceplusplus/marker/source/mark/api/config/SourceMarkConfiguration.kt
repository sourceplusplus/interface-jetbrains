package com.sourceplusplus.marker.source.mark.api.config

import com.sourceplusplus.marker.source.mark.api.component.api.SourceMarkComponentProvider
import com.sourceplusplus.marker.source.mark.api.filter.ApplySourceMarkFilter

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface SourceMarkConfiguration {
    var applySourceMarkFilter: ApplySourceMarkFilter
    var activateOnKeyboardShortcut: Boolean
    var componentProvider: SourceMarkComponentProvider
}
