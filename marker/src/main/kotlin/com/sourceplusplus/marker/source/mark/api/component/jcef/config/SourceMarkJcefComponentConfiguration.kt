package com.sourceplusplus.marker.source.mark.api.component.jcef.config

import com.intellij.ui.jcef.JBCefBrowser
import com.sourceplusplus.marker.source.mark.api.component.api.config.SourceMarkComponentConfiguration
import com.sourceplusplus.marker.source.mark.api.component.jcef.SourceMarkJcefComponent
import java.awt.Dimension

/**
 * Used to configure [SourceMarkJcefComponent].
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("MagicNumber")
class SourceMarkJcefComponentConfiguration : SourceMarkComponentConfiguration() {

    var preloadJcefBrowser: Boolean = true
    var initialUrl: String = "about:blank"
    var initialHtml: String? = null
    var componentWidth: Int = 400
    var componentHeight: Int = 300
    var autoDisposeBrowser: Boolean = true
    var browserLoadingListener: BrowserLoadingListener = BrowserLoadingListener()

    fun setComponentSize(size: Dimension) {
        componentWidth = size.width
        componentHeight = size.height
    }

    override fun copy(): SourceMarkJcefComponentConfiguration {
        val copy = SourceMarkJcefComponentConfiguration()
        copy.useHeavyPopup = useHeavyPopup
        copy.hideOnMouseMotion = hideOnMouseMotion
        copy.hideOnScroll = hideOnScroll
        copy.showAboveClass = showAboveClass
        copy.showAboveMethod = showAboveMethod
        copy.showAboveExpression = showAboveExpression
        copy.componentSizeEvaluator = componentSizeEvaluator

        copy.preloadJcefBrowser = preloadJcefBrowser
        copy.initialUrl = initialUrl
        copy.initialHtml = initialHtml
        copy.componentWidth = componentWidth
        copy.componentHeight = componentHeight
        copy.autoDisposeBrowser = autoDisposeBrowser
        copy.browserLoadingListener = browserLoadingListener
        return copy
    }
}

/**
 * Used to listen for JCEF browser creation events.
 */
open class BrowserLoadingListener {
    open fun beforeBrowserCreated(configuration: SourceMarkJcefComponentConfiguration) = Unit
    open fun afterBrowserCreated(browser: JBCefBrowser) = Unit
}
