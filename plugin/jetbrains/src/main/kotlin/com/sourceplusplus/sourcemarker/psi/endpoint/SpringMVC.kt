package com.sourceplusplus.sourcemarker.psi.endpoint

import com.intellij.openapi.application.ApplicationManager
import com.sourceplusplus.sourcemarker.psi.EndpointNameDetector
import io.vertx.core.Future
import io.vertx.core.Promise
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.expressions.UInjectionHost
import org.jetbrains.uast.java.JavaUQualifiedReferenceExpression
import java.util.*

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SpringMVC : EndpointNameDetector.EndpointNameDeterminer {

    private val requestMappingAnnotation = "org.springframework.web.bind.annotation.RequestMapping"
    private val qualifiedNameSet = setOf(
        "org.springframework.web.bind.annotation.GetMapping",
        "org.springframework.web.bind.annotation.PostMapping",
        "org.springframework.web.bind.annotation.PutMapping",
        "org.springframework.web.bind.annotation.DeleteMapping",
        "org.springframework.web.bind.annotation.PatchMapping",
        requestMappingAnnotation
    )

    override fun determineEndpointName(uMethod: UMethod): Future<Optional<String>> {
        val promise = Promise.promise<Optional<String>>()
        ApplicationManager.getApplication().runReadAction {
            for (annotationName in qualifiedNameSet) {
                val annotation = uMethod.findAnnotation(annotationName)
                if (annotation != null) {
                    if (annotationName == requestMappingAnnotation) {
                        val endpointNameExpr = annotation.findAttributeValue("value")
                        val value = (endpointNameExpr as UInjectionHost).evaluateToString()
                        val method = (annotation.findAttributeValue("method")
                                as JavaUQualifiedReferenceExpression).selector
                        promise.complete(Optional.of("{$method}$value"))
                    } else {
                        val endpointNameExpr = annotation.findAttributeValue("name")
                        val value = (endpointNameExpr as UInjectionHost).evaluateToString()
                        val method = annotationName.substring(annotationName.lastIndexOf(".") + 1)
                            .replace("Mapping", "").toUpperCase()
                        promise.complete(Optional.of("{$method}$value"))
                    }
                }
            }

            promise.tryComplete(Optional.empty())
        }
        return promise.future()
    }
}
