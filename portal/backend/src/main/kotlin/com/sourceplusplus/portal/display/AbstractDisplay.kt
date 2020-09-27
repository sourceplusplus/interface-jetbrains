package com.sourceplusplus.portal.display

import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.OpenedPortal
import com.sourceplusplus.protocol.portal.PageType
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle

/**
 * Contains common portal tab functionality.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
abstract class AbstractDisplay(val thisTab: PageType) : CoroutineVerticle() {

    override suspend fun start() {
        vertx.eventBus().consumer<JsonObject>(OpenedPortal) {
            val portalUuid = JsonObject.mapFrom(it.body()).getString("portal_uuid")
            val portal = SourcePortal.getPortal(portalUuid)!!
            if (portal.currentTab == thisTab) {
                updateUI(portal)
            }
        }
    }

    abstract fun updateUI(portal: SourcePortal)
}
