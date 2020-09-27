package com.sourceplusplus.marker.source.mark.api.component.api

import com.sourceplusplus.marker.source.mark.api.SourceMark
import com.sourceplusplus.marker.source.mark.api.component.api.config.SourceMarkComponentConfiguration

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface SourceMarkComponentProvider {

    val defaultConfiguration: SourceMarkComponentConfiguration

    fun getComponent(sourceMark: SourceMark): SourceMarkComponent
    fun disposeComponent(sourceMark: SourceMark)
}
