package com.sourceplusplus.marker.source.mark.api.component.jcef

import com.sourceplusplus.marker.source.mark.api.SourceMark
import com.sourceplusplus.marker.source.mark.api.component.api.SourceMarkComponentProvider
import com.sourceplusplus.marker.source.mark.api.component.jcef.config.SourceMarkJcefComponentConfiguration
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEvent
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventCode
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventListener
import com.sourceplusplus.marker.source.mark.gutter.event.GutterMarkEventCode
import com.sourceplusplus.marker.source.mark.inlay.InlayMark

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
open class SourceMarkJcefComponentProvider : SourceMarkComponentProvider, SourceMarkEventListener {

    override val defaultConfiguration = SourceMarkJcefComponentConfiguration()

    override fun getComponent(sourceMark: SourceMark): SourceMarkJcefComponent {
        sourceMark.addEventListener(this)
        return SourceMarkJcefComponent(defaultConfiguration.copy())
    }

    override fun disposeComponent(sourceMark: SourceMark) {
        sourceMark.sourceMarkComponent.dispose()
    }

    private fun initializeComponent(sourceMark: SourceMark) {
        val jcefComponent = sourceMark.sourceMarkComponent as SourceMarkJcefComponent
        if (jcefComponent.configuration.preloadJcefBrowser) {
            jcefComponent.initialize()
        }
    }

    override fun handleEvent(event: SourceMarkEvent) {
        when (event.eventCode) {
            SourceMarkEventCode.MARK_REMOVED -> {
                disposeComponent(event.sourceMark)
            }
            GutterMarkEventCode.GUTTER_MARK_VISIBLE -> {
                initializeComponent(event.sourceMark)
            }
            SourceMarkEventCode.MARK_ADDED -> {
                if (event.sourceMark.configuration.activateOnKeyboardShortcut || event.sourceMark is InlayMark) {
                    initializeComponent(event.sourceMark)
                }
            }
        }
    }
}
