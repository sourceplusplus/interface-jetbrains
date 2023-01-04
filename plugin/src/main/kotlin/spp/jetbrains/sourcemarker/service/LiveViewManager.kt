/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2023 CodeBrig, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package spp.jetbrains.sourcemarker.service

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.impl.jose.JWT
import io.vertx.ext.bridge.BridgeEventType
import io.vertx.ext.eventbus.bridge.tcp.impl.protocol.FrameHelper
import io.vertx.kotlin.coroutines.CoroutineVerticle
import spp.jetbrains.marker.SourceMarkerKeys
import spp.jetbrains.sourcemarker.config.SourceMarkerConfig
import spp.jetbrains.sourcemarker.mark.SourceMarkSearch
import spp.jetbrains.sourcemarker.service.discover.TCPServiceDiscoveryBackend
import spp.protocol.service.SourceServices.Subscribe.toLiveViewSubscriberAddress
import spp.protocol.view.LiveViewEvent

class LiveViewManager(
    private val project: Project,
    private val pluginConfig: SourceMarkerConfig
) : CoroutineVerticle() {

    private val log = logger<LiveViewManager>()

    override suspend fun start() {
        //register listener
        var developer = "system"
        if (pluginConfig.serviceToken != null) {
            val json = JWT.parse(pluginConfig.serviceToken)
            developer = json.getJsonObject("payload").getString("developer_id")
        }

        vertx.eventBus().consumer<JsonObject>(toLiveViewSubscriberAddress(developer)) {
            val event = LiveViewEvent(it.body())
            if (log.isTraceEnabled) log.trace("Received live event: $event")

            //todo: remove in favor of sending events to individual subscribers
            SourceMarkSearch.findBySubscriptionId(project, event.subscriptionId)
                ?.getUserData(SourceMarkerKeys.VIEW_EVENT_LISTENERS)?.forEach { it.accept(event) }

            vertx.eventBus().publish(toLiveViewSubscriberAddress(event.subscriptionId), it.body())
        }

        FrameHelper.sendFrame(
            BridgeEventType.REGISTER.name.lowercase(),
            toLiveViewSubscriberAddress(developer), null,
            JsonObject().apply { pluginConfig.serviceToken?.let { put("auth-token", it) } },
            null, null, TCPServiceDiscoveryBackend.getSocket(project)
        )
    }
}
