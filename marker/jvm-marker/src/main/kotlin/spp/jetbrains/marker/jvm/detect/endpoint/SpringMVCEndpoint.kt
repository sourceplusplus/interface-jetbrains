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

import com.intellij.lang.jvm.annotation.JvmAnnotationConstantValue
import com.intellij.lang.jvm.annotation.JvmAnnotationEnumFieldValue
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import com.intellij.psi.*
import io.vertx.core.Future
import io.vertx.core.Promise
import org.jetbrains.kotlin.idea.util.findAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScMethodCall
import spp.jetbrains.artifact.service.isGroovy
import spp.jetbrains.artifact.service.isScala
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
            if (guideMark.getPsiElement() is PsiMethod) {
                determineEndpointName(guideMark.getPsiElement() as PsiMethod)
            } else if (guideMark.getPsiElement() is KtNamedFunction) {
                determineEndpointName(guideMark.getPsiElement() as KtNamedFunction)
            } else {
                Future.succeededFuture(emptyList())
            }
        })
    }

    override fun determineEndpointName(element: PsiMethod): Future<List<DetectedEndpoint>> {
        val promise = Promise.promise<List<DetectedEndpoint>>()
        ApplicationManager.getApplication().runReadAction {
            for (annotationName in qualifiedNameSet) {
                val annotation = element.getAnnotation(annotationName)
                if (annotation != null) {
                    val detectedEndpoint = handleAnnotation(false, annotation, annotationName)
                    val classRequestMapping = element.containingClass?.getAnnotation(requestMappingAnnotation)
                    if (classRequestMapping != null) {
                        val classEndpoint = handleAnnotation(
                            true, classRequestMapping, requestMappingAnnotation
                        )
                        handleEndpointDetection(detectedEndpoint, classEndpoint, promise)
                    } else {
                        promise.complete(detectedEndpoint)
                    }
                }
            }

            promise.tryComplete(emptyList())
        }
        return promise.future()
    }

    override fun determineEndpointName(element: KtNamedFunction): Future<List<DetectedEndpoint>> {
        val promise = Promise.promise<List<DetectedEndpoint>>()
        ApplicationManager.getApplication().runReadAction {
            for (annotationName in qualifiedNameSet) {
                val annotation = element.findAnnotation(FqName(annotationName))
                if (annotation != null) {
                    val detectedEndpoint = handleAnnotation(false, annotation, annotationName)
                    val classRequestMapping = element.containingClass()
                        ?.findAnnotation(FqName(requestMappingAnnotation))
                    if (classRequestMapping != null) {
                        val classEndpoint = handleAnnotation(
                            true, classRequestMapping, requestMappingAnnotation
                        )
                        handleEndpointDetection(detectedEndpoint, classEndpoint, promise)
                    } else {
                        promise.complete(detectedEndpoint)
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

    private fun handleAnnotation(
        isClass: Boolean,
        annotation: PsiAnnotation,
        annotationName: String
    ): List<DetectedEndpoint> {
        if (annotationName == requestMappingAnnotation) {
            var endpointNameExpr = getAttributeValue(annotation, "value")
            if (endpointNameExpr == null) {
                endpointNameExpr = getAttributeValue(annotation, "path")
            }
            if (isClass) {
                val value = when (endpointNameExpr) {
                    null -> "/"
                    is JvmAnnotationConstantValue -> endpointNameExpr.constantValue?.toString()
                    is PsiLiteral -> endpointNameExpr.value?.toString()
                    else -> endpointNameExpr.toString()
                }
                return listOf(DetectedEndpoint(value.toString(), false, value.toString()))
            }

            val methodExpr = getAttributeValue(annotation, "method")
            val methodTypes = when {
                methodExpr is JvmAnnotationConstantValue -> listOf(methodExpr.constantValue?.toString())
                methodExpr is PsiReferenceExpression -> listOf(methodExpr.referenceName)
                methodExpr is JvmAnnotationEnumFieldValue -> listOf(methodExpr.fieldName)
                methodExpr is PsiElement && methodExpr.isGroovy() && methodExpr is GrReferenceExpression -> {
                    listOf(methodExpr.referenceName)
                }

                methodExpr is PsiElement && methodExpr.isScala() && methodExpr is ScMethodCall -> {
                    listOf(methodExpr.args().exprs().head().text.substringAfter("RequestMethod."))
                }

                else -> EndpointDetector.httpMethods
            }

            var value = when {
                endpointNameExpr == null -> ""
                endpointNameExpr is JvmAnnotationConstantValue -> endpointNameExpr.constantValue?.toString()
                endpointNameExpr is PsiElement && endpointNameExpr.isScala() && endpointNameExpr is ScMethodCall -> {
                    endpointNameExpr.args().exprs().head().text.replace("\"", "")
                }

                else -> (endpointNameExpr as? PsiLiteral)?.value?.toString()
            }
            if (value.isNullOrEmpty()) value = "/"
            return methodTypes.map { DetectedEndpoint("$it:$value", false, value, it) }
        } else {
            var endpointNameExpr = getAttributeValue(annotation, "value")
            if (endpointNameExpr == null) endpointNameExpr = getAttributeValue(annotation, "path")
            var value = if (endpointNameExpr is PsiLiteral) {
                endpointNameExpr.value?.toString()
            } else if (endpointNameExpr is JvmAnnotationConstantValue) {
                endpointNameExpr.constantValue?.toString()
            } else {
                "/"
            } as String
            val method = annotationName.substring(annotationName.lastIndexOf(".") + 1)
                .replace("Mapping", "").uppercase()
            if (value.isNullOrEmpty()) value = "/"
            return listOf(DetectedEndpoint("$method:$value", false, value.toString(), method))
        }
    }

    private fun handleAnnotation(
        isClass: Boolean,
        annotation: KtAnnotationEntry,
        annotationName: String
    ): List<DetectedEndpoint> {
        if (annotationName == requestMappingAnnotation) {
            var endpointNameExpr = getAttributeValue(annotation, "value")
            if (endpointNameExpr == null) {
                endpointNameExpr = getAttributeValue(annotation, "path")
            }
            if (endpointNameExpr == null) {
                endpointNameExpr = getAttributeValue(annotation, null)
            }
            if (isClass) {
                val value = if (endpointNameExpr is KtStringTemplateExpression) {
                    endpointNameExpr.entries?.firstOrNull()?.text ?: endpointNameExpr.text
                } else {
                    "/"
                }
                return listOf(DetectedEndpoint(value, false, value))
            }

            val methodTypes = when (val methodExpr = getAttributeValue(annotation, "method")) {
                is KtCollectionLiteralExpression -> methodExpr.getInnerExpressions().map {
                    when (it) {
                        is KtNameReferenceExpression -> it.getReferencedName()
                        is KtDotQualifiedExpression -> it.selectorExpression?.text
                        else -> null
                    }
                }

                else -> EndpointDetector.httpMethods
            }

            var value = if (endpointNameExpr is KtCollectionLiteralExpression) {
                endpointNameExpr.getInnerExpressions()[0].text.replace("\"", "")
            } else {
                null //endpointNameExpr?.evaluate() ?: ""
            }
            if (value.isNullOrEmpty()) value = "/"
            return methodTypes.map { DetectedEndpoint("$it:$value", false, value.toString(), it) }
        } else {
            var valueExpr = getAttributeValue(annotation, "value")
            if (valueExpr == null) {
                valueExpr = getAttributeValue(annotation, "path")
            }
            if (valueExpr == null) {
                valueExpr = getAttributeValue(annotation, null)
            }
            var value = when (valueExpr) {
                is KtCollectionLiteralExpression -> valueExpr.getInnerExpressions()[0].text.replace("\"", "")
                is KtStringTemplateExpression -> valueExpr.entries?.firstOrNull()?.text ?: valueExpr.text
                else -> "/"
            }
            val method = annotationName.substring(annotationName.lastIndexOf(".") + 1)
                .replace("Mapping", "").toUpperCase()
            if (value.isNullOrEmpty()) value = "/"
            return listOf(DetectedEndpoint("$method:$value", false, value, method))
        }
    }
}
