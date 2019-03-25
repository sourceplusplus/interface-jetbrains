package com.sourceplusplus.tooltip.coordinate.track

import com.sourceplusplus.api.model.artifact.SourceArtifact
import com.sourceplusplus.api.model.QueryTimeFrame
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Used to track the current viewable state of the Source++ Tooltip.
 *
 * Recognizes and produces messages for the following events:
 *  - user hovered over S++ icon
 *  - user opened/closed tooltip
 *
 * @version 0.1.3
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class TooltipViewTracker extends AbstractVerticle {

    public static final String UPDATE_TOOLTIP_ARTIFACT = "UpdateTooltipArtifact"
    public static final String CAN_OPEN_TOOLTIP = "CanOpenTooltip"
    public static final String OPENED_TOOLTIP = "OpenedTooltip"
    public static final String CLOSED_TOOLTIP = "ClosedTooltip"
    public static final String CHANGED_TOOLTIP_ARTIFACT = "ChangedTooltipArtifact"
    public static final String UPDATED_METRIC_TIME_FRAME = "UpdatedMetricTimeFrame"

    private static final Logger log = LoggerFactory.getLogger(this.name)
    public static String viewingTooltipArtifact //todo: better
    public static QueryTimeFrame currentMetricTimeFrame = QueryTimeFrame.LAST_15_MINUTES
    public static String viewingTab //todo: impl (and then better)

    @Override
    void start() throws Exception {
        //user wants to open tooltip
        vertx.eventBus().consumer(CAN_OPEN_TOOLTIP, { messageHandler ->
            messageHandler.reply(true)
        })

        //user opened tooltip
        vertx.eventBus().consumer(OPENED_TOOLTIP, {
            if (it.body() instanceof SourceArtifact) {
                def artifact = it.body() as SourceArtifact
                log.info("Showing IDE tooltip for source artifact: {}", artifact.artifactQualifiedName())
                //todo: reset ui if artifact different than last artifact
            }
        })

        //user closed tooltip
        vertx.eventBus().consumer(CLOSED_TOOLTIP, {
            if (it.body() instanceof SourceArtifact) {
                def artifact = it.body() as SourceArtifact
                log.info("Hiding IDE tooltip for source artifact: {}", artifact.artifactQualifiedName())
            }
        })

        vertx.eventBus().consumer(UPDATE_TOOLTIP_ARTIFACT, {
            if (((String) it.body()) != viewingTooltipArtifact) {
                viewingTooltipArtifact = (String) it.body()
                vertx.eventBus().publish(CHANGED_TOOLTIP_ARTIFACT, viewingTooltipArtifact)
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
