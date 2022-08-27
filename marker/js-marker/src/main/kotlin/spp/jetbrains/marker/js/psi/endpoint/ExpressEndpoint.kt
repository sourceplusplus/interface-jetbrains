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
import com.intellij.lang.javascript.psi.JSLiteralExpression
import com.intellij.lang.javascript.psi.JSReferenceExpression
import com.intellij.lang.javascript.psi.JSVariable
import com.intellij.openapi.application.ApplicationManager
import io.vertx.core.Future
import io.vertx.core.Promise
import spp.jetbrains.marker.source.info.EndpointDetector
import spp.jetbrains.marker.source.info.EndpointDetector.DetectedEndpoint
import spp.jetbrains.marker.source.mark.guide.GuideMark
import java.util.*

/**
 * todo: description.
 *
 * @since 0.5.5
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class ExpressEndpoint : EndpointDetector.EndpointNameDeterminer {
    override fun determineEndpointName(guideMark: GuideMark): Future<Optional<DetectedEndpoint>> {
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
            val method = expression.firstChild as JSReferenceExpression
            val router = method.firstChild as JSReferenceExpression
            val routerVariable = router.resolve() as JSVariable
            if (routerVariable.initializer !is JSCallExpression) {
                promise.complete(Optional.empty())
                return@runReadAction
            }
            val initializer = routerVariable.initializer as JSCallExpression

            var isRouter = false
            if (initializer.isRequireCall) {
                if (initializer.arguments[0] !is JSLiteralExpression) { // TODO: How would we support require with a variable?
                    promise.complete(Optional.empty())
                    return@runReadAction
                }

                val require = initializer.arguments[0] as JSLiteralExpression
                if (require.value != "express") {
                    promise.complete(Optional.empty())
                    return@runReadAction
                }
            } else {
                val initializerMethod = initializer.firstChild
                if (initializerMethod.children[2].text != "Router") { // TODO: Is this the only thing we want out of express?
                    promise.complete(Optional.empty())
                    return@runReadAction
                }

                val expressReference = initializerMethod.children[0] as JSReferenceExpression
                val expressVariable = expressReference.resolve() as JSVariable
                if (expressVariable.initializer !is JSCallExpression) {
                    promise.complete(Optional.empty())
                    return@runReadAction
                }

                val expressInitializer = expressVariable.initializer as JSCallExpression
                if (!expressInitializer.isRequireCall) {
                    promise.complete(Optional.empty())
                    return@runReadAction
                }
                if (expressInitializer.arguments[0] !is JSLiteralExpression) { // TODO: How would we support require with a variable?
                    promise.complete(Optional.empty())
                    return@runReadAction
                }

                val expressRequire = expressInitializer.arguments[0] as JSLiteralExpression
                if (expressRequire.value != "express") { // TODO: Is this the only way to get express?
                    promise.complete(Optional.empty())
                    return@runReadAction
                }

                isRouter = true
            }

            val endpointType = method.children[2].text
            val endpointName = expression.arguments[0].text

            if (isRouter) {
                // TODO: Find usages of routerVariable
            }

            if (endpointType == "get" ||
                endpointType == "post" ||
                endpointType == "put" ||
                endpointType == "delete") {
                promise.complete(Optional.of(DetectedEndpoint(endpointName, false, type = endpointType.toUpperCase())))
            } else {
                promise.complete(Optional.empty())
            }
        }
        return promise.future()
    }
}
