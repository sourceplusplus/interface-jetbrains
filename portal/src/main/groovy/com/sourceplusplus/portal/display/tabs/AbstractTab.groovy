package com.sourceplusplus.portal.display.tabs

import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.portal.coordinate.track.PortalViewTracker
import com.sourceplusplus.portal.display.PortalTab
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject

/**
 * Contains common portal tab functionality.
 *
 * @version 0.2.1
 * @since 0.2.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
abstract class AbstractTab extends AbstractVerticle {

    private final PortalTab thisTab

    AbstractTab(PortalTab thisTab) {
        this.thisTab = thisTab
    }

    @Override
    void start() throws Exception {
        vertx.eventBus().consumer(PortalViewTracker.OPENED_PORTAL, {
            def portal = SourcePortal.getPortal(JsonObject.mapFrom(it.body()).getString("portal_uuid"))
            if (portal.interface.currentTab == thisTab) {
                updateUI(portal)

                //https://github.com/CodeBrig/Journey/issues/13
                if (portal.interface.browser != null && System.getProperty("os.name").toLowerCase().startsWith("windows")) {
                    portal.interface.browser.browser.setZoomLevel(-1.5)
                }
            }
        })
    }

    PortalTab getThisTab() {
        return thisTab
    }

    abstract void updateUI(SourcePortal portal)
}
