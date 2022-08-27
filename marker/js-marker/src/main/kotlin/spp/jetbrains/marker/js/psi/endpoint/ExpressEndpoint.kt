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
package spp.jetbrains.marker.js.psi.endpoint

import com.intellij.lang.javascript.psi.JSCallExpression
import com.intellij.openapi.application.ApplicationManager
import io.vertx.core.Future
import io.vertx.core.Promise
import spp.jetbrains.marker.source.info.EndpointDetector
import spp.jetbrains.marker.source.info.EndpointDetector.DetectedEndpoint
import spp.jetbrains.marker.source.mark.guide.GuideMark
import spp.jetbrains.marker.source.mark.guide.MethodGuideMark
import java.util.*

/**
 * todo: description.
 *
 * @since 0.5.5
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class ExpressEndpoint : EndpointDetector.EndpointNameDeterminer {
    override fun determineEndpointName(guideMark: GuideMark): Future<Optional<DetectedEndpoint>> {
        System.err.println("determining endpoint name for $guideMark")

        if (!guideMark.isExpressionMark) {
            return Future.succeededFuture(Optional.empty())
        }

        val promise = Promise.promise<Optional<DetectedEndpoint>>()
        ApplicationManager.getApplication().runReadAction {
            if (guideMark.getPsiElement() !is JSCallExpression) {
                promise.complete(Optional.empty())
                return@runReadAction
            }

            val expression = guideMark.getPsiElement() as JSCallExpression
            System.err.println("expression: " + expression.text)
        }
        return promise.future()
    }
}
