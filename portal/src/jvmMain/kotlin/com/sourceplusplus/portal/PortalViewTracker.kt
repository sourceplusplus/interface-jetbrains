package com.sourceplusplus.portal

import spp.protocol.ProtocolAddress.Global.CanOpenPortal
import spp.protocol.ProtocolAddress.Global.ChangedPortalArtifact
import spp.protocol.ProtocolAddress.Global.ClickedViewAsExternalPortal
import spp.protocol.ProtocolAddress.Global.ClosePortal
import spp.protocol.ProtocolAddress.Global.KeepAlivePortal
import spp.protocol.ProtocolAddress.Global.UpdatePortalArtifact
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import org.slf4j.LoggerFactory

/**
 * Used to track the current viewable state of the SourceMarker Portal.
 *
 * Recognizes and produces messages for the following events:
 *  - user hovered over S++ icon
 *  - user opened/closed portal
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class PortalViewTracker : CoroutineVerticle() {

    companion object {
        private val log = LoggerFactory.getLogger(PortalViewTracker::class.java)
    }

    override suspend fun start() {
        //get portal from cache to ensure it remains active
        vertx.eventBus().consumer<JsonObject>(KeepAlivePortal) { messageHandler ->
            val portalUuid = JsonObject.mapFrom(messageHandler.body()).getString("portalUuid")
            val portal = SourcePortal.getPortal(portalUuid)
            if (portal != null) {
                SourcePortal.ensurePortalActive(portal)
                messageHandler.reply(200)
            } else {
                log.warn("Failed to find portal. Portal UUID: {}", portalUuid)
                messageHandler.fail(404, "Portal not found")
            }
        }

        //user wants to open portal
        vertx.eventBus().consumer<Void>(CanOpenPortal) { messageHandler ->
            messageHandler.reply(true)
        }

        //user wants a new external portal
        vertx.eventBus().consumer<JsonObject>(ClickedViewAsExternalPortal) {
            val portal = SourcePortal.getPortal(JsonObject.mapFrom(it.body()).getString("portalUuid"))!!
            //close internal portal
            if (!portal.configuration.external) vertx.eventBus().send(ClosePortal, portal)
            //open external portal
            it.reply(JsonObject().put("portalUuid", portal.createExternalPortal().portalUuid))
        }

        vertx.eventBus().consumer<JsonObject>(UpdatePortalArtifact) {
            val request = JsonObject.mapFrom(it.body())
            val portalUuid = request.getString("portalUuid")
            val artifactQualifiedName = request.getString("artifact_qualified_name")

            val portal = SourcePortal.getPortal(portalUuid)!!
            if (artifactQualifiedName != portal.viewingPortalArtifact) {
                portal.viewingPortalArtifact = artifactQualifiedName
                vertx.eventBus().publish(
                    ChangedPortalArtifact,
                    JsonObject().put("portalUuid", portalUuid)
                        .put("artifact_qualified_name", artifactQualifiedName)
                )
            }
        }
        log.info("{} started", javaClass.simpleName)
    }
}
