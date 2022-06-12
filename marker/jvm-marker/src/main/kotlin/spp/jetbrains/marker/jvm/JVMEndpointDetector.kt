/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package spp.jetbrains.marker.jvm

import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import org.jetbrains.uast.UMethod
import spp.jetbrains.marker.jvm.JVMEndpointDetector.JVMEndpointNameDeterminer
import spp.jetbrains.marker.jvm.psi.endpoint.SkywalkingTraceEndpoint
import spp.jetbrains.marker.jvm.psi.endpoint.SpringMVCEndpoint
import spp.jetbrains.marker.source.info.EndpointDetector
import java.util.*

/**
 * todo: description.
 *
 * @since 0.5.5
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class JVMEndpointDetector(vertx: Vertx) : EndpointDetector<JVMEndpointNameDeterminer>(vertx) {

    override val detectorSet: Set<JVMEndpointNameDeterminer> = setOf(
        SkywalkingTraceEndpoint(),
        SpringMVCEndpoint()
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
