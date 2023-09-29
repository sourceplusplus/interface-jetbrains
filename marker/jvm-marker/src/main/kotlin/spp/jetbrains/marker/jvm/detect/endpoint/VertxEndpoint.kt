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
package spp.jetbrains.marker.jvm.detect.endpoint

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.Key
import com.intellij.psi.util.descendants
import com.intellij.psi.util.findParentInFile
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.http.HttpMethod
import spp.jetbrains.artifact.model.*
import spp.jetbrains.artifact.service.ArtifactScopeService
import spp.jetbrains.artifact.service.toArtifact
import spp.jetbrains.marker.jvm.detect.JVMEndpointDetector
import spp.jetbrains.marker.service.getFullyQualifiedName
import spp.jetbrains.marker.source.info.EndpointDetector
import spp.jetbrains.marker.source.mark.guide.GuideMark
import spp.jetbrains.marker.source.mark.guide.MethodGuideMark

class VertxEndpoint : JVMEndpointDetector.JVMEndpointNameDetector {

    companion object {
        private val log = logger<VertxEndpoint>()
        private val httpMethods = HttpMethod.values().map { it.name() }.toSet()
        private val DETECTED_ENDPOINT = Key.create<EndpointDetector.DetectedEndpoint>("VertxEndpoint.DetectedEndpoint")
    }

    override fun detectEndpointNames(guideMark: GuideMark): Future<List<EndpointDetector.DetectedEndpoint>> {
        if (guideMark !is MethodGuideMark) {
            return Future.succeededFuture(emptyList())
        }

        val promise = Promise.promise<List<EndpointDetector.DetectedEndpoint>>()
        ApplicationManager.getApplication().runReadAction {
            val artifact = guideMark.getPsiElement().toArtifact()
            if (artifact !is FunctionArtifact) {
                promise.complete(emptyList())
                return@runReadAction
            }

            var fallbackSearch = false
            val callers = mutableListOf<EndpointDetector.DetectedEndpoint>()
            val callerExpressions = try {
                artifact.getCallerExpressions()
            } catch (e: IllegalArgumentException) {
                log.warn("Failed to get caller expressions for ${artifact.getFullyQualifiedName()}. Reason: $e")
                fallbackSearch = true
                emptyList()
            }
            callerExpressions.forEach {
                it.findParentInFile {
                    val innerArtifact = it.toArtifact()
                    if (innerArtifact !is CallArtifact) return@findParentInFile false
                    check(innerArtifact)?.let { callers.add(it) }
                    false
                }
            }

            if (fallbackSearch) {
                //won't be able to search for references in files outside current file
                //instead use regex to search for calls to router.post, router.get, etc
                if (artifact.psiElement.getUserData(DETECTED_ENDPOINT) != null) {
                    callers.add(artifact.psiElement.getUserData(DETECTED_ENDPOINT)!!)
                } else {
                    artifact.getCalls().forEach { checkSimple(it) }
                }
            }

            promise.complete(callers.toSet().toList())
        }

        return promise.future()
    }

    private fun checkSimple(artifact: CallArtifact) {
        val importRegex = Regex("""import io\.vertx""")
        importRegex.find(artifact.containingFile.text) ?: return

        val regex = Regex("""router\.([a-zA-Z]+)\("([^"]+)"\)\.handler\(this::([a-zA-Z]+)\)""")
        val match = regex.matchEntire(artifact.text) ?: return
        val httpMethod = match.groupValues[1].uppercase()
        val endpointName = match.groupValues[2]
        val referenceMethod = match.groupValues[3]

        val fileFunctions = ArtifactScopeService.getFunctions(artifact.containingFile)
        val refFunction = fileFunctions.firstOrNull { it.name == referenceMethod } ?: return

        val endpoint = EndpointDetector.DetectedEndpoint(
            "$httpMethod:$endpointName",
            false,
            endpointName,
            httpMethod
        )
        refFunction.putUserData(DETECTED_ENDPOINT, endpoint)
        log.info("Detected endpoint: $endpoint")
    }

    private fun check(artifact: CallArtifact): EndpointDetector.DetectedEndpoint? {
        val routerCall = getRouterCall(artifact) ?: return null
        val endpointType = routerCall.getName()!!.uppercase().substringAfter(".")
        val callee = routerCall.getResolvedFunction() ?: return null
        val calleeReceiver = callee.getFullyQualifiedName().toClass()?.identifier
        if (calleeReceiver != "io.vertx.ext.web.Router") {
            return null
        }

        val args = routerCall.getArguments()
        val functionReference = args.firstOrNull()
        if (functionReference == null || functionReference !is ArtifactLiteralValue) {
            return null
        }
        val endpointName = functionReference.value?.toString() ?: return null

        val endpoint = EndpointDetector.DetectedEndpoint(
            "$endpointType:$endpointName",
            false,
            endpointName,
            endpointType
        )
        log.info("Detected endpoint: $endpoint")
        return endpoint
    }

    private fun getRouterCall(artifact: CallArtifact): CallArtifact? {
        var result: CallArtifact? = null
        artifact.findParentInFile {
            it.descendants { true }.mapNotNull { it.toArtifact() }.filterIsInstance<CallArtifact>().forEach {
                val name = it.getName().toString().uppercase().substringAfter(".")
                if (httpMethods.contains(name)) {
                    result = it
                    return@findParentInFile true
                }
            }
            return@findParentInFile false
        }
        return result
    }
}
