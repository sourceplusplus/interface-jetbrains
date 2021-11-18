package spp.jetbrains.marker.source.mark.api.component.jcef

import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.api.component.api.SourceMarkComponentProvider
import spp.jetbrains.marker.source.mark.api.component.jcef.config.SourceMarkJcefComponentConfiguration
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEvent
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventCode
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventListener
import spp.jetbrains.marker.source.mark.gutter.event.GutterMarkEventCode
import spp.jetbrains.marker.source.mark.inlay.InlayMark

/**
 * todo: description.
 *
 * @since 0.1.0
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
