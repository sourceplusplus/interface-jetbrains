package com.sourceplusplus.tooltip.display.tabs.representation

import com.sourceplusplus.api.model.trace.ArtifactTraceResult
import groovy.transform.Canonical
import io.vertx.core.json.JsonArray

@Canonical
class TracesTabRepresentation {

    int innerLevel = 0
    boolean innerTrace
    String rootArtifactQualifiedName
    //JsonArray traceStack
    ArtifactTraceResult artifactTraceResult
    JsonArray innerTraceStack
    private final Map<String, JsonArray> traceStacks = new HashMap<>() //todo: evicting cache


    void cacheTraceStack(String traceId, JsonArray traceStack) {
        traceStacks.put(traceId, traceStack)
    }

    JsonArray getTraceStack(String traceId) {
        return traceStacks.get(traceId)
    }
}
