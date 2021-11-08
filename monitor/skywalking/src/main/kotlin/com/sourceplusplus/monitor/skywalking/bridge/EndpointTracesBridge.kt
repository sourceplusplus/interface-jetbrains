package com.sourceplusplus.monitor.skywalking.bridge

import com.sourceplusplus.monitor.skywalking.SkywalkingClient
import com.sourceplusplus.monitor.skywalking.model.GetEndpointTraces
import com.sourceplusplus.monitor.skywalking.toProtocol
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
