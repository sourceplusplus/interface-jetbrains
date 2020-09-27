package com.sourceplusplus.sourcemarker.psi

import com.sourceplusplus.marker.source.mark.api.MethodSourceMark
import com.sourceplusplus.sourcemarker.activities.PluginSourceMarkerStartupActivity.Companion.vertx
import com.sourceplusplus.sourcemarker.psi.endpoint.SkywalkingTrace
import com.sourceplusplus.sourcemarker.psi.endpoint.SpringMVC
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.uast.UMethod
import java.util.*

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class EndpointNameDetector {

    private val detectorSet = setOf(
        SkywalkingTrace(),
        SpringMVC()
    )

    fun determineEndpointName(sourceMark: MethodSourceMark): Future<Optional<String>> {
        return determineEndpointName(sourceMark.getPsiMethod())
    }

    fun determineEndpointName(uMethod: UMethod): Future<Optional<String>> {
        val promise = Promise.promise<Optional<String>>()
        GlobalScope.launch(vertx.dispatcher()) {
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
