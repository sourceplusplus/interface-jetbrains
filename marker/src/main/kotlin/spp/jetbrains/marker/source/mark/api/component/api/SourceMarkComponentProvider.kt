package spp.jetbrains.marker.source.mark.api.component.api

import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.api.component.api.config.SourceMarkComponentConfiguration

/**
 * Used to configure, display, and dispose [SourceMark] components.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface SourceMarkComponentProvider {

    val defaultConfiguration: SourceMarkComponentConfiguration

    fun getComponent(sourceMark: SourceMark): SourceMarkComponent
    fun disposeComponent(sourceMark: SourceMark)
}
