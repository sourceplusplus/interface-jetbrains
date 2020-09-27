package com.sourceplusplus.portal

import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.CanOpenPortal
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.ChangedPortalArtifact
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.ClickedViewAsExternalPortal
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.ClosePortal
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.KeepAlivePortal
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.UpdatePortalArtifact
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
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class PortalViewTracker : CoroutineVerticle() {

    companion object {
        private val log = LoggerFactory.getLogger(PortalViewTracker::class.java)
    }

    override suspend fun start() {
        //get portal from cache to ensure it remains active
        vertx.eventBus().consumer<JsonObject>(KeepAlivePortal) { messageHandler ->
            val portalUuid = JsonObject.mapFrom(messageHandler.body()).getString("portal_uuid")
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
            val portal = SourcePortal.getPortal(JsonObject.mapFrom(it.body()).getString("portal_uuid"))!!
            //close internal portal
            if (!portal.external) vertx.eventBus().send(ClosePortal, portal)
            //open external portal
            it.reply(JsonObject().put("portal_uuid", portal.createExternalPortal().portalUuid))
        }

        vertx.eventBus().consumer<JsonObject>(UpdatePortalArtifact) {
            val request = JsonObject.mapFrom(it.body())
            val portalUuid = request.getString("portal_uuid")
            val artifactQualifiedName = request.getString("artifact_qualified_name")

            val portal = SourcePortal.getPortal(portalUuid)!!
            if (artifactQualifiedName != portal.viewingPortalArtifact) {
                portal.viewingPortalArtifact = artifactQualifiedName
                vertx.eventBus().publish(
                    ChangedPortalArtifact,
                    JsonObject().put("portal_uuid", portalUuid)
                        .put("artifact_qualified_name", artifactQualifiedName)
                )
            }
        }
        log.info("{} started", javaClass.simpleName)
    }
}
