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
package spp.jetbrains.marker.jvm.detect

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiMethod
import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.Promise
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.annotation.GrAnnotationNameValuePair
import spp.jetbrains.artifact.service.isGroovy
import spp.jetbrains.marker.jvm.detect.JVMEndpointDetector.JVMEndpointNameDetector
import spp.jetbrains.marker.jvm.detect.endpoint.MicronautEndpoint
import spp.jetbrains.marker.jvm.detect.endpoint.SkywalkingTraceEndpoint
import spp.jetbrains.marker.jvm.detect.endpoint.SpringMVCEndpoint
import spp.jetbrains.marker.source.info.EndpointDetector

/**
 * todo: description.
 *
 * @since 0.5.5
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class JVMEndpointDetector(project: Project) : EndpointDetector<JVMEndpointNameDetector>(project) {

    override val detectorSet: Set<JVMEndpointNameDetector> = setOf(
        SkywalkingTraceEndpoint(),
        SpringMVCEndpoint(),
        MicronautEndpoint()
    )

    fun determineEndpointName(element: PsiMethod): Future<List<DetectedEndpoint>> {
        val promise = Promise.promise<List<DetectedEndpoint>>()
        CompositeFuture.all(detectorSet.map { it.determineEndpointName(element) }).onComplete {
            if (it.succeeded()) {
                val detectedEndpoints = it.result().list<List<DetectedEndpoint>>()
                promise.complete(detectedEndpoints.firstOrNull { it.isNotEmpty() } ?: emptyList())
            } else {
                promise.fail(it.cause())
            }
        }
        return promise.future()
    }

    fun determineEndpointName(element: KtNamedFunction): Future<List<DetectedEndpoint>> {
        val promise = Promise.promise<List<DetectedEndpoint>>()
        CompositeFuture.all(detectorSet.map { it.determineEndpointName(element) }).onComplete {
            if (it.succeeded()) {
                val detectedEndpoints = it.result().list<List<DetectedEndpoint>>()
                promise.complete(detectedEndpoints.firstOrNull { it.isNotEmpty() } ?: emptyList())
            } else {
                promise.fail(it.cause())
            }
        }
        return promise.future()
    }

    interface JVMEndpointNameDetector : EndpointNameDetector {
        fun determineEndpointName(element: PsiMethod): Future<List<DetectedEndpoint>>
        fun determineEndpointName(element: KtNamedFunction): Future<List<DetectedEndpoint>>

        fun getAttributeValue(annotation: PsiAnnotation, name: String): Any? {
            return if (annotation.isGroovy()) {
                (annotation.attributes.find { it.attributeName == name } as? GrAnnotationNameValuePair)?.value
            } else if (annotation.attributes.any { it.attributeName == name }) {
                annotation.findAttributeValue(name)
            } else {
                annotation.attributes.find { it.attributeName == name }?.attributeValue
            }
        }

        fun getAttributeValue(annotation: KtAnnotationEntry, name: String?): KtExpression? {
            return annotation.valueArguments.find { it.getArgumentName()?.asName?.identifier == name }
                ?.getArgumentExpression()
        }
    }
}
