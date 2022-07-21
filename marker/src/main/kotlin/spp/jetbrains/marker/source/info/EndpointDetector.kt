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
package spp.jetbrains.marker.source.info

import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.eventbus.ReplyException
import io.vertx.core.eventbus.ReplyFailure
import io.vertx.kotlin.coroutines.await
import org.slf4j.LoggerFactory
import spp.jetbrains.marker.source.mark.api.key.SourceKey
import spp.jetbrains.marker.source.mark.guide.GuideMark
import spp.jetbrains.marker.source.mark.guide.MethodGuideMark
import spp.jetbrains.monitor.skywalking.bridge.EndpointBridge
import java.util.*

/**
 * todo: description.
 *
 * @since 0.5.5
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
abstract class EndpointDetector<T : EndpointDetector.EndpointNameDeterminer>(val vertx: Vertx) {

    companion object {
        private val log = LoggerFactory.getLogger(EndpointDetector::class.java)
        val ENDPOINT_ID = SourceKey<String>("ENDPOINT_ID")
        val ENDPOINT_NAME = SourceKey<String>("ENDPOINT_NAME")
        val ENDPOINT_INTERNAL = SourceKey<Boolean>("ENDPOINT_INTERNAL")
    }

    abstract val detectorSet: Set<T>

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

    private suspend fun determineEndpointId(endpointName: String, sourceMark: MethodGuideMark) {
        log.trace("Determining endpoint id")
        try {
            val endpoint = EndpointBridge.searchExactEndpoint(endpointName, vertx)
            if (endpoint != null) {
                sourceMark.putUserData(ENDPOINT_ID, endpoint.getString("id"))
                log.trace("Detected endpoint id: ${endpoint.getString("id")}")
            } else {
                log.trace("Could not find endpoint id for: $endpointName")
            }
        } catch (ex: ReplyException) {
            if (ex.failureType() == ReplyFailure.TIMEOUT) {
                log.debug("Timed out looking for endpoint id for: $endpointName")
            } else {
                throw ex
            }
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
