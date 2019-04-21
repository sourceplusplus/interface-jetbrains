package com.sourceplusplus.portal

import com.sourceplusplus.portal.display.PortalUI
import groovy.transform.Canonical

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Canonical
class SourcePortal implements Closeable {

    private static final Map<Integer, SourcePortal> portalMap = new ConcurrentHashMap<>()
    private static final AtomicInteger portalIdIndex = new AtomicInteger()
    private final int portalId
    private PortalUI portalUI

    private SourcePortal(int portalId) {
        this.portalId = portalId
    }

    static int registerPortalId() {
        int portalId = portalIdIndex.incrementAndGet()
        def portal = new SourcePortal(portalId)
        portal.portalUI = new PortalUI(portalId)

        portalMap.put(portalId, portal)
        return portalId
    }

    static SourcePortal getPortal(int portalId) {
        return portalMap.get(portalId)
    }

    PortalUI getPortalUI() {
        return portalUI
    }

    @Override
    void close() throws IOException {
        portalUI.close()
    }
}
