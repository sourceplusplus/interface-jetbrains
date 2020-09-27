package com.sourceplusplus.marker.source.mark.api.component.jcef

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefClient
import com.sourceplusplus.marker.source.mark.api.component.api.SourceMarkComponent
import com.sourceplusplus.marker.source.mark.api.component.jcef.config.SourceMarkJcefComponentConfiguration
import org.slf4j.LoggerFactory
import java.awt.Dimension
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JComponent

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SourceMarkJcefComponent(
    override val configuration: SourceMarkJcefComponentConfiguration
) : SourceMarkComponent {

    companion object {
        private val log = LoggerFactory.getLogger(SourceMarkJcefComponent::class.java)
        private val client: JBCefClient by lazy { JBCefApp.getInstance().createClient() }

        init {
            Disposer.register(ApplicationManager.getApplication(), client)
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
