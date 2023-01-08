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
package spp.jetbrains.monitor.skywalking.service

import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import spp.jetbrains.monitor.skywalking.bridge.ServiceBridge
import spp.protocol.platform.auth.*
import spp.protocol.platform.developer.Developer
import spp.protocol.platform.developer.SelfInfo
import spp.protocol.platform.general.Service
import spp.protocol.platform.general.ServiceEndpoint
import spp.protocol.platform.general.ServiceInstance
import spp.protocol.platform.status.InstanceConnection
import spp.protocol.service.LiveManagementService

/**
 * Implements [LiveManagementService] for SkyWalking-only environments.
 */
class SWLiveManagementService : CoroutineVerticle(), LiveManagementService {

    /**
     * Requires Source++ platform. Fails in SkyWalking-only environments.
     */
    override fun getRolePermissions(role: DeveloperRole): Future<List<RolePermission>> {
        return Future.failedFuture("Illegal operation")
    }

    /**
     * Requires Source++ platform. Fails in SkyWalking-only environments.
     */
    override fun getClientAccessors(): Future<List<ClientAccess>> {
        return Future.failedFuture("Illegal operation")
    }

    /**
     * Requires Source++ platform. Fails in SkyWalking-only environments.
     */
    override fun getClientAccess(id: String): Future<ClientAccess?> {
        return Future.failedFuture("Illegal operation")
    }

    /**
     * Requires Source++ platform. Fails in SkyWalking-only environments.
     */
    override fun addClientAccess(): Future<ClientAccess> {
        return Future.failedFuture("Illegal operation")
    }

    /**
     * Requires Source++ platform. Fails in SkyWalking-only environments.
     */
    override fun addDeveloper(developerId: String): Future<Developer> {
        return Future.failedFuture(UnsupportedOperationException("Not implemented"))
    }

    override fun removeDeveloper(developerId: String): Future<Void> {
        return Future.failedFuture("Illegal operation")
    }

    override fun refreshDeveloperToken(developerId: String): Future<Developer> {
        return Future.failedFuture("Illegal operation")
    }

    override fun getRoles(): Future<List<DeveloperRole>> {
        return Future.failedFuture("Illegal operation")
    }

    /**
     * Requires Source++ platform. Fails in SkyWalking-only environments.
     */
    override fun addDeveloperRole(developerId: String, role: DeveloperRole): Future<Void> {
        return Future.failedFuture(UnsupportedOperationException("Not implemented"))
    }

    override fun removeDeveloperRole(developerId: String, role: DeveloperRole): Future<Void> {
        return Future.failedFuture("Illegal operation")
    }

    override fun getDeveloperPermissions(developerId: String): Future<List<RolePermission>> {
        return Future.failedFuture("Illegal operation")
    }

    override fun reset(): Future<Void> {
        return Future.failedFuture("Illegal operation")
    }

    /**
     * Requires Source++ platform. Fails in SkyWalking-only environments.
     */
    override fun addRole(role: DeveloperRole): Future<Boolean> {
        return Future.failedFuture(UnsupportedOperationException("Not implemented"))
    }

    override fun removeRole(role: DeveloperRole): Future<Boolean> {
        return Future.failedFuture("Illegal operation")
    }

    override fun getDeveloperRoles(developerId: String): Future<List<DeveloperRole>> {
        return Future.failedFuture("Illegal operation")
    }

    /**
     * Requires Source++ platform. Fails in SkyWalking-only environments.
     */
    override fun addRolePermission(role: DeveloperRole, permission: RolePermission): Future<Void> {
        return Future.failedFuture(UnsupportedOperationException("Not implemented"))
    }

    override fun removeRolePermission(role: DeveloperRole, permission: RolePermission): Future<Void> {
        return Future.failedFuture("Illegal operation")
    }

    /**
     * Requires Source++ platform. Fails in SkyWalking-only environments.
     */
    override fun removeClientAccess(id: String): Future<Boolean> {
        return Future.failedFuture("Illegal operation")
    }

    /**
     * Requires Source++ platform. Fails in SkyWalking-only environments.
     */
    override fun refreshClientAccess(id: String): Future<ClientAccess> {
        return Future.failedFuture("Illegal operation")
    }

    /**
     * Requires Source++ platform. Fails in SkyWalking-only environments.
     */
    override fun getClients(): Future<JsonObject> {
        return Future.failedFuture("Illegal operation")
    }

