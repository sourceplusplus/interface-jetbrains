package spp.jetbrains.marker.source.mark.gutter.config

import spp.jetbrains.marker.source.mark.api.component.api.SourceMarkComponentProvider
import spp.jetbrains.marker.source.mark.api.component.jcef.SourceMarkJcefComponentProvider
import spp.jetbrains.marker.source.mark.api.config.SourceMarkConfiguration
import spp.jetbrains.marker.source.mark.api.filter.ApplySourceMarkFilter
import spp.jetbrains.marker.source.mark.gutter.GutterMark
import javax.swing.Icon

/**
 * Used to configure [GutterMark]s.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
data class GutterMarkConfiguration(
    override var applySourceMarkFilter: ApplySourceMarkFilter = ApplySourceMarkFilter.ALL,
    var icon: Icon? = null,
    var activateOnMouseHover: Boolean = true,
    var activateOnMouseClick: Boolean = false,
    override var activateOnKeyboardShortcut: Boolean = false,
    override var componentProvider: SourceMarkComponentProvider = SourceMarkJcefComponentProvider()
) : SourceMarkConfiguration
