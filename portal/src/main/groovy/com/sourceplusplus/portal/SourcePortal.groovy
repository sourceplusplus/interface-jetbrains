package com.sourceplusplus.portal

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.sourceplusplus.portal.display.PortalInterface
import groovy.transform.Canonical
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.TimeUnit

/**
 * todo: description
 *
 * @version 0.2.1
 * @since 0.2.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Canonical
class SourcePortal implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(this.name)

    private static final LoadingCache<String, SourcePortal> portalMap = CacheBuilder.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build(new CacheLoader<String, SourcePortal>() {
        @Override
        SourcePortal load(String portalUuid) throws Exception {
            return getPortal(portalUuid)
        }
    })
    private final String portalUuid
    private final String appUuid
    private final boolean external
    private PortalInterface portalUI

    private SourcePortal(String portalUuid, String appUuid, boolean external) {
        this.portalUuid = portalUuid
        this.appUuid = appUuid
        this.external = external
    }

    static void ensurePortalActive(SourcePortal portal) {
        log.info("Keep alive portal: " + Objects.requireNonNull(portal).portalUuid)
        portalMap.refresh(portal.portalUuid)
        log.info("Active portals: " + portalMap.size())
    }

    static void destroyPortal(String portalUuid) {
        log.info("Destroying portal: " + portalUuid)
        def portal = portalMap.get(portalUuid)
        if (portal) {
            portal.close()
            portalMap.invalidate(portalUuid)
        }
        log.info("Active portals: " + portalMap.size())
    }

    static Optional<SourcePortal> getInternalPortal(String appUuid, String artifactQualifiedName) {
        return Optional.ofNullable(portalMap.asMap().values().find {
            it.appUuid == appUuid && it.interface.viewingPortalArtifact == artifactQualifiedName && !it.external
        })
    }

    static List<SourcePortal> getSimilarPortals(SourcePortal portal) {
        return portalMap.asMap().values().findAll {
            it.appUuid == portal.appUuid &&
                    it.interface.viewingPortalArtifact == portal.interface.viewingPortalArtifact &&
                    it.interface.currentTab == portal.interface.currentTab
        }
    }

    static List<SourcePortal> getExternalPortals() {
        return portalMap.asMap().values().findAll { it.external }
    }

    static List<SourcePortal> getExternalPortals(String appUuid) {
        return portalMap.asMap().values().findAll {
            it.appUuid == appUuid && it.external
        }
    }

    static List<SourcePortal> getExternalPortals(String appUuid, String artifactQualifiedName) {
        return portalMap.asMap().values().findAll {
            it.appUuid == appUuid && it.interface.viewingPortalArtifact == artifactQualifiedName && it.external
        }
    }

    static List<SourcePortal> getPortals(String appUuid, String artifactQualifiedName) {
        return portalMap.asMap().values().findAll {
            it.appUuid == appUuid && it.interface.viewingPortalArtifact == artifactQualifiedName
        }
    }

    static String register(String appUuid, String artifactQualifiedName, boolean external) {
        def portalUuid = UUID.randomUUID().toString()
        def portal = new SourcePortal(portalUuid, Objects.requireNonNull(appUuid), external)
        portal.portalUI = new PortalInterface(portalUuid)
        portal.portalUI.viewingPortalArtifact = Objects.requireNonNull(artifactQualifiedName)

        portalMap.put(portalUuid, portal)
        log.info("Registered new Source++ Portal. Portal UUID: $portalUuid - App UUID: $appUuid - Artifact: $artifactQualifiedName")
        log.info("Active portals: " + portalMap.size())
        return portalUuid
    }

    static List<SourcePortal> getPortals() {
        return new ArrayList<>(portalMap.asMap().values())
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

    boolean isExternal() {
        return external
    }

    SourcePortal createExternalPortal() {
        def portalClone = getPortal(register(appUuid, portalUI.viewingPortalArtifact, true))
        portalClone.portalUI.cloneViews(this.interface)
        return portalClone
    }
}
