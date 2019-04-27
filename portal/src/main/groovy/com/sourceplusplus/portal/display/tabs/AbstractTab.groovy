package com.sourceplusplus.portal.display.tabs

import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.portal.coordinate.track.PortalViewTracker
import com.sourceplusplus.portal.display.PortalTab
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject

abstract class AbstractTab extends AbstractVerticle {

    private PortalTab thisTab

    AbstractTab(PortalTab thisTab) {
        this.thisTab = thisTab
    }

    @Override
    void start() throws Exception {
        vertx.eventBus().consumer(PortalViewTracker.OPENED_PORTAL, {
            def portal = SourcePortal.getPortal(JsonObject.mapFrom(it.body()).getString("portal_uuid"))
            if (portal.interface.currentTab == thisTab) {
                println "opened portal at this tab: " + thisTab
            }
        })
    }

    abstract void updateUI(SourcePortal portal)
}
