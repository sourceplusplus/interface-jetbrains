package com.sourceplusplus.marker.source.mark.api.config

import com.sourceplusplus.marker.source.mark.api.SourceMark
import com.sourceplusplus.marker.source.mark.api.component.api.SourceMarkComponentProvider
import com.sourceplusplus.marker.source.mark.api.filter.ApplySourceMarkFilter

/**
 * Used to configure [SourceMark]s.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface SourceMarkConfiguration {
    var applySourceMarkFilter: ApplySourceMarkFilter
    var activateOnKeyboardShortcut: Boolean
    var componentProvider: SourceMarkComponentProvider
}
