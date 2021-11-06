package com.sourceplusplus.portal.display

import com.sourceplusplus.portal.SourcePortal
import spp.protocol.ProtocolAddress.Global.ClickedEndpointArtifact
import spp.protocol.ProtocolAddress.Global.ClosePortal
import spp.protocol.ProtocolAddress.Global.FindAndOpenPortal
import spp.protocol.ProtocolAddress.Global.NavigateToArtifact
import spp.protocol.ProtocolAddress.Global.RefreshOverview
import spp.protocol.ProtocolAddress.Global.SetOverviewTimeFrame
import spp.protocol.artifact.ArtifactQualifiedName
import spp.protocol.artifact.QueryTimeFrame
import spp.protocol.portal.PageType
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory

/**
 * Used to quickly display general data over a range of source code artifacts.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class OverviewDisplay(
    private val refreshIntervalMs: Int, private val pullMode: Boolean
) : AbstractDisplay(PageType.OVERVIEW) {

    companion object {
        private val log = LoggerFactory.getLogger(OverviewDisplay::class.java)
    }

    override suspend fun start() {
        if (pullMode) {
            log.info("Log pull mode enabled")
            vertx.setPeriodic(refreshIntervalMs.toLong()) {
                SourcePortal.getPortals().filter {
                    it.configuration.currentPage == PageType.OVERVIEW && (it.visible || it.configuration.external)
                }.forEach {
                    vertx.eventBus().send(RefreshOverview, it)
                }
            }
        } else {
            log.info("Log push mode enabled")
        }

        vertx.eventBus().consumer<JsonObject>(SetOverviewTimeFrame) {
            val portalUuid = it.body().getString("portalUuid")
            val portal = SourcePortal.getPortal(portalUuid)!!
            portal.overviewView.timeFrame = QueryTimeFrame.valueOf(it.body().getString("queryTimeFrame"))
            vertx.eventBus().send(RefreshOverview, portal)
        }
        vertx.eventBus().consumer<JsonObject>(ClickedEndpointArtifact) {
            val portalUuid = it.body().getString("portalUuid")
            val portal = SourcePortal.getPortal(portalUuid)!!
            val artifactQualifiedName = Json.decodeValue(
                it.body().getJsonObject("artifactQualifiedName").toString(), ArtifactQualifiedName::class.java
            )
            if (!portal.configuration.external) {
                vertx.eventBus().send(ClosePortal, portal)
                vertx.eventBus().send(NavigateToArtifact, artifactQualifiedName)
                vertx.eventBus().send(FindAndOpenPortal, artifactQualifiedName)
            }
        }

        super.start()
    }

    override fun updateUI(portal: SourcePortal) {
    }
}
