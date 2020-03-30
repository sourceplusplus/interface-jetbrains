package com.sourceplusplus.portal

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.intellij.ui.jcef.JBCefBrowser
import com.sourceplusplus.portal.display.PortalInterface
import groovy.transform.Canonical
import groovy.util.logging.Slf4j

import java.util.concurrent.TimeUnit

/**
 * todo: description
 *
 * @version 0.2.4
 * @since 0.2.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
@Canonical
class SourcePortal implements Closeable {

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

    static void registerInternal(String appUuid, String portalUuid, String artifactQualifiedName, JBCefBrowser browser) {
        //todo: ensure portal uuid valid
        def portal = new SourcePortal(portalUuid, Objects.requireNonNull(appUuid), false)
        portal.portalUI = new PortalInterface(portalUuid, browser)
        portal.portalUI.viewingPortalArtifact = Objects.requireNonNull(artifactQualifiedName)

        portalMap.put(portalUuid, portal)
        log.info("Registered internal Source++ Portal. Portal UUID: $portalUuid - App UUID: $appUuid - Artifact: $artifactQualifiedName")
        log.info("Active portals: " + portalMap.size())
    }

    static String register(String appUuid, String artifactQualifiedName, boolean external) {
        def portalUuid = UUID.randomUUID().toString()
        def portal = new SourcePortal(portalUuid, Objects.requireNonNull(appUuid), external)
        portal.portalUI = new PortalInterface(portalUuid)
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
        log.info("Closed portal: $portalUuid")
        portalMap.invalidate(portalUuid)
        portalUI.close()
        log.info("Active portals: " + portalMap.size())
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