    override fun getEndpoints(serviceId: String): Future<List<ServiceEndpoint>> {
        return Future.failedFuture("Illegal operation")
    }

    override fun getInstances(serviceId: String): Future<List<ServiceInstance>> {
        return Future.failedFuture("Illegal operation")
    }

    /**
     * Requires Source++ platform. Fails in SkyWalking-only environments.
     */
    override fun getStats(): Future<JsonObject> {
        return Future.failedFuture("Illegal operation")
    }

    override fun getVersion(): Future<String> {
        return Future.failedFuture("Illegal operation")
    }

    override fun getAccessPermissions(): Future<List<AccessPermission>> {
        return Future.failedFuture("Illegal operation")
    }

    override fun getAccessPermission(id: String): Future<AccessPermission> {
        return Future.failedFuture("Illegal operation")
    }

    override fun addAccessPermission(locationPatterns: List<String>, type: AccessType): Future<AccessPermission> {
        return Future.failedFuture("Illegal operation")
    }

    override fun removeAccessPermission(id: String): Future<Void> {
        return Future.failedFuture("Illegal operation")
    }

    override fun getRoleAccessPermissions(role: DeveloperRole): Future<List<AccessPermission>> {
        return Future.failedFuture("Illegal operation")
    }

    override fun addRoleAccessPermission(role: DeveloperRole, id: String): Future<Void> {
        return Future.failedFuture("Illegal operation")
    }

    override fun removeRoleAccessPermission(role: DeveloperRole, id: String): Future<Void> {
        return Future.failedFuture("Illegal operation")
    }

    override fun getDeveloperAccessPermissions(developerId: String): Future<List<AccessPermission>> {
        return Future.failedFuture("Illegal operation")
    }

    override fun getDataRedactions(): Future<List<DataRedaction>> {
        return Future.failedFuture("Illegal operation")
    }

    override fun getDataRedaction(id: String): Future<DataRedaction> {
        return Future.failedFuture("Illegal operation")
    }

    override fun addDataRedaction(
        id: String,
        type: RedactionType,
        lookup: String,
        replacement: String
    ): Future<DataRedaction> {
        return Future.failedFuture("Illegal operation")
    }

    override fun updateDataRedaction(
        id: String,
        type: RedactionType,
        lookup: String,
        replacement: String
    ): Future<DataRedaction> {
        return Future.failedFuture("Illegal operation")
    }

    override fun removeDataRedaction(id: String): Future<Void> {
        return Future.failedFuture("Illegal operation")
    }

    override fun getRoleDataRedactions(role: DeveloperRole): Future<List<DataRedaction>> {
        return Future.failedFuture("Illegal operation")
    }

    override fun addRoleDataRedaction(role: DeveloperRole, id: String): Future<Void> {
        return Future.failedFuture("Illegal operation")
    }

    override fun removeRoleDataRedaction(role: DeveloperRole, id: String): Future<Void> {
        return Future.failedFuture("Illegal operation")
    }

    override fun getDeveloperDataRedactions(developerId: String): Future<List<DataRedaction>> {
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

    override fun getServices(layer: String?): Future<List<Service>> {
        return Future.failedFuture("Illegal operation")
    }

    /**
     * Requires Source++ platform. Fails in SkyWalking-only environments.
     */
    override fun getActiveProbes(): Future<List<InstanceConnection>> {
        return Future.failedFuture(UnsupportedOperationException("Not implemented"))
    }

    /**
     * Requires Source++ platform. Fails in SkyWalking-only environments.
     */
    override fun getAuthToken(accessToken: String): Future<String> {
        return Future.failedFuture(UnsupportedOperationException("Not implemented"))
    }

    override fun getDevelopers(): Future<List<Developer>> {
        return Future.failedFuture("Illegal operation")
    }

    /**
     * Requires Source++ platform. Fails in SkyWalking-only environments.
     */
    override fun getActiveProbe(id: String): Future<InstanceConnection?> {
        return Future.failedFuture("Illegal operation")
    }

    /**
     * Requires Source++ platform. Fails in SkyWalking-only environments.
     */
    override fun updateActiveProbeMetadata(id: String, metadata: JsonObject): Future<InstanceConnection> {
        return Future.failedFuture("Illegal operation")
    }
}
