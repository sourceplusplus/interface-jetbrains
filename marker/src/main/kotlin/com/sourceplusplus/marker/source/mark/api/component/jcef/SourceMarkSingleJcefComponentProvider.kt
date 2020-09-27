package com.sourceplusplus.marker.source.mark.api.component.jcef

import com.sourceplusplus.marker.source.mark.api.SourceMark
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEvent
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventCode
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventListener
import com.sourceplusplus.marker.source.mark.gutter.event.GutterMarkEventCode

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SourceMarkSingleJcefComponentProvider : SourceMarkJcefComponentProvider(), SourceMarkEventListener {

    private val jcefComponent: SourceMarkJcefComponent by lazy {
        SourceMarkJcefComponent(defaultConfiguration.copy())
    }

    override fun getComponent(sourceMark: SourceMark): SourceMarkJcefComponent {
        sourceMark.addEventListener(this)
        return jcefComponent
    }

    override fun disposeComponent(sourceMark: SourceMark) {
        //do nothing
    }

    override fun handleEvent(event: SourceMarkEvent) {
        when (event.eventCode) {
            GutterMarkEventCode.GUTTER_MARK_VISIBLE -> super.handleEvent(event)
            SourceMarkEventCode.MARK_ADDED -> super.handleEvent(event)
        }
    }
}
