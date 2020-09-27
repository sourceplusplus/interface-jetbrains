package com.sourceplusplus.sourcemarker.psi.endpoint

import com.intellij.openapi.application.ApplicationManager
import com.sourceplusplus.sourcemarker.psi.EndpointNameDetector
import io.vertx.core.Future
import io.vertx.core.Promise
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.expressions.UInjectionHost
import java.util.*

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SkywalkingTrace : EndpointNameDetector.EndpointNameDeterminer {

    private val skywalkingTraceAnnotation = "org.apache.skywalking.apm.toolkit.trace.Trace"

    override fun determineEndpointName(uMethod: UMethod): Future<Optional<String>> {
        val promise = Promise.promise<Optional<String>>()
        ApplicationManager.getApplication().runReadAction {
            val annotation = uMethod.findAnnotation(skywalkingTraceAnnotation)
            if (annotation != null) {
                val operationNameExpr = annotation.findAttributeValue("operationName")
                val value = (operationNameExpr as UInjectionHost?)?.evaluateToString()
                if (value == null || value == "") {
                    promise.complete(Optional.of("${uMethod.containingClass!!.qualifiedName}.${uMethod.name}"))
                } else {
                    promise.complete(Optional.of(value))
                }
            }

            promise.tryComplete(Optional.empty())
        }
        return promise.future()
    }
}
