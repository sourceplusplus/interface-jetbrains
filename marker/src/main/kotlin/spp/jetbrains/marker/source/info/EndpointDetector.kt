/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
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

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import io.vertx.core.Future
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import spp.jetbrains.UserData
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.source.mark.api.key.SourceKey
import spp.jetbrains.marker.source.mark.guide.GuideMark
import spp.jetbrains.marker.source.mark.guide.MethodGuideMark
import spp.jetbrains.monitor.skywalking.SkywalkingMonitorService
import java.util.*

/**
 * todo: description.
 *
 * @since 0.5.5
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
abstract class EndpointDetector<T : EndpointDetector.EndpointNameDeterminer>(val project: Project) {

    companion object {
        private val log = logger<EndpointDetector<*>>()
        val ENDPOINT_ID = SourceKey<String>("ENDPOINT_ID")
        val ENDPOINT_NAME = SourceKey<String>("ENDPOINT_NAME")
        val ENDPOINT_INTERNAL = SourceKey<Boolean>("ENDPOINT_INTERNAL")
        val ENDPOINT_FOUND = SourceKey<Boolean>("ENDPOINT_FOUND")
        private val REDETECTOR_SETUP = Key.create<Boolean>("SPP_REDETECTOR_SETUP")
    }

    abstract val detectorSet: Set<T>
    private val skywalkingMonitor by lazy { project.getUserData(SkywalkingMonitorService.KEY)!! }

    init {
        if (project.getUserData(REDETECTOR_SETUP) != true) {
            project.putUserData(REDETECTOR_SETUP, true)
            log.info("Setting up endpoint re-detector for project ${project.name}")

            val vertx = UserData.vertx(project)
            vertx.setPeriodic(5000) {
                GlobalScope.launch(vertx.dispatcher()) {
                    try {
                        redetectEndpoints()
                    } catch (e: Exception) {
                        log.error("Error detecting endpoints", e)
                    }
                }
            }
        }
    }

    private suspend fun redetectEndpoints() {
        log.debug("Redetecting endpoints for project ${project.name}")
        SourceMarker.getInstance(project).getSourceMarks().forEach {
            if (it is MethodGuideMark && it.getUserData(ENDPOINT_FOUND) == false) {
                getOrFindEndpoint(it)
            }
        }
    }

    fun getEndpointName(sourceMark: GuideMark): String? {
        return sourceMark.getUserData(ENDPOINT_NAME)
    }

    fun getEndpointId(sourceMark: GuideMark): String? {
        return sourceMark.getUserData(ENDPOINT_ID)
    }

    fun isExternalEndpoint(sourceMark: GuideMark): Boolean {
        return sourceMark.getUserData(ENDPOINT_INTERNAL) == false
    }

    suspend fun getOrFindEndpointId(sourceMark: GuideMark): String? {
        val cachedEndpointId = sourceMark.getUserData(ENDPOINT_ID)
        return if (cachedEndpointId != null) {
            log.trace("Found cached endpoint id: $cachedEndpointId")
            cachedEndpointId
        } else if (sourceMark is MethodGuideMark) {
            getOrFindEndpoint(sourceMark)
            sourceMark.getUserData(ENDPOINT_ID)
        } else {
            null
        }
    }

    suspend fun getOrFindEndpointName(sourceMark: GuideMark): String? {
        val cachedEndpointName = sourceMark.getUserData(ENDPOINT_NAME)
        return if (cachedEndpointName != null) {
            log.trace("Found cached endpoint name: $cachedEndpointName")
            cachedEndpointName
        } else if (sourceMark is MethodGuideMark) {
            getOrFindEndpoint(sourceMark)
            sourceMark.getUserData(ENDPOINT_NAME)
        } else {
            null
        }
    }

    private suspend fun getOrFindEndpoint(sourceMark: MethodGuideMark) {
        if (sourceMark.getUserData(ENDPOINT_NAME) == null || sourceMark.getUserData(ENDPOINT_ID) == null) {
            if (sourceMark.getUserData(ENDPOINT_NAME) == null) {
                log.trace("Determining endpoint name")
                val detectedEndpoint = determineEndpointName(sourceMark)
                if (detectedEndpoint != null) {
                    log.trace("Detected endpoint name: ${detectedEndpoint.name}")
                    sourceMark.putUserData(ENDPOINT_NAME, detectedEndpoint.name)
                    sourceMark.putUserData(ENDPOINT_INTERNAL, detectedEndpoint.internal)

                    determineEndpointId(detectedEndpoint.name, sourceMark)
                } else {
                    log.trace("Could not find endpoint name for: ${sourceMark.artifactQualifiedName}")
                }
            } else {
                determineEndpointId(sourceMark.getUserData(ENDPOINT_NAME)!!, sourceMark)
            }
        }
    }

    private suspend fun determineEndpointId(endpointName: String, guideMark: MethodGuideMark) {
        if (guideMark.getUserData(ENDPOINT_INTERNAL) == true) {
            log.trace("Internal endpoint, skipping endpoint id lookup")
            return
        }

        log.trace("Determining endpoint id for endpoint name: $endpointName")
        val endpoint = skywalkingMonitor.searchExactEndpoint(endpointName)
        if (endpoint != null) {
            guideMark.putUserData(ENDPOINT_ID, endpoint.getString("id"))
            guideMark.putUserData(ENDPOINT_FOUND, true)
            log.trace("Detected endpoint id: ${endpoint.getString("id")}")
        } else {
            if (guideMark.getUserData(ENDPOINT_FOUND) != false) {
                guideMark.putUserData(ENDPOINT_FOUND, false)
            }
            log.trace("Could not find endpoint id for: $endpointName")
        }
    }

    private suspend fun determineEndpointName(guideMark: MethodGuideMark): DetectedEndpoint? {
        detectorSet.forEach {
            val detectedEndpoint = it.determineEndpointName(guideMark).await()
            if (detectedEndpoint.isPresent) return detectedEndpoint.get()
        }
        return null
    }

    /**
     * todo: description.
     */
    data class DetectedEndpoint(
        val name: String,
        val internal: Boolean,
        val path: String? = null,
        val type: String? = null,
    )

    /**
     * todo: description.
     */
    interface EndpointNameDeterminer {
        fun determineEndpointName(guideMark: MethodGuideMark): Future<Optional<DetectedEndpoint>>
    }
}
