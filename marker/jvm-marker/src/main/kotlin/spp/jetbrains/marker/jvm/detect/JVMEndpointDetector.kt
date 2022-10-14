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
package spp.jetbrains.marker.jvm.detect

import com.intellij.openapi.project.Project
import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.Promise
import org.jetbrains.uast.UMethod
import spp.jetbrains.marker.jvm.detect.JVMEndpointDetector.JVMEndpointNameDeterminer
import spp.jetbrains.marker.jvm.detect.endpoint.MicronautEndpoint
import spp.jetbrains.marker.jvm.detect.endpoint.SkywalkingTraceEndpoint
import spp.jetbrains.marker.jvm.detect.endpoint.SpringMVCEndpoint
import spp.jetbrains.marker.source.info.EndpointDetector
import java.util.*

/**
 * todo: description.
 *
 * @since 0.5.5
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class JVMEndpointDetector(project: Project) : EndpointDetector<JVMEndpointNameDeterminer>(project) {

    override val detectorSet: Set<JVMEndpointNameDeterminer> = setOf(
        SkywalkingTraceEndpoint(),
        SpringMVCEndpoint(),
        MicronautEndpoint()
    )

    fun determineEndpointName(uMethod: UMethod): Future<Optional<DetectedEndpoint>> {
        val promise = Promise.promise<Optional<DetectedEndpoint>>()
        CompositeFuture.all(detectorSet.map { it.determineEndpointName(uMethod) }).onComplete {
            if (it.succeeded()) {
                it.result().list<Optional<DetectedEndpoint>>().find { it.isPresent }?.let {
                    promise.complete(it)
                } ?: promise.complete(Optional.empty())
            } else {
                promise.fail(it.cause())
            }
        }
        return promise.future()
    }

    interface JVMEndpointNameDeterminer : EndpointNameDeterminer {
        fun determineEndpointName(uMethod: UMethod): Future<Optional<DetectedEndpoint>>
    }
}
