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
