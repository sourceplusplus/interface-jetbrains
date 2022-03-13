/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package spp.jetbrains.sourcemarker.service

import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.impl.jose.JWT
import io.vertx.ext.bridge.BridgeEventType
import io.vertx.ext.eventbus.bridge.tcp.impl.protocol.FrameHelper
import io.vertx.kotlin.coroutines.CoroutineVerticle
import org.slf4j.LoggerFactory
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.sourcemarker.mark.SourceMarkKeys
import spp.jetbrains.sourcemarker.mark.SourceMarkSearch
import spp.jetbrains.sourcemarker.service.discover.TCPServiceDiscoveryBackend
import spp.jetbrains.sourcemarker.settings.SourceMarkerConfig
import spp.protocol.SourceServices.Provide.toLiveViewSubscriberAddress
import spp.protocol.view.LiveViewEvent

class LiveViewManager(private val pluginConfig: SourceMarkerConfig) : CoroutineVerticle() {

    private val log = LoggerFactory.getLogger(LiveViewManager::class.java)

    override suspend fun start() {
        //register listener
        var developer = "system"
        if (pluginConfig.serviceToken != null) {
            val json = JWT.parse(pluginConfig.serviceToken)
            developer = json.getJsonObject("payload").getString("developer_id")
        }

        vertx.eventBus().consumer<JsonObject>(toLiveViewSubscriberAddress(developer)) {
            val event = Json.decodeValue(it.body().toString(), LiveViewEvent::class.java)
            if (log.isTraceEnabled) log.trace("Received live event: {}", event)
            if (!SourceMarker.enabled) {
                log.warn("SourceMarker is not enabled, ignoring live event: {}", event)
                return@consumer
            }

            SourceMarkSearch.findBySubscriptionId(event.subscriptionId)
                ?.getUserData(SourceMarkKeys.VIEW_EVENT_LISTENERS)?.forEach { it.accept(event) }
        }

        FrameHelper.sendFrame(
            BridgeEventType.REGISTER.name.toLowerCase(),
            toLiveViewSubscriberAddress(developer), null,
            JsonObject().apply { pluginConfig.serviceToken?.let { put("auth-token", it) } },
            null, null, TCPServiceDiscoveryBackend.socket!!
        )
    }
}
