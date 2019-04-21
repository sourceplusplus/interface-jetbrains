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
    private final String appUuid
    private PortalUI portalUI

    private SourcePortal(int portalId, String appUuid) {
        this.portalId = portalId
        this.appUuid = appUuid
    }

    static List<SourcePortal> getPortals(String appUuid, String artifactQualifiedName) {
        return portalMap.values().findAll {
            it.appUuid == appUuid && it.portalUI.viewingPortalArtifact == artifactQualifiedName
        }
    }

    static int registerPortalId(String appUuid) {
        int portalId = portalIdIndex.incrementAndGet()
        def portal = new SourcePortal(portalId, appUuid)
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

    int getPortalId() {
        return portalId
    }

    String getAppUuid(){
        return appUuid
    }

    @Override
    void close() throws IOException {
        portalUI.close()
    }
}
