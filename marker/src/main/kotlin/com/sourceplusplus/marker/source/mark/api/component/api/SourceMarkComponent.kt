package com.sourceplusplus.marker.source.mark.api.component.api

import com.sourceplusplus.marker.source.mark.api.component.api.config.SourceMarkComponentConfiguration
import javax.swing.JComponent

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface SourceMarkComponent {

    val configuration: SourceMarkComponentConfiguration

    fun getComponent(): JComponent
    fun dispose()
}
