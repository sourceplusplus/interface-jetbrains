package com.sourceplusplus.portal.coordinate.track

import com.sourceplusplus.api.model.artifact.SourceArtifact
import com.sourceplusplus.portal.SourcePortal
import groovy.util.logging.Slf4j
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject

/**
 * Used to track the current viewable state of the Source++ Portal.
 *
 * Recognizes and produces messages for the following events:
 *  - user hovered over S++ icon
 *  - user opened/closed portal
 *
 * @version 0.2.4
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class PortalViewTracker extends AbstractVerticle {

    public static final String KEEP_ALIVE_PORTAL = "KeepAlivePortal"
    public static final String UPDATE_PORTAL_ARTIFACT = "UpdatePortalArtifact"
    public static final String CAN_OPEN_PORTAL = "CanOpenPortal"
    public static final String OPENED_PORTAL = "OpenedPortal"
    public static final String CLOSED_PORTAL = "ClosedPortal"
    public static final String CHANGED_PORTAL_ARTIFACT = "ChangedPortalArtifact"
    public static final String CLICKED_VIEW_AS_EXTERNAL_PORTAL = "ClickedViewAsExternalPortal"

    @Override
    void start() throws Exception {
        //get portal from cache to ensure it remains active
        vertx.eventBus().consumer(KEEP_ALIVE_PORTAL, { messageHandler ->
            SourcePortal.ensurePortalActive(SourcePortal.getPortal(JsonObject.mapFrom(
                    messageHandler.body()).getString("portal_uuid")))
        })

        //user wants to open portal
        vertx.eventBus().consumer(CAN_OPEN_PORTAL, { messageHandler ->
            messageHandler.reply(true)
        })

        //user wants a new external portal
        vertx.eventBus().consumer(CLICKED_VIEW_AS_EXTERNAL_PORTAL, { messageHandler ->
            def portal = SourcePortal.getPortal(JsonObject.mapFrom(messageHandler.body()).getString("portal_uuid"))
            messageHandler.reply(new JsonObject().put("portal_uuid", portal.createExternalPortal().portalUuid))
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
            def portalUuid = request.getString("portal_uuid")
            def artifactQualifiedName = request.getString("artifact_qualified_name")

            def portal = SourcePortal.getPortal(portalUuid)
            if (artifactQualifiedName != portal.interface.viewingPortalArtifact) {
                portal.interface.viewingPortalArtifact = artifactQualifiedName
                vertx.eventBus().publish(CHANGED_PORTAL_ARTIFACT,
                        new JsonObject().put("portal_uuid", portalUuid)
                                .put("artifact_qualified_name", artifactQualifiedName)
                )
            }
        })
        log.info("{} started", getClass().getSimpleName())
    }
}
