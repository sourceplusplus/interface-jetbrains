package com.sourceplusplus.plugin.intellij.portal

import com.sourceplusplus.portal.SourcePortal
import groovy.util.logging.Slf4j

/**
 * Used to display the Source++ Portal UI.
 *
 * @version 0.2.6
 * @since 0.2.5
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class IntelliJSourcePortal extends SourcePortal {

    private IntelliJSourcePortal(String portalUuid, String appUuid, boolean external) {
        super(portalUuid, appUuid, external)
    }

    static Optional<IntelliJSourcePortal> getInternalPortal(String appUuid, String artifactQualifiedName) {
        return Optional.ofNullable(portalMap.asMap().values().find {
            it.appUuid == appUuid && it.portalUI.viewingPortalArtifact == artifactQualifiedName && !it.external
        }) as Optional<IntelliJSourcePortal>
    }

    static List<IntelliJSourcePortal> getSimilarPortals(SourcePortal portal) {
        return portalMap.asMap().values().findAll {
            it.appUuid == portal.appUuid &&
                    it.portalUI.viewingPortalArtifact == portal.portalUI.viewingPortalArtifact &&
                    it.portalUI.currentTab == portal.portalUI.currentTab
        } as List<IntelliJSourcePortal>
    }

    static List<IntelliJSourcePortal> getExternalPortals() {
        return portalMap.asMap().values().findAll { it.external } as List<IntelliJSourcePortal>
    }

    static List<IntelliJSourcePortal> getExternalPortals(String appUuid) {
        return portalMap.asMap().values().findAll {
            it.appUuid == appUuid && it.external
        } as List<IntelliJSourcePortal>
    }

    static List<IntelliJSourcePortal> getExternalPortals(String appUuid, String artifactQualifiedName) {
        return portalMap.asMap().values().findAll {
            it.appUuid == appUuid && it.portalUI.viewingPortalArtifact == artifactQualifiedName && it.external
        } as List<IntelliJSourcePortal>
    }

    static List<IntelliJSourcePortal> getPortals(String appUuid, String artifactQualifiedName) {
        return portalMap.asMap().values().findAll {
            it.appUuid == appUuid && it.portalUI.viewingPortalArtifact == artifactQualifiedName
        } as List<IntelliJSourcePortal>
    }

    static String register(String appUuid, String artifactQualifiedName, boolean external) {
        def portalUuid = UUID.randomUUID().toString()
        def portal = new IntelliJSourcePortal(portalUuid, Objects.requireNonNull(appUuid), external)
        portal.portalUI = new IntelliJPortalUI(portalUuid, null)
        portal.portalUI.viewingPortalArtifact = Objects.requireNonNull(artifactQualifiedName)

        portalMap.put(portalUuid, portal)
        log.info("Registered Source++ Portal. Portal UUID: $portalUuid - App UUID: $appUuid - Artifact: $artifactQualifiedName")
        log.info("Active portals: " + portalMap.size())
        return portalUuid
    }

    static String register(String appUuid, String portalUuid, String artifactQualifiedName, IntelliJPortalUI portalUI) {
        def portal = new IntelliJSourcePortal(portalUuid, Objects.requireNonNull(appUuid), false)
        portal.portalUI = portalUI
        portal.portalUI.viewingPortalArtifact = Objects.requireNonNull(artifactQualifiedName)

        portalMap.put(portalUuid, portal)
        log.info("Registered Source++ Portal. Portal UUID: $portalUuid - App UUID: $appUuid - Artifact: $artifactQualifiedName")
        log.info("Active portals: " + portalMap.size())
        return portalUuid
    }

    static List<IntelliJSourcePortal> getPortals() {
        return new ArrayList<>(portalMap.asMap().values()) as List<IntelliJSourcePortal>
    }

    static IntelliJSourcePortal getPortal(String portalUuid) {
        return portalMap.getIfPresent(portalUuid) as IntelliJSourcePortal
    }

    IntelliJPortalUI getPortalUI() {
        return super.portalUI as IntelliJPortalUI
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void close() throws IOException {
        portalUI.close()
        super.close()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    SourcePortal createExternalPortal() {
        def portalClone = getPortal(register(appUuid, portalUI.viewingPortalArtifact, true))
        portalClone.portalUI.cloneUI(portalUI)
        return portalClone
    }
}
