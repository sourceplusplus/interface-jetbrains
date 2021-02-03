package com.sourceplusplus.portal.display

import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.portal.extensions.displayActivity
import com.sourceplusplus.protocol.ProtocolAddress.Global.ActivityTabOpened
import com.sourceplusplus.protocol.ProtocolAddress.Global.ArtifactMetricsUpdated
import com.sourceplusplus.protocol.portal.PageType
import com.sourceplusplus.protocol.ProtocolAddress.Global.RefreshActivity
import com.sourceplusplus.protocol.ProtocolAddress.Global.SetActiveChartMetric
import com.sourceplusplus.protocol.ProtocolAddress.Global.SetMetricTimeFrame
import com.sourceplusplus.protocol.ProtocolAddress.Portal.ClearActivity
import com.sourceplusplus.protocol.artifact.QueryTimeFrame
import com.sourceplusplus.protocol.artifact.metrics.*
import com.sourceplusplus.protocol.artifact.metrics.MetricType.*
import com.sourceplusplus.protocol.utils.ArtifactNameUtils.getShortQualifiedFunctionName
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory

/**
 * Displays general source code artifact statistics.
 * Useful for gathering an overall view of an artifact's runtime behavior.
 *
 * Viewable artifact metrics:
 *  - Average throughput
 *  - Average response time
 *  - 99/95/90/75/50 response time percentiles
 *  - Minimum/Maximum response time
 *  - Average SLA
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class ActivityDisplay : AbstractDisplay(PageType.ACTIVITY) {

    companion object {
        private val log = LoggerFactory.getLogger(ActivityDisplay::class.java)
    }

    override suspend fun start() {
        vertx.setPeriodic(5000) {
            SourcePortal.getPortals().filter {
                it.configuration.currentPage == PageType.ACTIVITY && (it.visible || it.configuration.external)
            }.forEach {
                vertx.eventBus().send(RefreshActivity, it)
            }
        }

        //refresh with stats from cache (if avail)
        vertx.eventBus().consumer<JsonObject>(ActivityTabOpened) {
            log.info("Activity tab opened")
            val portalUuid = it.body().getString("portalUuid")
            val portal = SourcePortal.getPortal(portalUuid)!!
            portal.configuration.currentPage = thisTab
            SourcePortal.ensurePortalActive(portal)
            updateUI(portal)

            //for some reason clearing (resizing) the activity chart is necessary once SourceMarkerPlugin.init()
            //has been called more than once; for now just do it whenever the activity tab is opened
            vertx.eventBus().send(ClearActivity(portal.portalUuid), null)
        }
        vertx.eventBus().consumer<ArtifactMetricResult>(ArtifactMetricsUpdated) {
            val artifactMetricResult = it.body()
            SourcePortal.getPortals(artifactMetricResult.appUuid, artifactMetricResult.artifactQualifiedName)
                .forEach { portal ->
                    portal.activityView.cacheMetricResult(artifactMetricResult)
                    updateUI(portal)
                }
        }

        vertx.eventBus().consumer<JsonObject>(SetMetricTimeFrame) {
            val request = JsonObject.mapFrom(it.body())
            val portal = SourcePortal.getPortal(request.getString("portalUuid"))!!
            val view = portal.activityView
            view.timeFrame = QueryTimeFrame.valueOf(request.getString("metricTimeFrame").toUpperCase())
            log.info("Activity time frame set to: " + view.timeFrame)
            updateUI(portal)

            vertx.eventBus().send(RefreshActivity, portal)
        }
        vertx.eventBus().consumer<JsonObject>(SetActiveChartMetric) {
            val request = JsonObject.mapFrom(it.body())
            val portal = SourcePortal.getPortal(request.getString("portalUuid"))!!
            portal.activityView.activeChartMetric = valueOf(request.getString("metricType"))
            updateUI(portal)

            vertx.eventBus().send(RefreshActivity, portal)
        }

        super.start()
    }

    override fun updateUI(portal: SourcePortal) {
        if (portal.configuration.currentPage != thisTab) {
            return
        }

        val artifactMetricResult = portal.activityView.metricResult ?: return
        if (log.isTraceEnabled) {
            log.trace(
                "Artifact metrics updated. Portal uuid: {} - App uuid: {} - Artifact qualified name: {} - Time frame: {}",
                portal.portalUuid,
                artifactMetricResult.appUuid,
                getShortQualifiedFunctionName(artifactMetricResult.artifactQualifiedName),
                artifactMetricResult.timeFrame
            )
        }

        vertx.eventBus().displayActivity(
            portal.portalUuid,
            artifactMetricResult.copy(focus = portal.activityView.activeChartMetric)
        )
        log.trace(
            "Displayed metrics for artifact: {} - Portal: {}",
            getShortQualifiedFunctionName(artifactMetricResult.artifactQualifiedName),
            portal.portalUuid
        )
    }
}
