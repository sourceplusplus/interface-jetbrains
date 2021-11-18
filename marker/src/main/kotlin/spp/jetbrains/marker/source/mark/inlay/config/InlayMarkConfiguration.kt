package spp.jetbrains.marker.source.mark.inlay.config

import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.util.Ref
import spp.jetbrains.marker.source.mark.api.component.api.SourceMarkComponentProvider
import spp.jetbrains.marker.source.mark.api.component.jcef.SourceMarkJcefComponentProvider
import spp.jetbrains.marker.source.mark.api.config.SourceMarkConfiguration
import spp.jetbrains.marker.source.mark.api.filter.ApplySourceMarkFilter
import spp.jetbrains.marker.source.mark.inlay.InlayMark

/**
 * Used to configure [InlayMark]s.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
data class InlayMarkConfiguration(
    override var applySourceMarkFilter: ApplySourceMarkFilter = ApplySourceMarkFilter.NONE,
    var strictlyManualCreation: Boolean = false,
    var virtualText: InlayMarkVirtualText? = null,
    var showComponentInlay: Boolean = false,
    var inlayRef: Ref<Inlay<*>>? = null,
    var activateOnMouseClick: Boolean = true,
    override var activateOnKeyboardShortcut: Boolean = false, //todo: remove
    override var componentProvider: SourceMarkComponentProvider = SourceMarkJcefComponentProvider()
) : SourceMarkConfiguration
