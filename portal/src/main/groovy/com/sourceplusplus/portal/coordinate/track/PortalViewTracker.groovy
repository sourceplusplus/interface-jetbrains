package com.sourceplusplus.portal.coordinate.track

import com.sourceplusplus.api.model.QueryTimeFrame
import com.sourceplusplus.api.model.artifact.SourceArtifact
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
    public static String viewingPortalArtifact //todo: better
    public static QueryTimeFrame currentMetricTimeFrame = QueryTimeFrame.LAST_15_MINUTES
    public static String viewingTab //todo: impl (and then better)

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
            if (((String) it.body()) != viewingPortalArtifact) {
                viewingPortalArtifact = (String) it.body()
                vertx.eventBus().publish(CHANGED_PORTAL_ARTIFACT, viewingPortalArtifact)
            }
        })
        vertx.eventBus().consumer("SetMetricTimeFrame", {
            def message = JsonObject.mapFrom(it.body())
            def metricTimeFrame = QueryTimeFrame.valueOf(message.getString("value").toUpperCase())
            if (metricTimeFrame != currentMetricTimeFrame) {
                log.debug("Metric time frame updated to: " + (currentMetricTimeFrame = metricTimeFrame))
                vertx.eventBus().publish(UPDATED_METRIC_TIME_FRAME, currentMetricTimeFrame.toString())
            }
        })

        log.debug("Initial time frame set to: " + currentMetricTimeFrame)
        vertx.eventBus().publish(UPDATED_METRIC_TIME_FRAME, currentMetricTimeFrame.toString())
        log.info("{} started", getClass().getSimpleName())
    }
}
