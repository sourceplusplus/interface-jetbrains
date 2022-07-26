/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
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
package spp.jetbrains.marker.jvm.psi.endpoint

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import io.vertx.core.Future
import io.vertx.core.Promise
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.expressions.UInjectionHost
import org.jetbrains.uast.toUElementOfType
import spp.jetbrains.marker.jvm.JVMEndpointDetector.JVMEndpointNameDeterminer
import spp.jetbrains.marker.source.info.EndpointDetector.DetectedEndpoint
import spp.jetbrains.marker.source.mark.guide.MethodGuideMark
import java.util.*

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SkywalkingTraceEndpoint : JVMEndpointNameDeterminer {

    private val skywalkingTraceAnnotation = "org.apache.skywalking.apm.toolkit.trace.Trace"

    override fun determineEndpointName(guideMark: MethodGuideMark): Future<Optional<DetectedEndpoint>> {
        return ApplicationManager.getApplication().runReadAction(Computable {
            val uMethod = guideMark.getPsiElement().toUElementOfType<UMethod>()
                ?: return@Computable Future.succeededFuture(Optional.empty())
            determineEndpointName(uMethod)
        })
    }

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
