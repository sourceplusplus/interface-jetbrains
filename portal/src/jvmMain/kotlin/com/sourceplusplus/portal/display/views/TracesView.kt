package com.sourceplusplus.portal.display.views

import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.portal.model.TraceDisplayType
import spp.protocol.artifact.trace.TraceOrderType
import spp.protocol.artifact.trace.TraceResult
import spp.protocol.artifact.trace.TraceStack
import spp.protocol.artifact.trace.TraceStackPath
import java.util.concurrent.ConcurrentHashMap

/**
 * Holds the current view for the Traces portal tab.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class TracesView(
    val portal: SourcePortal
) {

    var traceResultCache = ConcurrentHashMap<TraceOrderType, TraceResult>()
    var traceStacks = HashMap<String, TraceStack>() //todo: evicting cache
    var traceStack: TraceStack? = null
    var orderType = TraceOrderType.LATEST_TRACES
    var viewType = TraceDisplayType.TRACES
    var traceId: String? = null
    var traceStackPath: TraceStackPath? = null
    var spanId: Int = 0
    val viewTraceAmount = if (portal.configuration.external) 20 else 10
    var innerTraceStack = false
    var rootArtifactQualifiedName: String? = null
    var pageNumber = 1
    var resolvedEndpointNames = HashMap<String, String>() //todo: evicting cache
    var localTracing: Boolean = false

    fun cacheArtifactTraceResult(artifactTraceResult: TraceResult) {
        var cacheResult = artifactTraceResult
        if (resolvedEndpointNames.isNotEmpty()) {
            cacheResult = artifactTraceResult.copy(
                traces = artifactTraceResult.traces.map {
                    if (resolvedEndpointNames.containsKey(it.traceIds[0])) {
                        it.copy(operationNames = listOf(resolvedEndpointNames[it.traceIds[0]]!!))
                    } else {
                        it
                    }
                }
            )
        }

        val currentTraceResult = traceResultCache[cacheResult.orderType]
        if (currentTraceResult != null) {
            val mergedArtifactTraceResult = cacheResult.mergeWith(currentTraceResult)
            if (pageNumber == 1) {
                traceResultCache[mergedArtifactTraceResult.orderType] =
                    mergedArtifactTraceResult.truncate(viewTraceAmount)
            } else {
                traceResultCache[mergedArtifactTraceResult.orderType] = mergedArtifactTraceResult
            }
        } else {
            traceResultCache[cacheResult.orderType] = cacheResult.truncate(viewTraceAmount)
        }
    }

    val artifactTraceResult: TraceResult?
        get() = traceResultCache[orderType]

    fun cacheTraceStack(traceId: String, traceStack: TraceStack) {
        traceStacks[traceId] = traceStack
    }

    fun getTraceStack(traceId: String): TraceStack? {
        return traceStacks[traceId]
    }

    fun cloneView(view: TracesView) {
        traceResultCache = ConcurrentHashMap(view.traceResultCache)
        traceStacks = HashMap(view.traceStacks)
        traceStack = if (view.traceStack != null) {
            view.traceStack!!.copy(traceSpans = view.traceStack!!.traceSpans.toList())
        } else {
            null
        }
        traceStackPath = if (view.traceStackPath != null) {
            view.traceStackPath!!.copy(path = view.traceStackPath!!.path.toMutableList())
        } else {
            null
        }
        orderType = view.orderType
        viewType = view.viewType
        traceId = view.traceId
        spanId = view.spanId
        resolvedEndpointNames = HashMap(view.resolvedEndpointNames)
    }
}
