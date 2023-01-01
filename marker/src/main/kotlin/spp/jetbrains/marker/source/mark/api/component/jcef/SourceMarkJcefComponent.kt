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
package spp.jetbrains.marker.source.mark.api.component.jcef

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefClient
import kotlinx.coroutines.delay
import org.joor.Reflect
import org.joor.ReflectException
import spp.jetbrains.ScopeExtensions.safeGlobalLaunch
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
        private val client: JBCefClient by lazy {
            val client = JBCefApp.getInstance().createClient()
            if (!ApplicationManager.getApplication().isUnitTestMode) {
                Disposer.register(ApplicationManager.getApplication(), client)
            }
            client
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
            safeGlobalLaunch {
                while (browser != null) {
                    delay(50)
                    try {
                        browser?.let { Reflect.on(it).call("setZoomLevel", configuration.zoomLevel) }
                    } catch (ignore: ReflectException) {
                    }
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
