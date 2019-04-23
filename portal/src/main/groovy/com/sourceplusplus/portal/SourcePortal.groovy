package com.sourceplusplus.portal

import com.sourceplusplus.portal.display.PortalInterface
import groovy.transform.Canonical

import java.util.concurrent.ConcurrentHashMap

/**
 * todo: description
 *
 * @version 0.2.0
 * @since 0.2.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Canonical
class SourcePortal implements Closeable {

    private static final Map<String, SourcePortal> portalMap = new ConcurrentHashMap<>()
    private final String portalUuid
    private final String appUuid
    private PortalInterface portalUI

    private SourcePortal(String portalUuid, String appUuid) {
        this.portalUuid = portalUuid
        this.appUuid = appUuid
    }

    static Optional<SourcePortal> getInternalPortal(String appUuid, String artifactQualifiedName) {
        return Optional.ofNullable(portalMap.values().find {
            it.appUuid == appUuid && it.interface.viewingPortalArtifact == artifactQualifiedName &&
                    !it.interface.externalPortal
        })
    }

    static List<SourcePortal> getExternalPortals(String appUuid, String artifactQualifiedName) {
        return portalMap.values().findAll {
            it.appUuid == appUuid && it.interface.viewingPortalArtifact == artifactQualifiedName &&
                    it.interface.externalPortal
        }
    }

    static List<SourcePortal> getPortals(String appUuid, String artifactQualifiedName) {
        return portalMap.values().findAll {
            it.appUuid == appUuid && it.interface.viewingPortalArtifact == artifactQualifiedName
        }
    }

    static String register(String appUuid, String artifactQualifiedName) {
        def portalUuid = UUID.randomUUID().toString()
        def portal = new SourcePortal(portalUuid, Objects.requireNonNull(appUuid))
        portal.portalUI = new PortalInterface(portalUuid)
        portal.portalUI.viewingPortalArtifact = Objects.requireNonNull(artifactQualifiedName)

        portalMap.put(portalUuid, portal)
        return portalUuid
    }

    static SourcePortal getPortal(String portalUuid) {
        return portalMap.get(portalUuid)
    }

    PortalInterface getInterface() {
        return portalUI
    }

    String getPortalUuid() {
        return portalUuid
    }

    String getAppUuid() {
        return appUuid
    }

    @Override
    void close() throws IOException {
        portalUI.close()
    }

    @Override
    SourcePortal clone() {
        return getPortal(register(appUuid, portalUI.viewingPortalArtifact))
    }
}
