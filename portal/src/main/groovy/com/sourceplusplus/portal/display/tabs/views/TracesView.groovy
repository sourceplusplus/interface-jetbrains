package com.sourceplusplus.portal.display.tabs.views

import com.sourceplusplus.api.model.trace.ArtifactTraceResult
import com.sourceplusplus.api.model.trace.TraceOrderType
import groovy.transform.Canonical
import io.vertx.core.json.JsonArray

import java.util.concurrent.ConcurrentHashMap

/**
 * Holds the current view for the Traces portal tab.
 *
 * @version 0.3.1
 * @since 0.2.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Canonical
class TracesView {

    private Map<TraceOrderType, ArtifactTraceResult> traceResultCache = new ConcurrentHashMap<>()
    private Map<String, JsonArray> traceStacks = new HashMap<>() //todo: evicting cache
    JsonArray traceStack
    Stack<JsonArray> innerTraceStack = new Stack<>()
    TraceOrderType orderType = TraceOrderType.LATEST_TRACES
    ViewType viewType = ViewType.TRACES
    String traceId
    int spanId
    int viewTraceAmount = 10

    void cacheArtifactTraceResult(ArtifactTraceResult artifactTraceResult) {
        def currentTraceResult = traceResultCache.get(artifactTraceResult.orderType())
        if (currentTraceResult) {
            artifactTraceResult = artifactTraceResult.mergeWith(currentTraceResult)
        }
        traceResultCache.put(artifactTraceResult.orderType(), artifactTraceResult.truncate(viewTraceAmount))
    }

    ArtifactTraceResult getArtifactTraceResult() {
        return traceResultCache.get(orderType)
    }

    void cacheTraceStack(String traceId, JsonArray traceStack) {
        traceStacks.put(traceId, traceStack)
    }

    JsonArray getTraceStack(String traceId) {
        return traceStacks.get(traceId)
    }

    boolean getInnerTrace() {
        return !innerTraceStack.isEmpty()
    }

    void cloneView(TracesView view) {
        traceResultCache = new ConcurrentHashMap<>(view.traceResultCache)
        traceStacks = new HashMap<>(view.traceStacks)
        if (view.traceStack) {
            traceStack = new JsonArray().addAll(view.traceStack)
        } else {
            traceStack = null
        }

        view.innerTraceStack.reverse().each {
            innerTraceStack.push(it)
        }
        orderType = view.orderType
        viewType = view.viewType
        traceId = view.traceId
        spanId = view.spanId
    }

    static enum ViewType {
        TRACES, TRACE_STACK, SPAN_INFO
    }
}
