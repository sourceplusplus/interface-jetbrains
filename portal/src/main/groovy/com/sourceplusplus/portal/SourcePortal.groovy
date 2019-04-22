package com.sourceplusplus.portal

import com.sourceplusplus.portal.display.PortalInterface
import groovy.transform.Canonical

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * todo: description
 *
 * @version 0.2.0
 * @since 0.2.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Canonical
class SourcePortal implements Closeable {

    private static final Map<Integer, SourcePortal> portalMap = new ConcurrentHashMap<>()
    private static final AtomicInteger portalIdIndex = new AtomicInteger()
    private final int portalId
    private final String appUuid
    private PortalInterface portalUI

    private SourcePortal(int portalId, String appUuid) {
        this.portalId = portalId
        this.appUuid = appUuid
    }

    static List<SourcePortal> getPortals(String appUuid, String artifactQualifiedName) {
        return portalMap.values().findAll {
            it.appUuid == appUuid && it.interface.viewingPortalArtifact == artifactQualifiedName
        }
    }

    static int registerPortalId(String appUuid) {
        int portalId = portalIdIndex.incrementAndGet()
        def portal = new SourcePortal(portalId, appUuid)
        portal.portalUI = new PortalInterface(portalId)

        portalMap.put(portalId, portal)
        return portalId
    }

    static SourcePortal getPortal(int portalId) {
        return portalMap.get(portalId)
    }

    PortalInterface getInterface() {
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
