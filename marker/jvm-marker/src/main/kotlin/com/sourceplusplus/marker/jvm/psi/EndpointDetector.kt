package com.sourceplusplus.marker.jvm.psi

import com.sourceplusplus.marker.source.mark.api.MethodSourceMark
import com.sourceplusplus.marker.source.mark.api.SourceMark
import com.sourceplusplus.marker.source.mark.api.key.SourceKey
import com.sourceplusplus.monitor.skywalking.bridge.EndpointBridge
import com.sourceplusplus.marker.jvm.psi.endpoint.SkywalkingTraceEndpoint
import com.sourceplusplus.marker.jvm.psi.endpoint.SpringMVCEndpoint
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.eventbus.ReplyException
import io.vertx.core.eventbus.ReplyFailure
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement
import org.slf4j.LoggerFactory
import java.util.*

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class EndpointDetector(val vertx: Vertx) {

    companion object {
        private val log = LoggerFactory.getLogger(EndpointDetector::class.java)
        private val ENDPOINT_ID = SourceKey<String>("ENDPOINT_ID")
        private val ENDPOINT_NAME = SourceKey<String>("ENDPOINT_NAME")
        private val ENDPOINT_INTERNAL = SourceKey<Boolean>("ENDPOINT_INTERNAL")
    }

    private val detectorSet = setOf(
        SkywalkingTraceEndpoint(),
        SpringMVCEndpoint()
    )

    fun getEndpointName(sourceMark: SourceMark): String? {
        return sourceMark.getUserData(ENDPOINT_NAME)
    }

    fun getEndpointId(sourceMark: SourceMark): String? {
        return sourceMark.getUserData(ENDPOINT_ID)
    }

    fun isExternalEndpoint(sourceMark: SourceMark): Boolean {
        return sourceMark.getUserData(ENDPOINT_INTERNAL) == false
    }

    suspend fun getOrFindEndpointId(sourceMark: SourceMark): String? {
        val cachedEndpointId = sourceMark.getUserData(ENDPOINT_ID)
        return if (cachedEndpointId != null) {
            log.trace("Found cached endpoint id: $cachedEndpointId")
            cachedEndpointId
        } else if (sourceMark is MethodSourceMark) {
            getOrFindEndpoint(sourceMark)
            sourceMark.getUserData(ENDPOINT_ID)
        } else {
            null
        }
    }

    suspend fun getOrFindEndpointName(sourceMark: SourceMark): String? {
        val cachedEndpointName = sourceMark.getUserData(ENDPOINT_NAME)
        return if (cachedEndpointName != null) {
            log.trace("Found cached endpoint name: $cachedEndpointName")
            cachedEndpointName
        } else if (sourceMark is MethodSourceMark) {
            getOrFindEndpoint(sourceMark)
            sourceMark.getUserData(ENDPOINT_NAME)
        } else {
            null
        }
    }

    private suspend fun getOrFindEndpoint(sourceMark: MethodSourceMark) {
        if (sourceMark.getUserData(ENDPOINT_NAME) == null || sourceMark.getUserData(ENDPOINT_ID) == null) {
            if (sourceMark.getUserData(ENDPOINT_NAME) == null) {
                log.trace("Determining endpoint name")
                val detectedEndpoint = determineEndpointName(sourceMark).await().orElse(null)
                if (detectedEndpoint != null) {
                    log.debug("Detected endpoint name: ${detectedEndpoint.name}")
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

    private suspend fun determineEndpointId(endpointName: String, sourceMark: MethodSourceMark) {
        log.trace("Determining endpoint id")
        try {
            val endpoint = EndpointBridge.searchExactEndpoint(endpointName, vertx)
            if (endpoint != null) {
                sourceMark.putUserData(ENDPOINT_ID, endpoint.id)
                log.debug("Detected endpoint id: ${endpoint.id}")
            } else {
                log.debug("Could not find endpoint id for: $endpointName")
            }
        } catch (ex: ReplyException) {
            if (ex.failureType() == ReplyFailure.TIMEOUT) {
                log.debug("Timed out looking for endpoint id for: $endpointName")
            } else {
                throw ex
            }
        }
    }

    private fun determineEndpointName(sourceMark: MethodSourceMark): Future<Optional<DetectedEndpoint>> {
        return determineEndpointName(sourceMark.getPsiMethod().toUElement() as UMethod)
    }

    fun determineEndpointName(uMethod: UMethod): Future<Optional<DetectedEndpoint>> {
        val promise = Promise.promise<Optional<DetectedEndpoint>>()
        GlobalScope.launch(vertx.dispatcher()) {
            detectorSet.forEach {
                try {
                    val detectedEndpoint = it.determineEndpointName(uMethod).await()
                    if (detectedEndpoint.isPresent) promise.complete(detectedEndpoint)
                } catch (throwable: Throwable) {
                    promise.fail(throwable)
                }
            }
            promise.tryComplete(Optional.empty())
        }
        return promise.future()
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
        fun determineEndpointName(uMethod: UMethod): Future<Optional<DetectedEndpoint>>
    }
}
