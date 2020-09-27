package com.sourceplusplus.monitor.skywalking.track

import com.sourceplusplus.monitor.skywalking.SkywalkingClient
import com.sourceplusplus.monitor.skywalking.model.GetEndpointTraces
import com.sourceplusplus.monitor.skywalking.toProtocol
import com.sourceplusplus.protocol.artifact.trace.Trace
import com.sourceplusplus.protocol.artifact.trace.TraceResult
import com.sourceplusplus.protocol.artifact.trace.TraceSpanStackQueryResult
import io.vertx.core.Vertx
import io.vertx.kotlin.core.eventbus.requestAwait
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class EndpointTracesTracker(private val skywalkingClient: SkywalkingClient) : CoroutineVerticle() {

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
                        appUuid = request.appUuid,
                        artifactQualifiedName = request.artifactQualifiedName,
                        orderType = request.orderType,
                        start = request.zonedDuration.start.toInstant().toEpochMilli(),
                        stop = request.zonedDuration.start.toInstant().toEpochMilli(),
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
                .requestAwait<TraceResult>(getTracesAddress, request)
                .body()
        }

        suspend fun getTraceStack(traceId: String, vertx: Vertx): TraceSpanStackQueryResult {
            return vertx.eventBus()
                .requestAwait<TraceSpanStackQueryResult>(getTraceStackAddress, traceId)
                .body()
        }
    }
}
