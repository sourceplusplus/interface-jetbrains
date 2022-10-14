/*
 * Source++, the continuous feedback platform for developers.
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
package spp.jetbrains.marker.jvm.detect.endpoint

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.Computable
import io.vertx.core.Future
import io.vertx.core.Promise
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.expressions.UInjectionHost
import org.jetbrains.uast.toUElementOfType
import spp.jetbrains.marker.jvm.detect.JVMEndpointDetector.JVMEndpointNameDeterminer
import spp.jetbrains.marker.source.info.EndpointDetector.DetectedEndpoint
import spp.jetbrains.marker.source.mark.guide.GuideMark
import java.util.*

/**
 * Detects Micronaut HTTP Server endpoints.
 *
 * @since 0.7.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class MicronautEndpoint : JVMEndpointNameDeterminer {

    private val log = logger<MicronautEndpoint>()
    private val endpointAnnotationPrefix = "io.micronaut.http.annotation."

    override fun determineEndpointName(uMethod: UMethod): Future<Optional<DetectedEndpoint>> {
        val promise = Promise.promise<Optional<DetectedEndpoint>>()
        ApplicationManager.getApplication().runReadAction {
            val annotation = uMethod.annotations.find {
                it.qualifiedName?.startsWith(endpointAnnotationPrefix) == true
            }.toUElementOfType<UAnnotation>()
            if (annotation?.qualifiedName != null) {
                val endpointType = annotation.qualifiedName!!.substringAfterLast(".").uppercase()
                val endpointValue = annotation.attributeValues.find { it.name == null }
                val value = if (endpointValue is UInjectionHost) {
                    endpointValue.evaluateToString()
                } else {
                    endpointValue?.evaluate()
                } as String?

                //get controller
                val controllerAnnotation = uMethod.containingClass?.annotations?.find {
                    it.qualifiedName?.startsWith(endpointAnnotationPrefix) == true
                }?.toUElementOfType<UAnnotation>()
                val controllerValue = controllerAnnotation?.attributeValues?.find { it.name == null }
                val controller = if (controllerValue is UInjectionHost) {
                    controllerValue.evaluateToString()
                } else {
                    controllerValue?.evaluate()
                } as String?

                val endpoint = if (controller != null) "$controller$value" else value
                if (endpoint?.isNotBlank() == true) {
                    val endpointName = "$endpointType:$endpoint"
                    log.info("Detected Micronaut endpoint: $endpointName")
                    promise.complete(Optional.of(DetectedEndpoint(endpointName, false)))
                }
            }

            promise.tryComplete(Optional.empty())
        }
        return promise.future()
    }

    override fun determineEndpointName(guideMark: GuideMark): Future<Optional<DetectedEndpoint>> {
        if (!guideMark.isMethodMark) {
            return Future.succeededFuture(Optional.empty())
        }

        return ApplicationManager.getApplication().runReadAction(Computable {
            val uMethod = guideMark.getPsiElement().toUElementOfType<UMethod>()
                ?: return@Computable Future.succeededFuture(Optional.empty())
            determineEndpointName(uMethod)
        })
    }
}
