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
package spp.jetbrains.monitor.skywalking.bridge

import spp.jetbrains.monitor.skywalking.SkywalkingClient
import spp.jetbrains.monitor.skywalking.model.GetEndpointTraces
import spp.jetbrains.monitor.skywalking.toProtocol
import spp.protocol.artifact.trace.Trace
import spp.protocol.artifact.trace.TraceResult
import spp.protocol.artifact.trace.TraceSpanStackQueryResult
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

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
                        start = Instant.fromEpochMilliseconds(request.zonedDuration.start.toInstant().toEpochMilli()),
                        stop = Instant.fromEpochMilliseconds(request.zonedDuration.start.toInstant().toEpochMilli()),
                        step = request.zonedDuration.step.name.toLowerCase(),
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
                    it.reply(
                        TraceSpanStackQueryResult(
                            traceSpans = traceStack.spans.map { it.toProtocol() },
                            total = traceStack.spans.size
                        )
                    )
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

        suspend fun getTraceStack(traceId: String, vertx: Vertx): TraceSpanStackQueryResult {
            return vertx.eventBus()
                .request<TraceSpanStackQueryResult>(getTraceStackAddress, traceId)
                .await().body()
        }
    }
}
