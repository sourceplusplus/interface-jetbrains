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
package spp.jetbrains.marker.source.info

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import io.vertx.core.Future
import io.vertx.kotlin.coroutines.await
import spp.jetbrains.SourceKey
import spp.jetbrains.UserData
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.source.mark.guide.GuideMark
import spp.jetbrains.safeLaunch
import spp.jetbrains.status.SourceStatusListener
import spp.jetbrains.status.SourceStatusService

/**
 * Base class for endpoint detectors. Concrete endpoint detectors are responsible for determining the endpoint name(s)
 * for a given [GuideMark]. The base class will then use the endpoint name(s) to determine the endpoint id(s). The
 * endpoint id(s) are then used to associate endpoint telemetry with the [GuideMark].
 *
 * @since 0.5.5
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
abstract class EndpointDetector<T : EndpointDetector.EndpointNameDetector>(val project: Project) {

    companion object {
        private val log = logger<EndpointDetector<*>>()
        val DETECTED_ENDPOINTS = SourceKey<List<DetectedEndpoint>>("DETECTED_ENDPOINTS")
        val ENDPOINT_FOUND = SourceKey<Boolean>("ENDPOINT_FOUND")
        private val REDETECTOR_SETUP = Key.create<Boolean>("SPP_REDETECTOR_SETUP")
        val httpMethods = setOf("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS", "TRACE")
    }

    abstract val detectorSet: Set<T>

    init {
        if (!ApplicationManager.getApplication().isUnitTestMode) {
            setupRedetector()
        }
    }

    private fun setupRedetector() {
        if (project.getUserData(REDETECTOR_SETUP) != true) {
            project.messageBus.connect().subscribe(SourceStatusListener.TOPIC, SourceStatusListener {
                if (it.disposedPlugin) project.putUserData(REDETECTOR_SETUP, false)
            })
            project.putUserData(REDETECTOR_SETUP, true)
            log.info("Setting up endpoint re-detector for project ${project.name}")

            val vertx = UserData.vertx(project)
            vertx.setPeriodic(5000) {
                vertx.safeLaunch { redetectEndpoints() }
            }
        }
    }

    private suspend fun redetectEndpoints() {
        val redetectIds = SourceMarker.getInstance(project).getGuideMarks().filter {
            it.getUserData(DETECTED_ENDPOINTS)?.any { it.id == null } == true
        }.ifEmpty { return }

        log.trace("Redetecting endpoints ids for project ${project.name}")
        redetectIds.forEach {
            getOrFindEndpoints(it)
        }
    }

    suspend fun getOrFindEndpointIds(sourceMark: GuideMark): List<String> {
        val detectedEndpoints = sourceMark.getUserData(DETECTED_ENDPOINTS)
        return if (detectedEndpoints != null) {
            val cachedEndpointIds = detectedEndpoints.mapNotNull { it.id }
            log.trace("Found cached endpoint ids: $cachedEndpointIds")
            cachedEndpointIds
        } else {
            getOrFindEndpoints(sourceMark)
            val cachedEndpointIds = sourceMark.getUserData(DETECTED_ENDPOINTS)?.mapNotNull { it.id }
            cachedEndpointIds ?: emptyList()
        }
    }

    private suspend fun getOrFindEndpoints(sourceMark: GuideMark) {
        var detectedEndpoints = sourceMark.getUserData(DETECTED_ENDPOINTS)
        if (detectedEndpoints == null) {
            log.trace("Determining endpoint name(s)")
            detectedEndpoints = determineEndpointNames(sourceMark).ifEmpty { return }
            sourceMark.putUserData(DETECTED_ENDPOINTS, detectedEndpoints)

            detectedEndpoints.forEach {
                log.trace("Detected endpoint name: ${it.name}")
                determineEndpointId(it, sourceMark)
            }
        } else {
            detectedEndpoints.forEach {
                if (it.id == null) {
                    determineEndpointId(it, sourceMark)
                }
            }
        }
    }

    private suspend fun determineEndpointId(endpoint: DetectedEndpoint, guideMark: GuideMark) {
        if (endpoint.internal) {
            log.trace("Internal endpoint, skipping endpoint id lookup")
            return
        }
        val service = SourceStatusService.getCurrentService(project)
        if (service == null) {
            log.warn("Could not determine endpoint id for endpoint name: ${endpoint.name}")
            return
        }

        log.trace("Determining endpoint id for endpoint name: ${endpoint.name}")
        val endpoints = UserData.liveManagementService(guideMark.project)
            .searchEndpoints(service.id, endpoint.name, 1000)
        val foundEndpoint = endpoints.await().find { it.name == endpoint.name }
        if (foundEndpoint != null) {
            log.trace("Found endpoint id: ${foundEndpoint.id}")
            endpoint.id = foundEndpoint.id
            guideMark.putUserData(ENDPOINT_FOUND, true)
        } else {
            if (guideMark.getUserData(ENDPOINT_FOUND) == null) {
                guideMark.putUserData(ENDPOINT_FOUND, false)
            }
            log.trace("Could not find endpoint id for: ${endpoint.name}")
        }
    }

    private suspend fun determineEndpointNames(guideMark: GuideMark): List<DetectedEndpoint> {
        detectorSet.forEach {
            val detectedEndpoint = it.detectEndpointNames(guideMark).await()
            if (detectedEndpoint.isNotEmpty()) return detectedEndpoint
        }
        return emptyList()
    }

    /**
     * Endpoint detectors are responsible for determining [name], [internal], [path], and [type]. [id] will be
     * automatically determined once the endpoint is found.
     */
    data class DetectedEndpoint(
        val name: String,
        val internal: Boolean,
        val path: String? = null,
        val type: String? = null,
        var id: String? = null
    )

    /**
     * Provides implementations for determining the endpoint name(s) for a given [GuideMark].
     */
    interface EndpointNameDetector {
        fun detectEndpointNames(guideMark: GuideMark): Future<List<DetectedEndpoint>>
    }

    class AggregateEndpointDetector(
        project: Project,
        endpointDetectors: List<EndpointDetector<*>>
    ) : EndpointDetector<EndpointNameDetector>(project) {
        override val detectorSet = endpointDetectors.flatMap { it.detectorSet }.toSet()
    }
}
