package com.sourceplusplus.portal

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.sourceplusplus.portal.display.PortalUI
import groovy.transform.Canonical
import groovy.util.logging.Slf4j

import java.util.concurrent.TimeUnit

/**
 * todo: description
 *
 * @version 0.2.5
 * @since 0.2.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
@Canonical
class SourcePortal implements Closeable {

    protected static final LoadingCache<String, SourcePortal> portalMap = CacheBuilder.newBuilder()
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
    protected PortalUI portalUI

    protected SourcePortal(String portalUuid, String appUuid, boolean external) {
        this.portalUuid = portalUuid
        this.appUuid = appUuid
        this.external = external
    }

    static void ensurePortalActive(SourcePortal portal) {
        log.info("Keep alive portal: " + Objects.requireNonNull(portal).portalUuid)
        portalMap.refresh(portal.portalUuid)
        log.info("Active portals: " + portalMap.size())
    }

    static Optional<SourcePortal> getInternalPortal(String appUuid, String artifactQualifiedName) {
        return Optional.ofNullable(portalMap.asMap().values().find {
            it.appUuid == appUuid && it.portalUI.viewingPortalArtifact == artifactQualifiedName && !it.external
        })
    }

    static List<SourcePortal> getSimilarPortals(SourcePortal portal) {
        return portalMap.asMap().values().findAll {
            it.appUuid == portal.appUuid &&
                    it.portalUI.viewingPortalArtifact == portal.portalUI.viewingPortalArtifact &&
                    it.portalUI.currentTab == portal.portalUI.currentTab
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
            it.appUuid == appUuid && it.portalUI.viewingPortalArtifact == artifactQualifiedName && it.external
        }
    }

    static List<SourcePortal> getPortals(String appUuid, String artifactQualifiedName) {
        return portalMap.asMap().values().findAll {
            it.appUuid == appUuid && it.portalUI.viewingPortalArtifact == artifactQualifiedName
        }
    }

    static String register(String appUuid, String artifactQualifiedName, boolean external) {
        def portalUuid = UUID.randomUUID().toString()
        def portal = new SourcePortal(portalUuid, Objects.requireNonNull(appUuid), external)
        portal.portalUI = new PortalUI(portalUuid)
        portal.portalUI.viewingPortalArtifact = Objects.requireNonNull(artifactQualifiedName)

        portalMap.put(portalUuid, portal)
        log.info("Registered external Source++ Portal. Portal UUID: $portalUuid - App UUID: $appUuid - Artifact: $artifactQualifiedName")
        log.info("Active portals: " + portalMap.size())
        return portalUuid
    }

    static List<SourcePortal> getPortals() {
        return new ArrayList<>(portalMap.asMap().values())
    }

    static SourcePortal getPortal(String portalUuid) {
        return portalMap.getIfPresent(portalUuid)
    }

    PortalUI getPortalUI() {
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
        log.info("Closed portal: $portalUuid")
        portalMap.invalidate(portalUuid)
        log.info("Active portals: " + portalMap.size())
    }

    boolean isExternal() {
        return external
    }

    SourcePortal createExternalPortal() {
        def portalClone = getPortal(register(appUuid, portalUI.viewingPortalArtifact, true))
        portalClone.portalUI.cloneUI(portalUI)
        return portalClone
    }
}
