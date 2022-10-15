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
package spp.jetbrains.marker.js.detect.endpoint

import com.intellij.lang.javascript.psi.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiElement
import com.intellij.psi.search.searches.ReferencesSearch
import io.vertx.core.Future
import io.vertx.core.Promise
import spp.jetbrains.marker.source.info.EndpointDetector
import spp.jetbrains.marker.source.info.EndpointDetector.DetectedEndpoint
import spp.jetbrains.marker.source.mark.guide.GuideMark
import java.util.*

/**
 * todo: description.
 *
 * @since 0.7.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class ExpressEndpoint : EndpointDetector.EndpointNameDeterminer {

    private val log = logger<ExpressEndpoint>()

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
            if (method.firstChild !is JSReferenceExpression) {
                promise.complete(Optional.empty())
                return@runReadAction
            }
            val router = method.firstChild as JSReferenceExpression
            val routerVariable = router.resolve() as JSInitializerOwner? ?: run {
                promise.complete(Optional.empty())
                return@runReadAction
            }

            if (method.children.size < 3) {
                promise.complete(Optional.empty())
                return@runReadAction
            }
            val endpointType = method.children[2].text
            if (expression.arguments.isEmpty()) {
                promise.complete(Optional.empty())
                return@runReadAction
            }
            val endpointName = getArgumentValue(expression.arguments[0])

            if (endpointType == "get" ||
                endpointType == "post" ||
                endpointType == "put" ||
                endpointType == "delete"
            ) {
                var basePath = locateRouter(routerVariable)
                if (basePath == null) {
                    promise.complete(Optional.empty())
                    return@runReadAction
                } else if (basePath == "/") {
                    basePath = ""
                }

                log.info("Detected Express endpoint: $basePath$endpointName")
                promise.complete(
                    Optional.of(
                        DetectedEndpoint(
                            basePath + endpointName,
                            false,
                            type = endpointType.uppercase()
                        )
                    )
                )
            } else {
                promise.complete(Optional.empty())
            }
        }
        return promise.future()
    }

    private fun locateRouter(routerVariable: JSInitializerOwner): String? {
        if (isExpressApp(routerVariable)) {
            return ""
        }

        if (routerVariable.initializer !is JSCallExpression) {
            return null
        }
        val initializer = routerVariable.initializer as JSCallExpression

        val initializerMethod = initializer.firstChild
        if (initializerMethod.children.getOrNull(2)?.text != "Router") { // TODO: Is this the only thing we want out of express?
            return null
        }

        val expressReference = initializerMethod.children[0] as JSReferenceExpression
        val expressVariable = expressReference.resolve() as JSVariable
        if (expressVariable.initializer !is JSCallExpression) {
            return null
        }

        val expressInitializer = expressVariable.initializer as JSCallExpression
        if (!expressInitializer.isRequireCall) {
            return null
        }
        if (expressInitializer.arguments[0] !is JSLiteralExpression) { // TODO: How would we support require with a variable?
            return null
        }

        val expressRequire = expressInitializer.arguments[0] as JSLiteralExpression
        if (expressRequire.value != "express") { // TODO: Is this the only way to get express?
            return null
        }

        val indicator = EmptyProgressIndicator(ModalityState.defaultModalityState())
        val routes = ProgressManager.getInstance().runProcess(Computable {
            ReferencesSearch.search(routerVariable).map {
                if (it.element.parent !is JSArgumentList) {
                    return@map null
                }
                val argumentList = it.element.parent as JSArgumentList
                if (argumentList.arguments.size != 2) { // TODO: Is there any situation where this isn't the case?
                    return@map null
                }
                if (argumentList.arguments[1] != it.element) {
                    return@map null
                }

                val callExpression = it.element.parent.parent
                val useReference = callExpression.firstChild as JSReferenceExpression
                val superRouter = (useReference.firstChild as JSReferenceExpression).resolve() as JSVariable
                val superPath = locateRouter(superRouter) ?: return@map null
                val path = getArgumentValue(argumentList.arguments[0])

                return@map superPath + path
            }.filterNotNull().map { it }
        }, indicator)

        if (routes.size > 1) {
            // TODO: Handle multiple routes
        }

        return routes.getOrNull(0)
    }

    private fun isExpressApp(element: PsiElement): Boolean {
        if (element is JSVariable) {
            if (element.initializer !is JSCallExpression) {
                return false
            }
            val initializer = element.initializer as JSCallExpression
            return isExpress(initializer.firstChild)
        }
        if (element is JSReferenceExpression) {
            val resolved = element.resolve() ?: return false
            return isExpressApp(resolved)
        }
        return false
    }

    private fun isExpress(element: PsiElement): Boolean {
        if (element is JSVariable) {
            val initializer = element.initializer
            if (initializer !is JSCallExpression) {
                return false
            }

            if (initializer.arguments[0] !is JSLiteralExpression) { // TODO: How would we support require with a variable?
                return false
            }

            val require = initializer.arguments[0] as JSLiteralExpression
            if (require.value != "express") {
                return false
            }

            return true
        }
        if (element is JSReferenceExpression) {
            val resolved = element.resolve() ?: return false
            return isExpress(resolved)
        }
        return false
    }

    private fun getArgumentValue(element: PsiElement): Any? {
        if (element is JSLiteralExpression) {
            return element.value
        }
        if (element is JSReferenceExpression) {
            val resolved = element.resolve() ?: return element
            return getArgumentValue(resolved)
        }
        return null
    }
}
