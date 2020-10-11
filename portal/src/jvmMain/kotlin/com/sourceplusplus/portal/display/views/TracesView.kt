package com.sourceplusplus.portal.display.views

import com.sourceplusplus.protocol.artifact.trace.TraceOrderType
import com.sourceplusplus.protocol.artifact.trace.TraceResult
import io.vertx.core.json.JsonArray
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.HashMap

/**
 * Holds the current view for the Traces portal tab.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class TracesView {

    var traceResultCache = ConcurrentHashMap<TraceOrderType, TraceResult>()
    var traceStacks = HashMap<String, JsonArray>() //todo: evicting cache
    var traceStack: JsonArray? = null
    var innerTraceStack = Stack<JsonArray>()
    var orderType = TraceOrderType.LATEST_TRACES
    var viewType = ViewType.TRACES
    var traceId: String? = null
    var spanId: Int = 0
    var viewTraceAmount = 10

    fun cacheArtifactTraceResult(artifactTraceResult: TraceResult) {
        val currentTraceResult = traceResultCache[artifactTraceResult.orderType]
        if (currentTraceResult != null) {
            val mergedArtifactTraceResult = artifactTraceResult.mergeWith(currentTraceResult)
            traceResultCache[mergedArtifactTraceResult.orderType] = mergedArtifactTraceResult.truncate(viewTraceAmount)
        } else {
            traceResultCache[artifactTraceResult.orderType] = artifactTraceResult.truncate(viewTraceAmount)
        }
    }

    val artifactTraceResult: TraceResult?
        get() = traceResultCache[orderType]

    fun cacheTraceStack(traceId: String, traceStack: JsonArray) {
        traceStacks[traceId] = traceStack
    }

    fun getTraceStack(traceId: String): JsonArray? {
        return traceStacks[traceId]
    }

    val innerTrace: Boolean
        get() = !innerTraceStack.isEmpty()

    fun cloneView(view: TracesView) {
        traceResultCache = ConcurrentHashMap(view.traceResultCache)
        traceStacks = HashMap(view.traceStacks)
        traceStack = if (view.traceStack != null) {
            JsonArray().addAll(view.traceStack)
        } else {
            null
        }

        view.innerTraceStack.reversed().forEach {
            innerTraceStack.push(it)
        }
        orderType = view.orderType
        viewType = view.viewType
        traceId = view.traceId
        spanId = view.spanId
    }

    companion object {
        enum class ViewType {
            TRACES, TRACE_STACK, SPAN_INFO
        }
    }
}
