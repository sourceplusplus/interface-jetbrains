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
package spp.jetbrains.marker.py.detect.endpoint

import com.intellij.openapi.application.ApplicationManager
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.PyKeywordArgument
import com.jetbrains.python.psi.PyListLiteralExpression
import com.jetbrains.python.psi.PyStringLiteralExpression
import io.vertx.core.Future
import io.vertx.core.Promise
import spp.jetbrains.marker.source.info.EndpointDetector
import spp.jetbrains.marker.source.info.EndpointDetector.DetectedEndpoint
import spp.jetbrains.marker.source.mark.guide.GuideMark

/**
 * todo: description.
 *
 * @since 0.5.5
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class FlaskEndpoint : EndpointDetector.EndpointNameDetector {

    override fun detectEndpointNames(guideMark: GuideMark): Future<List<DetectedEndpoint>> {
        if (!guideMark.isMethodMark) {
            return Future.succeededFuture(emptyList())
        }

        val promise = Promise.promise<List<DetectedEndpoint>>()
        ApplicationManager.getApplication().runReadAction {
            val decorators = (guideMark.getPsiElement() as PyFunction).decoratorList?.decorators
            decorators?.forEach {
                if (it.qualifiedName.toString() == "app.route") {
                    val args = it.arguments
                    val arg = args[0]
                    var endpointName: String? = null
                    if (arg is PyStringLiteralExpression) {
                        endpointName = arg.stringValue
                    }

                    if (endpointName != null) {
                        var methodType: String? = null
                        if (args.size > 1 && args[1] is PyKeywordArgument) {
                            val keywordArg = args[1] as PyKeywordArgument
                            if (keywordArg.keyword == "methods") {
                                val valExpression = keywordArg.valueExpression
                                methodType = when (valExpression) {
                                    is PyListLiteralExpression -> valExpression.elements
                                        .joinToString(",") { (it as PyStringLiteralExpression).stringValue }
                                    else -> valExpression?.text
                                }
                            }
                        }
                        promise.complete(listOf(DetectedEndpoint(endpointName, false, type = methodType)))
                        return@runReadAction
                    }
                }
            }
            promise.tryComplete(emptyList())
        }
        return promise.future()
    }
}
