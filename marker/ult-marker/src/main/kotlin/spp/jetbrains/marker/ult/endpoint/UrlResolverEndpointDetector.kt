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
package spp.jetbrains.marker.ult.endpoint

import com.intellij.microservices.url.UrlPath
import com.intellij.microservices.url.UrlResolveRequest
import com.intellij.microservices.url.UrlResolverManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import io.vertx.core.Future
import io.vertx.core.Promise
import spp.jetbrains.UserData
import spp.jetbrains.marker.source.info.EndpointDetector
import spp.jetbrains.marker.source.info.EndpointDetector.EndpointNameDetector
import spp.jetbrains.marker.source.mark.guide.GuideMark
import spp.jetbrains.executeBlockingReadActionWhenSmart

/**
 * Detects endpoints using IntelliJ's [UrlResolverManager] experimental functionality.
 *
 * @since 0.7.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class UrlResolverEndpointDetector(
    project: Project
) : EndpointDetector<EndpointNameDetector>(project), EndpointNameDetector {

    companion object {
        private val log = logger<UrlResolverEndpointDetector>()

        fun isAvailable(): Boolean {
            return try {
                Class.forName("com.intellij.microservices.url.UrlResolverManager")
                true
            } catch (ignore: ClassNotFoundException) {
                false
            }
        }
    }

    override val detectorSet = setOf(this)

    override fun detectEndpointNames(guideMark: GuideMark): Future<List<DetectedEndpoint>> {
        val detectedEndpointPromise = Promise.promise<List<DetectedEndpoint>>()
        UserData.vertx(guideMark.project).executeBlockingReadActionWhenSmart(guideMark.project) {
            UrlResolverManager.getInstance(guideMark.project).getVariants(
                UrlResolveRequest(null, null, UrlPath.fromExactString(""), null)
            )
        }.onSuccess { targetPaths ->
            DumbService.getInstance(guideMark.project).smartInvokeLater {
                ProgressManager.getInstance().runProcess({
                    for (targetPath in targetPaths) {
                        if (targetPath.resolveToPsiElement() == guideMark.getPsiElement()) {
                            val endpointName = getEndpointName(targetPath.path)
                            val methodType = targetPath.methods.firstOrNull()

                            if (methodType == null) {
                                //no method type means all HTTP methods are supported
                                val detectedEndpoints = mutableListOf<DetectedEndpoint>()
                                for (methodType in httpMethods) {
                                    val fullEndpointName = "$methodType:$endpointName"
                                    log.info("Detected endpoint: $fullEndpointName")
                                    detectedEndpoints.add(DetectedEndpoint(fullEndpointName, false))
                                }
                                detectedEndpointPromise.complete(detectedEndpoints)
                            } else {
                                val fullEndpointName = "$methodType:$endpointName"
                                log.info("Detected endpoint: $fullEndpointName")
                                detectedEndpointPromise.complete(listOf(DetectedEndpoint(fullEndpointName, false)))
                            }
                            break
                        }
                    }
                    detectedEndpointPromise.tryComplete(emptyList())
                }, null)
            }
        }.onFailure {
            detectedEndpointPromise.fail(it)
        }
        return detectedEndpointPromise.future()
    }

    private fun getEndpointName(url: UrlPath): String {
        val urlPath = StringBuilder()
        url.segments.forEach {
            when (it) {
                is UrlPath.PathSegment.Exact -> urlPath.append("/").append(it.value)
                is UrlPath.PathSegment.Variable -> urlPath.append("/").append("{").append(it.variableName).append("}")

                else -> {
                    //todo: correctly handle this
                    urlPath.append("/").append("*")
                    log.warn("Unsupported url path segment type: ${it.javaClass}")
                }
            }
        }
        return urlPath.toString()
    }
}
