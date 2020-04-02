package com.sourceplusplus.plugin.intellij.portal

import com.google.common.base.Joiner
import com.intellij.ui.jcef.JBCefBrowser
import com.sourceplusplus.portal.display.PortalInterface
import com.sourceplusplus.portal.display.PortalTab
import io.netty.handler.codec.http.QueryStringDecoder
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLifeSpanHandler

import javax.swing.*
import java.awt.*
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

/**
 * todo: description
 *
 * @version 0.2.5
 * @since 0.2.5
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class IntelliJPortalInterface extends PortalInterface implements CefLifeSpanHandler {

    private JBCefBrowser parentBrowser
    private JBCefBrowser browser
    private Map<String, String> currentQueryParams = [:]
    private static boolean DARK_MODE

    IntelliJPortalInterface(String portalUuid, JBCefBrowser browser) {
        super(portalUuid)
        this.browser = browser
        if (browser != null) {
            browser.JBCefClient.addLifeSpanHandler(this, browser.cefBrowser)
        }
    }

    void cloneViews(IntelliJPortalInterface portalInterface) {
        parentBrowser = portalInterface.parentBrowser
        super.cloneViews(portalInterface)
    }

    void loadPage(PortalTab tab) {
        loadPage(tab, [:])
    }

    void loadPage(PortalTab tab, Map<String, String> queryParams) {
        queryParams.put("dark_mode", Boolean.toString(DARK_MODE))
        currentQueryParams = new HashMap<>(queryParams)
        def userQuery = Joiner.on("&").withKeyValueSeparator("=").join(queryParams)
        if (userQuery) {
            userQuery = "&$userQuery"
        }
        browser.cefBrowser.loadURL(getPortalUrl(tab, portalUuid, userQuery))
    }

    void close() {
        browser.cefBrowser.close(true)
    }

    void reload() {
        if (browser != null) {
            loadPage(currentTab, currentQueryParams)
        }
    }

    static void updateTheme(boolean dark) {
        DARK_MODE = dark
        IntelliJSourcePortal.getPortals().each {
            it.interface.reload()
        }
    }

    @Override
    boolean onBeforePopup(CefBrowser browser, CefFrame frame, String targetUrl, String targetFrameName) {
        def portal = IntelliJSourcePortal.getPortal(new QueryStringDecoder(
                targetUrl).parameters().get("portal_uuid").get(0))
        if (portal.interface.parentBrowser == null) {
            portal.interface.parentBrowser = JBCefBrowser.getJBCefBrowser(browser)
        }
        def popupBrowser = browser.client.createBrowser(targetUrl, false, false)
        popupBrowser.createImmediately()
        portal.interface.browser = new JBCefBrowser(popupBrowser)
        portal.interface.parentBrowser.JBCefClient.addLifeSpanHandler(this, portal.interface.browser.cefBrowser)

        def popupFrame = new JFrame(portal.interface.viewingPortalArtifact)
        popupFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
        popupFrame.setPreferredSize(new Dimension(800, 600))
        popupFrame.add(portal.interface.browser.component)
        popupFrame.pack()
        popupFrame.setLocationByPlatform(true)
        popupFrame.setVisible(true)
        popupFrame.addWindowListener(new WindowAdapter() {
            @Override
            void windowClosing(WindowEvent e) {
                portal.close()
            }
        })

        return true
    }

    @Override
    void onAfterCreated(CefBrowser cefBrowser) {
    }

    @Override
    void onAfterParentChanged(CefBrowser cefBrowser) {
    }

    @Override
    boolean doClose(CefBrowser cefBrowser) {
        return false
    }

    @Override
    void onBeforeClose(CefBrowser cefBrowser) {
    }
}
