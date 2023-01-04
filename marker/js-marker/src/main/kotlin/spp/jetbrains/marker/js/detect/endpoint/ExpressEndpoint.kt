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

/**
 * Endpoint detector for Express.js endpoints.
 *
 * @since 0.7.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class ExpressEndpoint : EndpointDetector.EndpointNameDetector {

    private val log = logger<ExpressEndpoint>()

    override fun detectEndpointNames(guideMark: GuideMark): Future<List<DetectedEndpoint>> {
        if (!guideMark.isExpressionMark) {
            return Future.succeededFuture(emptyList())
        }

        val promise = Promise.promise<List<DetectedEndpoint>>()
        ApplicationManager.getApplication().runReadAction {
            if (guideMark.getPsiElement() !is JSCallExpression) {
                promise.complete(emptyList())
                return@runReadAction
            }

            val expression = guideMark.getPsiElement() as JSCallExpression
            val method = expression.firstChild as JSReferenceExpression
            if (method.firstChild !is JSReferenceExpression) {
                promise.complete(emptyList())
                return@runReadAction
            }
            val router = method.firstChild as JSReferenceExpression
            val routerVariable = router.resolve() as JSInitializerOwner? ?: run {
                promise.complete(emptyList())
                return@runReadAction
            }

            if (method.children.size < 3) {
                promise.complete(emptyList())
                return@runReadAction
            }
            val endpointType = method.children[2].text
            if (expression.arguments.isEmpty()) {
                promise.complete(emptyList())
                return@runReadAction
            }
            val endpointName = getArgumentValue(expression.arguments[0])

            if (EndpointDetector.httpMethods.contains(endpointType.uppercase())) {
                val basePath = locateRouter(routerVariable)

                log.info("Detected Express endpoint: $basePath$endpointName")
                promise.complete(basePath.map {
                    if (it == "/") {
                        return@map DetectedEndpoint(
                            "" + endpointName,
                            false,
                            type = endpointType.uppercase()
                        )
                    }

                    return@map DetectedEndpoint(
                        it + endpointName,
                        false,
                        type = endpointType.uppercase()
                    )
                })
            } else if (endpointType == "all") {
                val detectedEndpoints = mutableListOf<DetectedEndpoint>()
                val basePath = locateRouter(routerVariable)

                EndpointDetector.httpMethods.forEach { endpointType ->
                    log.info("Detected Express endpoint: $basePath$endpointName")
                    detectedEndpoints.addAll(basePath.map {
                        if (it == "/") {
                            return@map DetectedEndpoint(
                                "" + endpointName,
                                false,
                                type = endpointType.uppercase()
                            )
                        }

                        return@map DetectedEndpoint(
                            it + endpointName,
                            false,
                            type = endpointType.uppercase()
                        )
                    })
                }
                promise.complete(detectedEndpoints)
            } else {
                promise.complete(emptyList())
            }
        }
        return promise.future()
    }

    private fun locateRouter(routerVariable: JSInitializerOwner): List<String> {
        if (isExpressApp(routerVariable)) {
            return listOf("")
        }

        if (routerVariable.initializer !is JSCallExpression) {
            return emptyList()
        }
        val initializer = routerVariable.initializer as JSCallExpression

        val initializerMethod = initializer.firstChild
        if (initializerMethod.children.getOrNull(2)?.text != "Router") { // TODO: Is this the only thing we want out of express?
            return emptyList()
        }

        if (!isExpress(initializerMethod.firstChild)) {
            return emptyList()
        }

        val indicator = EmptyProgressIndicator(ModalityState.defaultModalityState())
        val routes = ProgressManager.getInstance().runProcess(Computable {
            ReferencesSearch.search(routerVariable).flatMap {
                if (it.element.parent !is JSElement) {
                    return@flatMap emptyList()
                }
                return@flatMap resolveRouterPath(it.element as JSElement)
            }.map { it }
        }, indicator)

        if (routes.size > 1) {
            // TODO: Handle multiple routes
        }

        return routes
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
        val expressInitializer = resolveVariableSource(element) as? JSCallExpression ?: return false
        if (!expressInitializer.isRequireCall) {
            return false
        }
        if (expressInitializer.arguments[0] !is JSLiteralExpression) { // TODO: How would we support require with a variable?
            return false
        }

        val expressRequire = expressInitializer.arguments[0] as JSLiteralExpression
        return expressRequire.value == "express" // TODO: Is this the only way to get express?
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

    private fun resolveVariableSource(element: PsiElement): JSExpression? {
        if (element is JSReferenceExpression) {
            val resolved = element.resolve() ?: return null
            return resolveVariableSource(resolved)
        }
        if (element is JSVariable) {
            return element.initializer
        }
        if (element is JSExpression) {
            return element
        }
        return null
    }

    private fun resolveRouterPath(router: JSElement): List<String> {
        // Handle direct require statement
        if (router.parent is JSAssignmentExpression) {
            val assignment = router.parent as JSAssignmentExpression
            if (assignment.firstChild.text != "module.exports") { // TODO: Is this the best way to detect exports?
                return emptyList()
            }

            if (assignment.children[1] == router) { // Search for direct require
                val jsFile = router.containingFile as? JSFile ?: return emptyList()
                return ReferencesSearch.search(jsFile).flatMap {
                    val requireArgumentList = it.element.parent as? JSArgumentList ?: return@flatMap emptyList()
                    val requireCall = requireArgumentList.parent as? JSCallExpression ?: return@flatMap emptyList()
                    if (!requireCall.isRequireCall) {
                        return@flatMap emptyList()
                    }

                    return@flatMap resolveRouterPath(requireCall)
                }
            }
        }
        if (router.parent is JSProperty) {
            val property = router.parent as JSProperty
            return ReferencesSearch.search(property).flatMap {
                return@flatMap resolveRouterPath(it.element as JSElement)
            }
        }

        // Check if this variable is indeed being used as a router
        val argumentList = router.parent as? JSArgumentList ?: return emptyList()
        if (argumentList.arguments.size != 2) { // TODO: Is there any situation where this isn't the case?
            return emptyList()
        }
        if (argumentList.arguments[1] != router) {
            return emptyList()
        }

        val callExpression = argumentList.parent
        val useReference = callExpression.firstChild as JSReferenceExpression
        val superRouter = (useReference.firstChild as JSReferenceExpression).resolve() as JSVariable
        val superPath = locateRouter(superRouter)
        val path = getArgumentValue(argumentList.arguments[0])

        return superPath.map { it + path }
    }
}
