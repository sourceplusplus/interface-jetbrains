package com.sourceplusplus.portal.coordinate.track

import com.sourceplusplus.api.model.QueryTimeFrame
import com.sourceplusplus.api.model.artifact.SourceArtifact
import com.sourceplusplus.portal.SourcePortal
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Used to track the current viewable state of the Source++ Portal.
 *
 * Recognizes and produces messages for the following events:
 *  - user hovered over S++ icon
 *  - user opened/closed portal
 *
 * @version 0.2.0
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class PortalViewTracker extends AbstractVerticle {

    public static final String UPDATE_PORTAL_ARTIFACT = "UpdatePortalArtifact"
    public static final String CAN_OPEN_PORTAL = "CanOpenPortal"
    public static final String OPENED_PORTAL = "OpenedPortal"
    public static final String CLOSED_PORTAL = "ClosedPortal"
    public static final String CHANGED_PORTAL_ARTIFACT = "ChangedPortalArtifact"
    public static final String UPDATED_METRIC_TIME_FRAME = "UpdatedMetricTimeFrame"

    private static final Logger log = LoggerFactory.getLogger(this.name)

    @Override
    void start() throws Exception {
        //user wants to open portal
        vertx.eventBus().consumer(CAN_OPEN_PORTAL, { messageHandler ->
            messageHandler.reply(true)
        })

        //user opened portal
        vertx.eventBus().consumer(OPENED_PORTAL, {
            if (it.body() instanceof SourceArtifact) {
                def artifact = it.body() as SourceArtifact
                log.info("Showing Source++ Portal for artifact: {}", artifact.artifactQualifiedName())
                //todo: reset ui if artifact different than last artifact
            }
        })

        //user closed portal
        vertx.eventBus().consumer(CLOSED_PORTAL, {
            if (it.body() instanceof SourceArtifact) {
                def artifact = it.body() as SourceArtifact
                log.info("Hiding Source++ Portal for artifact: {}", artifact.artifactQualifiedName())
            }
        })

        vertx.eventBus().consumer(UPDATE_PORTAL_ARTIFACT, {
            def request = JsonObject.mapFrom(it.body())
            def portalId = request.getInteger("portal_id")
            def artifactQualifiedName = request.getString("artifact_qualified_name")

            def portal = SourcePortal.getPortal(portalId)
            if (artifactQualifiedName != portal.portalUI.viewingPortalArtifact) {
                portal.portalUI.viewingPortalArtifact = artifactQualifiedName
                vertx.eventBus().publish(CHANGED_PORTAL_ARTIFACT,
                        new JsonObject().put("portal_id", portalId)
                                .put("artifact_qualified_name", artifactQualifiedName)
                )
            }
        })
        vertx.eventBus().consumer("SetMetricTimeFrame", {
            def message = JsonObject.mapFrom(it.body())
            def portal = SourcePortal.getPortal(message.getInteger("portal_id"))
            def metricTimeFrame = QueryTimeFrame.valueOf(message.getString("metric_time_frame").toUpperCase())

            if (metricTimeFrame != portal.portalUI.currentMetricTimeFrame) {
                log.debug("Metric time frame updated to: " + (portal.portalUI.currentMetricTimeFrame = metricTimeFrame))
                vertx.eventBus().publish(UPDATED_METRIC_TIME_FRAME,
                        new JsonObject().put("portal_id", portal.portalId)
                                .put("metric_time_frame", portal.portalUI.currentMetricTimeFrame.toString())
                )
            }
        })

        //log.debug("Initial time frame set to: " + currentMetricTimeFrame)
        //vertx.eventBus().publish(UPDATED_METRIC_TIME_FRAME, currentMetricTimeFrame.toString())
        log.info("{} started", getClass().getSimpleName())
    }
}
