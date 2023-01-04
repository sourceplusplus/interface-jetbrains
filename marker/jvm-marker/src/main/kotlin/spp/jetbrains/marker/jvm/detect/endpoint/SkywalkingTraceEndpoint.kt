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

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.Computable
import io.vertx.core.Future
import io.vertx.core.Promise
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.expressions.UInjectionHost
import org.jetbrains.uast.toUElementOfType
import spp.jetbrains.marker.jvm.detect.JVMEndpointDetector.JVMEndpointNameDetector
import spp.jetbrains.marker.source.info.EndpointDetector.DetectedEndpoint
import spp.jetbrains.marker.source.mark.guide.GuideMark

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SkywalkingTraceEndpoint : JVMEndpointNameDetector {

    private val skywalkingTraceAnnotation = "org.apache.skywalking.apm.toolkit.trace.Trace"

    override fun detectEndpointNames(guideMark: GuideMark): Future<List<DetectedEndpoint>> {
        if (!guideMark.isMethodMark) {
            return Future.succeededFuture(emptyList())
        }

        return DumbService.getInstance(guideMark.project).runReadActionInSmartMode(Computable {
            val uMethod = guideMark.getPsiElement().toUElementOfType<UMethod>()
                ?: return@Computable Future.succeededFuture(emptyList())
            determineEndpointName(uMethod)
        })
    }

    override fun determineEndpointName(uMethod: UMethod): Future<List<DetectedEndpoint>> {
        val promise = Promise.promise<List<DetectedEndpoint>>()
        DumbService.getInstance(uMethod.project).runReadActionInSmartMode {
            val annotation = uMethod.findAnnotation(skywalkingTraceAnnotation)
            if (annotation != null) {
                val operationNameExpr = annotation.attributeValues.find { it.name == "operationName" }
                val value = if (operationNameExpr is UInjectionHost) {
                    operationNameExpr.evaluateToString()
                } else {
                    operationNameExpr?.evaluate()
                } as String?
                if (value == null || value == "") {
                    val endpointName = "${uMethod.containingClass!!.qualifiedName}.${uMethod.name}"
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
