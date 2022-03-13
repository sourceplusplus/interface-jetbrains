/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package spp.jetbrains.marker.source.mark.api.component.jcef.config

import com.intellij.ui.jcef.JBCefBrowser
import spp.jetbrains.marker.source.mark.api.component.api.config.SourceMarkComponentConfiguration
import spp.jetbrains.marker.source.mark.api.component.jcef.SourceMarkJcefComponent
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
    var currentUrl: String = "about:blank"
    var initialUrl: String = "about:blank"
    var initialHtml: String? = null
    var componentWidth: Int = 400
    var componentHeight: Int = 300
    var autoDisposeBrowser: Boolean = true
    var zoomLevel = 0.8
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
        copy.currentUrl = currentUrl
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
