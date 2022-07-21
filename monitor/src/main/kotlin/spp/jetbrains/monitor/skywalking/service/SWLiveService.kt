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
package spp.jetbrains.monitor.skywalking.service

import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import spp.jetbrains.monitor.skywalking.bridge.ServiceBridge
import spp.protocol.platform.auth.CommandType
import spp.protocol.platform.auth.RolePermission
import spp.protocol.platform.developer.Developer
import spp.protocol.platform.developer.SelfInfo
import spp.protocol.platform.general.Service
import spp.protocol.platform.status.ActiveInstance
import spp.protocol.service.LiveService

/**
 * Implements [LiveService] for SkyWalking-only environments.
 */
class SWLiveService : CoroutineVerticle(), LiveService {

    /**
     * Requires Source++ platform. Fails in SkyWalking-only environments.
     */
    override fun getClients(): Future<JsonObject> {
        return Future.failedFuture("Illegal operation")
    }

    /**
     * Requires Source++ platform. Fails in SkyWalking-only environments.
     */
    override fun getStats(): Future<JsonObject> {
        return Future.failedFuture("Illegal operation")
    }

    override fun getSelf(): Future<SelfInfo> {
        return Future.succeededFuture(
            SelfInfo(
                Developer("system"),
                emptyList(),
                RolePermission.values().filter { it.commandType == CommandType.LIVE_VIEW },
                emptyList()
            )
        )
    }

    override fun getServices(): Future<List<Service>> {
        val promise = Promise.promise<List<Service>>()
        launch(vertx.dispatcher()) {
            val services = ServiceBridge.getActiveServices(vertx)
            promise.complete(services.map { Service(it.id, it.name) })
        }
        return promise.future()
    }

    override fun getActiveProbes(): Future<List<ActiveInstance>> {
        return Future.failedFuture(UnsupportedOperationException("Not implemented"))
    }
}
