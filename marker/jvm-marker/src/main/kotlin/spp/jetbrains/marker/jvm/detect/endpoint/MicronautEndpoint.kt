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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiMethod
import io.vertx.core.Future
import io.vertx.core.Promise
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import spp.jetbrains.marker.jvm.detect.JVMEndpointDetector.JVMEndpointNameDetector
import spp.jetbrains.marker.source.info.EndpointDetector.DetectedEndpoint

/**
 * Detects Micronaut HTTP Server endpoints.
 *
 * @since 0.7.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class MicronautEndpoint : JVMEndpointNameDetector {

    private val log = logger<MicronautEndpoint>()
    private val endpointAnnotationPrefix = "io.micronaut.http.annotation."

    override fun determineEndpointName(element: PsiMethod): Future<List<DetectedEndpoint>> {
        val promise = Promise.promise<List<DetectedEndpoint>>()
        ApplicationManager.getApplication().runReadAction {
            val annotation = element.annotations.find {
                it.qualifiedName?.startsWith(endpointAnnotationPrefix) == true
            }
            if (annotation != null) {
                val endpointType = annotation.qualifiedName!!.substringAfterLast(".").uppercase()
                var endpointValue = getAttributeValue(annotation, "value")
                if (endpointValue == null) {
                    endpointValue = getAttributeValue(annotation, "null")
                }
                val value = if (endpointValue is PsiLiteral) {
                    endpointValue.value?.toString()
                } else {
                    null
                }

                //get controller
                val controllerAnnotation = element.containingClass?.annotations?.find {
                    it.qualifiedName?.startsWith(endpointAnnotationPrefix) == true
                }
                var controllerValue = controllerAnnotation?.let { getAttributeValue(it, "value") }
                if (controllerValue == null) {
                    controllerValue = controllerAnnotation?.let { getAttributeValue(it, "null") }
                }
                val controller = if (controllerValue is PsiLiteral) {
                    controllerValue.value?.toString()
                } else {
                    null
                }

                val endpoint = if (controller != null) "$controller$value" else value
                if (endpoint?.isNotBlank() == true) {
                    val endpointName = "$endpointType:$endpoint"
                    log.info("Detected Micronaut endpoint: $endpointName")
                    promise.complete(listOf(DetectedEndpoint(endpointName, false)))
                }
            }

            promise.tryComplete(emptyList())
        }
        return promise.future()
    }

    override fun determineEndpointName(element: KtNamedFunction): Future<List<DetectedEndpoint>> {
        val promise = Promise.promise<List<DetectedEndpoint>>()
        ApplicationManager.getApplication().runReadAction {
            val annotation = element.findAnnotationStartsWith(endpointAnnotationPrefix)
            if (annotation != null) {
                val endpointType = annotation.shortName!!.asString().uppercase()
                var endpointValue = getAttributeValue(annotation, "value")
                if (endpointValue == null) {
                    endpointValue = getAttributeValue(annotation, null)
                }
                val value = if (endpointValue is KtStringTemplateExpression) {
                    endpointValue.entries?.firstOrNull()?.text ?: endpointValue.text
                } else {
                    null
                }

                //get controller
                val controllerAnnotation =
                    element.containingClassOrObject?.findAnnotationStartsWith(endpointAnnotationPrefix)
                var controllerValue = controllerAnnotation?.let { getAttributeValue(it, "value") }
                if (controllerValue == null) {
                    controllerValue = controllerAnnotation?.let { getAttributeValue(it, null) }
                }
                val controller = if (controllerValue is KtStringTemplateExpression) {
                    controllerValue.entries?.firstOrNull()?.text ?: controllerValue.text
                } else {
                    null
                }

                val endpoint = if (controller != null) "$controller$value" else value
                if (endpoint?.isNotBlank() == true) {
                    val endpointName = "$endpointType:$endpoint"
                    log.info("Detected Micronaut endpoint: $endpointName")
                    promise.complete(listOf(DetectedEndpoint(endpointName, false)))
                }
            }

            promise.tryComplete(emptyList())
        }
        return promise.future()
    }

    private fun KtAnnotated.findAnnotationStartsWith(annotationName: String): KtAnnotationEntry? {
        if (annotationEntries.isEmpty()) return null

        val context = analyze(bodyResolveMode = BodyResolveMode.PARTIAL)
        val descriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, this] ?: return null

        // Make sure all annotations are resolved
        descriptor.annotations.toList()

        return annotationEntries.firstOrNull { entry ->
            context.get(BindingContext.ANNOTATION, entry)?.fqName?.asString()?.startsWith(annotationName) == true
        }
    }
}
