package com.sourceplusplus.marker.jvm.psi.endpoint

import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.sourceplusplus.marker.jvm.psi.EndpointDetector
import com.sourceplusplus.marker.jvm.psi.EndpointDetector.DetectedEndpoint
import io.vertx.core.Future
import io.vertx.core.Promise
import org.jetbrains.plugins.groovy.lang.psi.uast.GrUReferenceExpression
import org.jetbrains.uast.*
import org.jetbrains.uast.expressions.UInjectionHost
import org.jetbrains.uast.java.JavaUSimpleNameReferenceExpression
import org.jetbrains.uast.kotlin.KotlinStringULiteralExpression
import org.jetbrains.uast.kotlin.KotlinUQualifiedReferenceExpression
import org.jetbrains.uast.kotlin.KotlinUSimpleReferenceExpression
import org.jetbrains.uast.kotlin.expressions.KotlinUCollectionLiteralExpression
import java.util.*

/**
 * todo: description.
 *
 * @since 0.1.0
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

    override fun determineEndpointName(uMethod: UMethod): Future<Optional<DetectedEndpoint>> {
        val promise = Promise.promise<Optional<DetectedEndpoint>>()
        ApplicationManager.getApplication().runReadAction {
            for (annotationName in qualifiedNameSet) {
                val annotation = uMethod.findAnnotation(annotationName)
                if (annotation != null) {
                    when {
                        annotation.lang === Language.findLanguageByID("JAVA")
                                || annotation.lang === Language.findLanguageByID("Groovy") -> {
                            val detectedEndpoint = handleJavaOrGroovyAnnotation(false, annotation, annotationName)
                            val classRequestMapping = uMethod.containingClass?.toUElementOfType<UClass>()
                                ?.findAnnotation(requestMappingAnnotation)
                            if (classRequestMapping != null) {
                                val classEndpoint = handleJavaOrGroovyAnnotation(
                                    true, classRequestMapping, requestMappingAnnotation
                                )
                                handleEndpointDetection(detectedEndpoint, classEndpoint, promise)
                            } else {
                                promise.complete(detectedEndpoint)
                            }
                        }
                        annotation.lang === Language.findLanguageByID("kotlin") -> {
                            val detectedEndpoint = handleKotlinAnnotation(false, annotation, annotationName)
                            val classRequestMapping = uMethod.containingClass?.toUElementOfType<UClass>()
                                ?.findAnnotation(requestMappingAnnotation)
                            if (classRequestMapping != null) {
                                val classEndpoint = handleKotlinAnnotation(
                                    true, classRequestMapping, requestMappingAnnotation
                                )
                                handleEndpointDetection(detectedEndpoint, classEndpoint, promise)
                            } else {
                                promise.complete(detectedEndpoint)
                            }
                        }
                        else -> throw UnsupportedOperationException(
                            "Language ${annotation.lang} is not currently supported"
                        )
                    }
                }
            }

            promise.tryComplete(Optional.empty())
        }
        return promise.future()
    }

    private fun handleEndpointDetection(
        detectedEndpoint: Optional<DetectedEndpoint>,
        classEndpoint: Optional<DetectedEndpoint>,
        promise: Promise<Optional<DetectedEndpoint>>
    ) {
        if (detectedEndpoint.isPresent && classEndpoint.isPresent) {
            var classEndpointPath = classEndpoint.get().path!!
            if (classEndpointPath.endsWith("/")) {
                classEndpointPath = classEndpointPath.substringBeforeLast("/")
            }

            val endpointName = buildString {
                append("{")
                append(detectedEndpoint.get().type)
                append("}")
                append(classEndpointPath)
                append(detectedEndpoint.get().path)
            }
            promise.complete(Optional.of(DetectedEndpoint(endpointName, false)))
        } else {
            promise.complete(detectedEndpoint)
        }
    }

    private fun handleJavaOrGroovyAnnotation(
        isClass: Boolean,
        annotation: UAnnotation,
        annotationName: String
    ): Optional<DetectedEndpoint> {
        if (annotationName == requestMappingAnnotation) {
            var endpointNameExpr = annotation.attributeValues.find { it.name == "value" }?.expression
            if (endpointNameExpr == null) {
                endpointNameExpr = annotation.attributeValues.find { it.name == "path" }?.expression
            }
            if (endpointNameExpr == null) {
                endpointNameExpr = annotation.attributeValues.find { it.name == null }?.expression
            }
            if (isClass) {
                val value = if (endpointNameExpr == null) {
                    "/"
                } else (endpointNameExpr as UInjectionHost).evaluateToString()
                return Optional.of(DetectedEndpoint(value.toString(), false, value.toString()))
            }

            val methodExpr = annotation.attributeValues.find { it.name == "method" }!!.expression
            val value = if (endpointNameExpr == null) "" else (endpointNameExpr as UInjectionHost).evaluateToString()
            val method = if (methodExpr is UQualifiedReferenceExpression) {
                methodExpr.selector.toString()
            } else {
                when (methodExpr) {
                    is JavaUSimpleNameReferenceExpression -> methodExpr.resolvedName.toString()
                    is GrUReferenceExpression -> methodExpr.resolvedName.toString()
                    else -> throw UnsupportedOperationException(methodExpr.javaClass.name)
                }
            }
            return Optional.of(DetectedEndpoint("{$method}$value", false, value, method))
        } else {
            var endpointNameExpr = annotation.attributeValues.find { it.name == "value" }
            if (endpointNameExpr == null) endpointNameExpr = annotation.attributeValues.find { it.name == "path" }
            val value = if (endpointNameExpr is UInjectionHost) {
                endpointNameExpr.evaluateToString()
            } else if (endpointNameExpr != null) {
                endpointNameExpr.evaluate()
            } else {
                "/"
            } as String
            val method = annotationName.substring(annotationName.lastIndexOf(".") + 1)
                .replace("Mapping", "").toUpperCase()
            return Optional.of(DetectedEndpoint("{$method}$value", false, value.toString(), method))
        }
    }

    private fun handleKotlinAnnotation(
        isClass: Boolean,
        annotation: UAnnotation,
        annotationName: String
    ): Optional<DetectedEndpoint> {
        if (annotationName == requestMappingAnnotation) {
            var endpointNameExpr = annotation.attributeValues.find { it.name == "value" }?.expression
            if (endpointNameExpr == null) {
                endpointNameExpr = annotation.attributeValues.find { it.name == "path" }?.expression
            }
            if (endpointNameExpr == null) {
                endpointNameExpr = annotation.attributeValues.find { it.name == null }?.expression
            }
            if (isClass) {
                val value = if (endpointNameExpr == null) {
                    "/"
                } else (endpointNameExpr as KotlinStringULiteralExpression).value
                return Optional.of(DetectedEndpoint(value, false, value))
            }

            val methodExpr = annotation.attributeValues.find { it.name == "method" }!!.expression
            val value = if (endpointNameExpr is KotlinUCollectionLiteralExpression) {
                endpointNameExpr!!.valueArguments[0].evaluate()
            } else {
                endpointNameExpr?.evaluate() ?: ""
            }
            val valueArg = (methodExpr as KotlinUCollectionLiteralExpression).valueArguments[0]
            val method = if (valueArg is KotlinUSimpleReferenceExpression) {
                valueArg.resolvedName
            } else if (valueArg is KotlinUQualifiedReferenceExpression) {
                valueArg.selector.asSourceString()
            } else {
                throw UnsupportedOperationException(valueArg.javaClass.name)
            }
            return Optional.of(DetectedEndpoint("{$method}$value", false, value.toString(), method))
        } else {
            var valueExpr = annotation.findAttributeValue("value")
            if (valueExpr == null) valueExpr = annotation.findAttributeValue("path")
            val value = if (valueExpr is KotlinUCollectionLiteralExpression) {
                valueExpr.valueArguments[0].evaluate()
            } else if (valueExpr != null) {
                valueExpr.evaluate()
            } else {
                "/"
            }
            val method = annotationName.substring(annotationName.lastIndexOf(".") + 1)
                .replace("Mapping", "").toUpperCase()
            return Optional.of(DetectedEndpoint("{$method}$value", false, value.toString(), method))
        }
    }
}
