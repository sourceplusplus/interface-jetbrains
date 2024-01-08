/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2024 CodeBrig, Inc.
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

import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiMethod
import io.vertx.core.Future
import io.vertx.core.Promise
import org.jetbrains.kotlin.idea.refactoring.memberInfo.qualifiedClassNameForRendering
import org.jetbrains.kotlin.idea.util.findAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import spp.jetbrains.marker.jvm.detect.JVMEndpointDetector.JVMEndpointNameDetector
import spp.jetbrains.marker.source.info.EndpointDetector.DetectedEndpoint

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SkywalkingTraceEndpoint : JVMEndpointNameDetector {

    private val skywalkingTraceAnnotation = "org.apache.skywalking.apm.toolkit.trace.Trace"

    override fun determineEndpointName(element: PsiMethod): Future<List<DetectedEndpoint>> {
        val promise = Promise.promise<List<DetectedEndpoint>>()
        DumbService.getInstance(element.project).runReadActionInSmartMode {
            val annotation = element.getAnnotation(skywalkingTraceAnnotation)
            if (annotation != null) {
                val operationNameExpr = annotation.findAttributeValue("operationName")
                val value = if (operationNameExpr is PsiLiteral) {
                    operationNameExpr.value?.toString()
                } else {
                    null
                }
                if (value == null || value == "") {
                    val endpointName = "${element.containingClass!!.qualifiedName}.${element.name}"
                    promise.complete(listOf(DetectedEndpoint(endpointName, true)))
                } else {
                    promise.complete(listOf(DetectedEndpoint(value, true)))
                }
            }

            promise.tryComplete(emptyList())
        }
        return promise.future()
    }

    override fun determineEndpointName(element: KtNamedFunction): Future<List<DetectedEndpoint>> {
        val promise = Promise.promise<List<DetectedEndpoint>>()
        DumbService.getInstance(element.project).runReadActionInSmartMode {
            val annotation = try {
                element.findAnnotation(FqName(skywalkingTraceAnnotation))
            } catch (ignored: Exception) {
                null
            }
            if (annotation != null) {
                val operationNameExpr = getAttributeValue(annotation, "operationName")
                val value = if (operationNameExpr is KtStringTemplateExpression) {
                    operationNameExpr.entries?.firstOrNull()?.text ?: operationNameExpr.text
                } else {
                    null
                }
                if (value == null || value == "") {
                    val endpointName = "${element.containingClass()!!.qualifiedClassNameForRendering()}.${element.name}"
                    promise.complete(listOf(DetectedEndpoint(endpointName, true)))
                } else {
                    promise.complete(listOf(DetectedEndpoint(value, true)))
                }
            }

            promise.tryComplete(emptyList())
        }
        return promise.future()
    }
}
