package com.sourceplusplus.sourcemarker.psi

import com.sourceplusplus.marker.source.mark.api.MethodSourceMark
import com.sourceplusplus.marker.source.mark.api.key.SourceKey
import com.sourceplusplus.monitor.skywalking.track.EndpointTracker
import com.sourceplusplus.sourcemarker.SourceMarkerPlugin
import com.sourceplusplus.sourcemarker.psi.endpoint.SkywalkingTraceEndpoint
import com.sourceplusplus.sourcemarker.psi.endpoint.SpringMVCEndpoint
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.uast.UMethod
import org.slf4j.LoggerFactory
import java.util.*

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class EndpointDetector {

    companion object {
        private val log = LoggerFactory.getLogger(EndpointDetector::class.java)
        private val ENDPOINT_ID = SourceKey<String>("ENDPOINT_ID")
        private val ENDPOINT_NAME = SourceKey<String>("ENDPOINT_NAME")
    }

    private val detectorSet = setOf(
        SkywalkingTraceEndpoint(),
        SpringMVCEndpoint()
    )

    suspend fun getOrFindEndpointId(sourceMark: MethodSourceMark): String? {
        val cachedEndpointId = sourceMark.getUserData(ENDPOINT_ID)
        return if (cachedEndpointId != null) {
            log.debug("Found cached endpoint id: $cachedEndpointId")
            cachedEndpointId
        } else {
            getOrFindEndpoint(sourceMark)
            sourceMark.getUserData(ENDPOINT_ID)
        }
    }

    suspend fun getOrFindEndpointName(sourceMark: MethodSourceMark): String? {
        val cachedEndpointName = sourceMark.getUserData(ENDPOINT_NAME)
        return if (cachedEndpointName != null) {
            log.debug("Found cached endpoint name: $cachedEndpointName")
            cachedEndpointName
        } else {
            getOrFindEndpoint(sourceMark)
            sourceMark.getUserData(ENDPOINT_NAME)
        }
    }

    private suspend fun getOrFindEndpoint(sourceMark: MethodSourceMark) {
        if (sourceMark.getUserData(ENDPOINT_NAME) == null || sourceMark.getUserData(ENDPOINT_ID) == null) {
            if (sourceMark.getUserData(ENDPOINT_NAME) == null) {
                log.debug("Determining endpoint name")
                val endpointName = determineEndpointName(sourceMark).await().orElse(null)
                if (endpointName != null) {
                    log.debug("Detected endpoint name: $endpointName")
                    sourceMark.putUserData(ENDPOINT_NAME, endpointName)

                    determineEndpointId(endpointName, sourceMark)
                }
            } else {
                determineEndpointId(sourceMark.getUserData(ENDPOINT_NAME)!!, sourceMark)
            }
        }
    }

    private suspend fun determineEndpointId(endpointName: String, sourceMark: MethodSourceMark) {
        log.debug("Determining endpoint id")
        val endpoint = EndpointTracker.searchExactEndpoint(endpointName, SourceMarkerPlugin.vertx)
        if (endpoint != null) {
            sourceMark.putUserData(ENDPOINT_ID, endpoint.id)
            log.debug("Detected endpoint id: ${endpoint.id}")
        } else {
            log.debug("Could not find endpoint id for: $endpointName")
        }
    }

    private fun determineEndpointName(sourceMark: MethodSourceMark): Future<Optional<String>> {
        return determineEndpointName(sourceMark.getPsiMethod())
    }

    fun determineEndpointName(uMethod: UMethod): Future<Optional<String>> {
        val promise = Promise.promise<Optional<String>>()
        GlobalScope.launch(SourceMarkerPlugin.vertx.dispatcher()) {
            detectorSet.forEach {
                try {
                    val endpointName = it.determineEndpointName(uMethod).await()
                    if (endpointName.isPresent) promise.complete(endpointName)
                } catch (throwable: Throwable) {
                    promise.fail(throwable)
                }
            }
            promise.tryComplete(Optional.empty())
        }
        return promise.future()
    }

    interface EndpointNameDeterminer {
        fun determineEndpointName(uMethod: UMethod): Future<Optional<String>>
    }
}
