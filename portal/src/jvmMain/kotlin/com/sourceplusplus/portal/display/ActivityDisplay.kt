package com.sourceplusplus.portal.display

import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.portal.extensions.displayActivity
import spp.protocol.ProtocolAddress.Global.ActivityTabOpened
import spp.protocol.ProtocolAddress.Global.ArtifactMetricsUpdated
import spp.protocol.ProtocolAddress.Global.RefreshActivity
import spp.protocol.ProtocolAddress.Global.SetActiveChartMetric
import spp.protocol.ProtocolAddress.Global.SetMetricTimeFrame
import spp.protocol.ProtocolAddress.Portal.ClearActivity
import spp.protocol.artifact.QueryTimeFrame
import spp.protocol.artifact.metrics.ArtifactMetricResult
import spp.protocol.artifact.metrics.MetricType.valueOf
import spp.protocol.portal.PageType
import spp.protocol.utils.ArtifactNameUtils.getShortQualifiedFunctionName
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
class ActivityDisplay(
    private val refreshIntervalMs: Int, private val pullMode: Boolean
) : AbstractDisplay(PageType.ACTIVITY) {

    companion object {
        private val log = LoggerFactory.getLogger(ActivityDisplay::class.java)
    }

    override suspend fun start() {
        if (pullMode) {
            log.info("Log pull mode enabled")
            vertx.setPeriodic(refreshIntervalMs.toLong()) {
                SourcePortal.getPortals().filter {
                    it.configuration.currentPage == PageType.ACTIVITY && (it.visible || it.configuration.external)
                }.forEach {
                    vertx.eventBus().send(RefreshActivity, it)
                }
            }
        } else {
            log.info("Log push mode enabled")
        }

        //refresh with stats from cache (if avail)
        vertx.eventBus().consumer<JsonObject>(ActivityTabOpened) {
            log.info("Activity tab opened")
            val portalUuid = it.body().getString("portalUuid")
            val portal = SourcePortal.getPortal(portalUuid)!!
            portal.configuration.currentPage = thisTab
            SourcePortal.ensurePortalActive(portal)
            updateUI(portal)

            //todo: can likely remove with #301 impl
            //for some reason clearing (resizing) the activity chart is necessary once SourceMarkerPlugin.init()
            //has been called more than once; for now just do it whenever the activity tab is opened
            vertx.eventBus().send(ClearActivity(portal.portalUuid), null)
        }
        vertx.eventBus().consumer<ArtifactMetricResult>(ArtifactMetricsUpdated) {
            val artifactMetricResult = it.body()
            SourcePortal.getPortals(artifactMetricResult.artifactQualifiedName).forEach { portal ->
                portal.activityView.cacheMetricResult(artifactMetricResult)
                updateUI(portal)
            }
        }

        vertx.eventBus().consumer<JsonObject>(SetMetricTimeFrame) {
            val request = JsonObject.mapFrom(it.body())
            val portal = SourcePortal.getPortal(request.getString("portalUuid"))!!
            portal.activityView.timeFrame = QueryTimeFrame.valueOf(request.getString("metricTimeFrame").toUpperCase())
            log.info("Activity time frame set to: " + portal.activityView.timeFrame)
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
                "Artifact metrics updated. Portal uuid: {} - Artifact qualified name: {} - Time frame: {}",
                portal.portalUuid,
                getShortQualifiedFunctionName(artifactMetricResult.artifactQualifiedName),
                artifactMetricResult.timeFrame
            )
        }

        vertx.eventBus().displayActivity(
            portal.portalUuid,
            artifactMetricResult.copy(focus = portal.activityView.activeChartMetric)
        )
    }
}
