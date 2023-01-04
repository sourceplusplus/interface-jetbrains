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
package spp.jetbrains.monitor.skywalking.bridge

import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import spp.jetbrains.monitor.skywalking.SkywalkingClient
import spp.jetbrains.monitor.skywalking.model.GetEndpointTraces
import spp.jetbrains.monitor.skywalking.toProtocol
import spp.protocol.artifact.trace.Trace
import spp.protocol.artifact.trace.TraceResult
import spp.protocol.artifact.trace.TraceSpan

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("MagicNumber")
class EndpointTracesBridge(private val skywalkingClient: SkywalkingClient) : CoroutineVerticle() {

    override suspend fun start() {
        vertx.eventBus().localConsumer<GetEndpointTraces>(getTracesAddress) {
            launch(vertx.dispatcher()) {
                val request = it.body()
                val traces = skywalkingClient.queryBasicTraces(request)
                val traceStack = mutableListOf<Trace>()
                if (traces != null) {
                    traceStack.addAll(traces.traces.map { it.toProtocol() })
                }
                it.reply(
                    TraceResult(
                        artifactQualifiedName = request.artifactQualifiedName,
                        orderType = request.orderType,
                        start = request.zonedDuration.start.toInstant(),
                        stop = request.zonedDuration.start.toInstant(),
                        step = request.zonedDuration.step.name.lowercase(),
                        total = traceStack.size,
                        traces = traceStack
                    )
                )
            }
        }
        vertx.eventBus().localConsumer<String>(getTraceStackAddress) {
            launch(vertx.dispatcher()) {
                val traceStack = skywalkingClient.queryTraceStack(it.body())
                if (traceStack != null) {
                    it.reply(traceStack.spans.map { it.toProtocol() })
                } else {
                    it.reply(null)
                }
            }
        }
    }

    companion object {
        private const val rootAddress = "monitor.skywalking.endpoint.traces"
        private const val getTracesAddress = "$rootAddress.getTraces"
        private const val getTraceStackAddress = "$rootAddress.getTraceStack"

        suspend fun getTraces(request: GetEndpointTraces, vertx: Vertx): TraceResult {
            return vertx.eventBus()
                .request<TraceResult>(getTracesAddress, request)
                .await().body()
        }

        suspend fun getTraceStack(traceId: String, vertx: Vertx): List<TraceSpan> {
            return vertx.eventBus()
                .request<List<TraceSpan>>(getTraceStackAddress, traceId)
                .await().body()
        }
    }
}
