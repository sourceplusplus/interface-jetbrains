package com.sourceplusplus.portal.display.tabs.views

import com.sourceplusplus.api.model.trace.ArtifactTraceResult
import com.sourceplusplus.api.model.trace.TraceOrderType
import com.sourceplusplus.portal.display.PortalInterface
import groovy.transform.Canonical
import io.vertx.core.json.JsonArray

import java.util.concurrent.ConcurrentHashMap

/**
 * todo: description
 *
 * @version 0.2.0
 * @since 0.2.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Canonical
class TracesView {

    private final Map<TraceOrderType, ArtifactTraceResult> traceResultCache = new ConcurrentHashMap<>()
    private final Map<String, JsonArray> traceStacks = new HashMap<>() //todo: evicting cache
    private final PortalInterface portalInterface
    int innerLevel = 0
    boolean innerTrace
    String rootArtifactQualifiedName
    JsonArray innerTraceStack
    TraceOrderType orderType = TraceOrderType.LATEST_TRACES

    TracesView(PortalInterface portalInterface) {
        this.portalInterface = portalInterface
    }

    void cacheArtifactTraceResult(ArtifactTraceResult artifactTraceResult) {
        traceResultCache.put(artifactTraceResult.orderType(), artifactTraceResult)
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
}
