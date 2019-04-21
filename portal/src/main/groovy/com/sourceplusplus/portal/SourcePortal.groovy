package com.sourceplusplus.portal

import com.sourceplusplus.portal.display.PortalUI
import groovy.transform.Canonical

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Canonical
class SourcePortal implements Closeable {

    private static final Map<Integer, PortalUI> portalUIMap = new ConcurrentHashMap<>()
    private static final AtomicInteger portalIdIndex = new AtomicInteger()
    int portalId
    PortalUI portalUI

    static int registerPortalId() {
        int portalId = portalIdIndex.incrementAndGet()
        portalUIMap.put(portalId, new PortalUI())
        return portalId
    }

    static SourcePortal getPortal(int portalId) {

    }

    @Override
    void close() throws IOException {
        portalUI.close()
    }
}
