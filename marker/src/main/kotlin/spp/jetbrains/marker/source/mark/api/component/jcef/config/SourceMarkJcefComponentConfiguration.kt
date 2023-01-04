/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2023 CodeBrig, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    var preloadJcefBrowser: Boolean = false
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
        copy.zoomLevel = zoomLevel
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
