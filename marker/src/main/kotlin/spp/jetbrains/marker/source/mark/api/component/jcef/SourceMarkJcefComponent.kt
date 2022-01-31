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
package spp.jetbrains.marker.source.mark.api.component.jcef

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefClient
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import spp.jetbrains.marker.source.mark.api.component.api.SourceMarkComponent
import spp.jetbrains.marker.source.mark.api.component.jcef.config.SourceMarkJcefComponentConfiguration
import java.awt.Dimension
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JComponent

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SourceMarkJcefComponent(
    override val configuration: SourceMarkJcefComponentConfiguration
) : SourceMarkComponent {

    companion object {
        private val client: JBCefClient by lazy { JBCefApp.getInstance().createClient() }

        init {
            if (!ApplicationManager.getApplication().isUnitTestMode) {
                Disposer.register(ApplicationManager.getApplication(), client)
            }
        }
    }

    private var browser: JBCefBrowser? = null
    private var component: JComponent? = null
    private var initialized = AtomicBoolean(false)

    fun initialize() {
        if (initialized.compareAndSet(false, true)) {
            getBrowser().cefBrowser.createImmediately()
        }
    }

    fun getBrowser(): JBCefBrowser {
        if (browser == null) {
            configuration.browserLoadingListener.beforeBrowserCreated(configuration)
            browser = JBCefBrowser(client, configuration.initialUrl)

            //periodically update zoom level
            GlobalScope.launch {
                while(browser != null) {
                    delay(50)
                    browser?.zoomLevel = configuration.zoomLevel
                }
            }

            if (configuration.initialHtml != null) {
                loadHtml(configuration.initialHtml!!)
            }
            configuration.browserLoadingListener.afterBrowserCreated(browser!!)
        }
        return browser!!
    }

    fun loadUrl(url: String) {
        getBrowser().loadURL(url)
    }

    fun loadHtml(html: String) {
        getBrowser().loadHTML(html)
    }

    override fun getComponent(): JComponent {
        if (component == null) {
            component = getBrowser().component
            component!!.preferredSize = Dimension(configuration.componentWidth, configuration.componentHeight)
        }
        return component!!
    }

    override fun dispose() {
        if (configuration.autoDisposeBrowser) {
            browser?.dispose()
        }
        browser = null
        component = null
        initialized.set(false)
    }
}
