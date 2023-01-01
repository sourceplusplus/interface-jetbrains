/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2023 CodeBrig, Inc.
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
package spp.jetbrains.marker.jvm.detect.endpoint

import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import io.vertx.core.Future
import io.vertx.core.Promise
import org.jetbrains.plugins.groovy.lang.psi.uast.GrUReferenceExpression
import org.jetbrains.uast.*
import org.jetbrains.uast.expressions.UInjectionHost
import org.jetbrains.uast.java.JavaUSimpleNameReferenceExpression
import org.jetbrains.uast.kotlin.KotlinStringULiteralExpression
import org.jetbrains.uast.kotlin.KotlinUQualifiedReferenceExpression
import org.jetbrains.uast.kotlin.KotlinUSimpleReferenceExpression
import org.joor.Reflect
import spp.jetbrains.marker.jvm.detect.JVMEndpointDetector.JVMEndpointNameDetector
import spp.jetbrains.marker.source.info.EndpointDetector
import spp.jetbrains.marker.source.info.EndpointDetector.DetectedEndpoint
import spp.jetbrains.marker.source.mark.guide.GuideMark

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SpringMVCEndpoint : JVMEndpointNameDetector {

    private val requestMappingAnnotation = "org.springframework.web.bind.annotation.RequestMapping"
    private val qualifiedNameSet = setOf(
        requestMappingAnnotation,
        "org.springframework.web.bind.annotation.GetMapping",
        "org.springframework.web.bind.annotation.PostMapping",
        "org.springframework.web.bind.annotation.PutMapping",
        "org.springframework.web.bind.annotation.DeleteMapping",
        "org.springframework.web.bind.annotation.PatchMapping"
    )

    override fun detectEndpointNames(guideMark: GuideMark): Future<List<DetectedEndpoint>> {
        if (!guideMark.isMethodMark) {
            return Future.succeededFuture(emptyList())
        }

        return ApplicationManager.getApplication().runReadAction(Computable {
            val uMethod = guideMark.getPsiElement().toUElementOfType<UMethod>()
                ?: return@Computable Future.succeededFuture(emptyList())
            determineEndpointName(uMethod)
        })
    }

    override fun determineEndpointName(uMethod: UMethod): Future<List<DetectedEndpoint>> {
        val promise = Promise.promise<List<DetectedEndpoint>>()
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

                        annotation.lang === Language.findLanguageByID("Scala") -> {
                            val detectedEndpoint = handleScalaAnnotation(false, annotation, annotationName)
                            val classRequestMapping = uMethod.containingClass?.toUElementOfType<UClass>()
                                ?.findAnnotation(requestMappingAnnotation)
                            if (classRequestMapping != null) {
                                val classEndpoint = handleScalaAnnotation(
                                    true, classRequestMapping, requestMappingAnnotation
                                )
                                handleEndpointDetection(detectedEndpoint, classEndpoint, promise)
                            } else {
                                promise.complete(detectedEndpoint)
                            }
                        }

                        else -> throw UnsupportedOperationException(
                            "Language ${annotation.lang.displayName} is not currently supported"
                        )
                    }
                }
            }

            promise.tryComplete(emptyList())
        }
        return promise.future()
    }

    private fun handleEndpointDetection(
        detectedEndpoints: List<DetectedEndpoint>,
        classEndpoints: List<DetectedEndpoint>,
        promise: Promise<List<DetectedEndpoint>>
    ) {
        if (detectedEndpoints.isNotEmpty() && classEndpoints.isNotEmpty()) {
            val combinedEndpoints = mutableListOf<DetectedEndpoint>()
            classEndpoints.forEach { classEndpoint ->
                detectedEndpoints.forEach { endpoint ->
                    var classEndpointPath = classEndpoint.path!!
                    if (classEndpointPath.endsWith("/")) {
                        classEndpointPath = classEndpointPath.substringBeforeLast("/")
                    }

                    val endpointName = buildString {
                        append(endpoint.type)
                        append(":")
                        append(classEndpointPath)
                        if (endpoint.path != "/") {
                            append(endpoint.path)
                        }
                    }
                    combinedEndpoints.add(DetectedEndpoint(endpointName, false))
                }
            }
            promise.complete(combinedEndpoints)
        } else {
            promise.complete(detectedEndpoints)
        }
    }

    private fun handleJavaOrGroovyAnnotation(
        isClass: Boolean,
        annotation: UAnnotation,
        annotationName: String
    ): List<DetectedEndpoint> {
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
                return listOf(DetectedEndpoint(value.toString(), false, value.toString()))
            }

            val methodExpr = annotation.attributeValues.find { it.name == "method" }?.expression
            var value = if (endpointNameExpr == null) "" else (endpointNameExpr as UInjectionHost).evaluateToString()
            val methodTypes = if (methodExpr is UQualifiedReferenceExpression) {
                listOf(methodExpr.selector.toString())
            } else {
                when (methodExpr) {
                    is JavaUSimpleNameReferenceExpression -> listOf(methodExpr.resolvedName.toString())
                    is GrUReferenceExpression -> listOf(methodExpr.resolvedName.toString())
                    else -> EndpointDetector.httpMethods
                }
            }
            if (value.isNullOrEmpty()) value = "/"
            return methodTypes.map { DetectedEndpoint("$it:$value", false, value, it) }
        } else {
            var endpointNameExpr = annotation.attributeValues.find { it.name == "value" }
            if (endpointNameExpr == null) endpointNameExpr = annotation.attributeValues.find { it.name == "path" }
            var value = if (endpointNameExpr is UInjectionHost) {
                endpointNameExpr.evaluateToString()
            } else if (endpointNameExpr != null) {
                endpointNameExpr.evaluate()
            } else {
                "/"
            } as String
            val method = annotationName.substring(annotationName.lastIndexOf(".") + 1)
                .replace("Mapping", "").toUpperCase()
            if (value.isNullOrEmpty()) value = "/"
            return listOf(DetectedEndpoint("$method:$value", false, value.toString(), method))
        }
    }

    private fun handleKotlinAnnotation(
        isClass: Boolean,
        annotation: UAnnotation,
        annotationName: String
    ): List<DetectedEndpoint> {
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
                return listOf(DetectedEndpoint(value, false, value))
            }

            val methodExpr = annotation.attributeValues.find { it.name == "method" }?.expression
            var value = if (endpointNameExpr?.javaClass?.simpleName?.equals("KotlinUCollectionLiteralExpression") == true) {
                getField<List<UExpression>>(endpointNameExpr, "valueArguments")[0].evaluate()
            } else {
                endpointNameExpr?.evaluate() ?: ""
            }
            val valueArg = methodExpr?.let { getField<List<Any>>(it, "valueArguments")[0] }
            val methodTypes = when (valueArg) {
                is KotlinUSimpleReferenceExpression -> listOf(valueArg.resolvedName)
                is KotlinUQualifiedReferenceExpression -> listOf(valueArg.selector.asSourceString())
                else -> EndpointDetector.httpMethods
            }
            if (value?.toString().isNullOrEmpty()) value = "/"
            return methodTypes.map { DetectedEndpoint("$it:$value", false, value.toString(), it) }
        } else {
            var valueExpr = annotation.findAttributeValue("value")
            if (valueExpr == null) valueExpr = annotation.findAttributeValue("path")
            var value = if (valueExpr?.javaClass?.simpleName?.equals("KotlinUCollectionLiteralExpression") == true) {
                getField<List<UExpression>>(valueExpr, "valueArguments")[0].evaluate()
            } else if (valueExpr != null) {
                valueExpr.evaluate()
            } else {
                "/"
            }
            val method = annotationName.substring(annotationName.lastIndexOf(".") + 1)
                .replace("Mapping", "").toUpperCase()
            if (value?.toString().isNullOrEmpty()) value = "/"
            return listOf(DetectedEndpoint("$method:$value", false, value.toString(), method))
        }
    }

    private fun handleScalaAnnotation(
        isClass: Boolean,
        annotation: UAnnotation,
        annotationName: String
    ): List<DetectedEndpoint> {
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
                return listOf(DetectedEndpoint(value, false, value))
            }

            val methodExpr = annotation.attributeValues.find { it.name == "method" }?.expression
            var value = endpointNameExpr.getUCallExpression()!!.valueArguments[0].evaluate()
            val valueArg = methodExpr?.let { (it as UCallExpressionAdapter).valueArguments[0] }
            val methodTypes = if (valueArg is USimpleNameReferenceExpression) {
                listOf(valueArg.resolvedName)
            } else if (valueArg is UQualifiedReferenceExpressionAdapter) {
                listOf(valueArg.selector.asSourceString().substringAfter("RequestMethod."))
            } else {
                EndpointDetector.httpMethods
            }
            if (value?.toString().isNullOrEmpty()) value = "/"
            return methodTypes.map { DetectedEndpoint("$it:$value", false, value.toString(), it) }
        } else {
            var valueExpr = annotation.findAttributeValue("value")
            if (valueExpr == null) valueExpr = annotation.findAttributeValue("path")
            if (valueExpr is UastEmptyExpression) valueExpr =
                annotation.findAttributeValue("path") //todo: have to call this twice???
            if (valueExpr is UastEmptyExpression) valueExpr = null
            var value = if (valueExpr?.javaClass?.simpleName?.equals("KotlinUCollectionLiteralExpression") == true) {
                getField<List<UExpression>>(valueExpr, "valueArguments")[0].evaluate()
            } else if (valueExpr != null) {
                valueExpr.evaluate()
            } else {
                "/"
            }
            val method = annotationName.substring(annotationName.lastIndexOf(".") + 1)
                .replace("Mapping", "").toUpperCase()
            if (value?.toString().isNullOrEmpty()) value = "/"
            return listOf(DetectedEndpoint("$method:$value", false, value.toString(), method))
        }
    }

    private fun <T> getField(value: Any, name: String): T {
        val fields = Reflect.on(value).fields()
        return if (fields.containsKey(name)) {
            fields[name]!!.get()
        } else if (fields.containsKey("$name\$delegate")) {
            val value = fields["$name\$delegate"]!!.get<Any>()
            return if (value is Lazy<*>) {
                value.value as T
            } else {
                value as T
            }
        } else {
            throw IllegalArgumentException("Field $name not found")
        }
    }
}
