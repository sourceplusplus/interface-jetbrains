package com.sourceplusplus.portal.display

import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.portal.model.PageType
import com.sourceplusplus.protocol.ProtocolAddress.Global.OverviewTabOpened
import com.sourceplusplus.protocol.ProtocolAddress.Global.RefreshOverview
import com.sourceplusplus.protocol.ProtocolAddress.Global.SetOverviewTimeFrame
import com.sourceplusplus.protocol.artifact.QueryTimeFrame
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory

/**
 * Used to quickly display general data over a range of source code artifacts.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class OverviewDisplay : AbstractDisplay(PageType.OVERVIEW) {

    companion object {
        private val log = LoggerFactory.getLogger(OverviewDisplay::class.java)
    }

    override suspend fun start() {
        vertx.eventBus().consumer<JsonObject>(OverviewTabOpened) {
            val portalUuid = it.body().getString("portalUuid")
            val portal = SourcePortal.getPortal(portalUuid)!!
            portal.currentTab = PageType.OVERVIEW
            vertx.eventBus().send(RefreshOverview, it.body())
        }
        vertx.eventBus().consumer<JsonObject>(SetOverviewTimeFrame) {
            val portalUuid = it.body().getString("portalUuid")
            val portal = SourcePortal.getPortal(portalUuid)!!
            portal.overviewView.timeFrame = QueryTimeFrame.valueOf(it.body().getString("queryTimeFrame"))
            vertx.eventBus().send(RefreshOverview, it.body())
        }
    }

    override fun updateUI(portal: SourcePortal) {
    }
}
