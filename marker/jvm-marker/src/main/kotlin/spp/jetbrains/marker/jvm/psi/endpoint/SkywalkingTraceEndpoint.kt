package spp.jetbrains.marker.jvm.psi.endpoint

import com.intellij.openapi.application.ApplicationManager
import spp.jetbrains.marker.jvm.psi.EndpointDetector
import spp.jetbrains.marker.jvm.psi.EndpointDetector.DetectedEndpoint
import io.vertx.core.Future
import io.vertx.core.Promise
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.expressions.UInjectionHost
import java.util.*

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SkywalkingTraceEndpoint : EndpointDetector.EndpointNameDeterminer {

    private val skywalkingTraceAnnotation = "org.apache.skywalking.apm.toolkit.trace.Trace"

    override fun determineEndpointName(uMethod: UMethod): Future<Optional<DetectedEndpoint>> {
        val promise = Promise.promise<Optional<DetectedEndpoint>>()
        ApplicationManager.getApplication().runReadAction {
            val annotation = uMethod.findAnnotation(skywalkingTraceAnnotation)
            if (annotation != null) {
                val operationNameExpr = annotation.attributeValues.find { it.name == "operationName" }
                val value = if (operationNameExpr is UInjectionHost) {
                    operationNameExpr.evaluateToString()
                } else {
                    operationNameExpr?.evaluate()
                } as String?
                if (value == null || value == "") {
                    promise.complete(
                        Optional.of(
                            DetectedEndpoint("${uMethod.containingClass!!.qualifiedName}.${uMethod.name}", true)
                        )
                    )
                } else {
                    promise.complete(Optional.of(DetectedEndpoint(value, true)))
                }
            }

            promise.tryComplete(Optional.empty())
        }
        return promise.future()
    }
}
