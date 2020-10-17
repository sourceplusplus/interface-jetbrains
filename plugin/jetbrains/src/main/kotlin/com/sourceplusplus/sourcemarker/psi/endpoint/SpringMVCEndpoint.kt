package com.sourceplusplus.sourcemarker.psi.endpoint

import com.intellij.openapi.application.ApplicationManager
import com.sourceplusplus.sourcemarker.psi.EndpointDetector
import io.vertx.core.Future
import io.vertx.core.Promise
import org.jetbrains.plugins.groovy.lang.psi.uast.GrUReferenceExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.expressions.UInjectionHost
import org.jetbrains.uast.kotlin.KotlinAbstractUExpression
import org.jetbrains.uast.kotlin.KotlinUAnnotation
import org.jetbrains.uast.kotlin.KotlinUQualifiedReferenceExpression
import org.jetbrains.uast.kotlin.expressions.KotlinUCollectionLiteralExpression
import java.util.*

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SpringMVCEndpoint : EndpointDetector.EndpointNameDeterminer {

    private val requestMappingAnnotation = "org.springframework.web.bind.annotation.RequestMapping"
    private val qualifiedNameSet = setOf(
        requestMappingAnnotation,
        "org.springframework.web.bind.annotation.GetMapping",
        "org.springframework.web.bind.annotation.PostMapping",
        "org.springframework.web.bind.annotation.PutMapping",
        "org.springframework.web.bind.annotation.DeleteMapping",
        "org.springframework.web.bind.annotation.PatchMapping"
    )

    override fun determineEndpointName(uMethod: UMethod): Future<Optional<String>> {
        val promise = Promise.promise<Optional<String>>()
        ApplicationManager.getApplication().runReadAction {
            for (annotationName in qualifiedNameSet) {
                val annotation = uMethod.findAnnotation(annotationName)
                if (annotation != null) {
                    if (annotationName == requestMappingAnnotation) {
                        val endpointNameExpr = annotation.attributeValues.find { it.name == "value" }!!.expression
                        val methodExpr = annotation.attributeValues.find { it.name == "method" }!!.expression
                        if (endpointNameExpr is KotlinAbstractUExpression) {
                            val value = if (endpointNameExpr is KotlinUCollectionLiteralExpression) {
                                endpointNameExpr.valueArguments[0].evaluate()
                            } else {
                                endpointNameExpr.evaluate()
                            }
                            val method =
                                ((methodExpr as KotlinUCollectionLiteralExpression)
                                    .valueArguments[0] as KotlinUQualifiedReferenceExpression)
                                    .selector.asSourceString()
                            promise.complete(Optional.of("{$method}$value"))
                        } else {
                            val value = (endpointNameExpr as UInjectionHost).evaluateToString()
                            val method = if (methodExpr is UQualifiedReferenceExpression) {
                                methodExpr.selector.toString()
                            } else {
                                (methodExpr as GrUReferenceExpression).resolvedName.toString()
                            }
                            promise.complete(Optional.of("{$method}$value"))
                        }
                    } else {
                        if (annotation is KotlinUAnnotation) {
                            val valueExpr = annotation.findAttributeValue("value")!!
                            val value = if (valueExpr is KotlinUCollectionLiteralExpression) {
                                valueExpr.valueArguments[0].evaluate()
                            } else {
                                valueExpr.evaluate()
                            }
                            val method = annotationName.substring(annotationName.lastIndexOf(".") + 1)
                                .replace("Mapping", "").toUpperCase()
                            promise.complete(Optional.of("{$method}$value"))
                        } else {
                            val endpointNameExpr = annotation.attributeValues.find { it.name == "name" }!!
                            val value = if (endpointNameExpr is UInjectionHost) {
                                endpointNameExpr.evaluateToString()
                            } else {
                                endpointNameExpr.evaluate()
                            } as String
                            val method = annotationName.substring(annotationName.lastIndexOf(".") + 1)
                                .replace("Mapping", "").toUpperCase()
                            promise.complete(Optional.of("{$method}$value"))
                        }
                    }
                }
            }

            promise.tryComplete(Optional.empty())
        }
        return promise.future()
    }
}
