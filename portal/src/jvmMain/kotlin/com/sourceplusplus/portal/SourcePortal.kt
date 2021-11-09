package com.sourceplusplus.portal

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.sourceplusplus.portal.display.views.*
import spp.protocol.advice.ArtifactAdvice
import spp.protocol.portal.PortalConfiguration
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.util.*

/**
 * Represents a view into a specific source artifact.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SourcePortal(
    val portalUuid: String,
    val configuration: PortalConfiguration
) : Closeable {

    var visible: Boolean = false
    val overviewView: OverviewView = OverviewView()
    val activityView: ActivityView = ActivityView(this)
    val tracesView: TracesView = TracesView(this)
    val logsView: LogsView = LogsView(this)
    val configurationView: ConfigurationView = ConfigurationView()
    lateinit var viewingPortalArtifact: String
    var advice: MutableList<ArtifactAdvice> = mutableListOf()
    //todo: portal should be able to fetch advice for an artifact instead of storing it

    fun cloneViews(portal: SourcePortal) {
        this.overviewView.cloneView(portal.overviewView)
        this.activityView.cloneView(portal.activityView)
        this.tracesView.cloneView(portal.tracesView)
        this.logsView.cloneView(portal.logsView)
        this.configurationView.cloneView(portal.configurationView)
    }

    override fun close() {
        log.info("Closed portal: $portalUuid - Artifact: $viewingPortalArtifact")
        portalMap.invalidate(portalUuid)
        //todo: de-register portal consumers
        log.debug("Active portals: " + portalMap.size())
    }

    fun createExternalPortal(): SourcePortal {
        val portalClone = getPortal(register(viewingPortalArtifact, configuration.copy(external = true)))!!
        portalClone.cloneViews(this)
        return portalClone
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SourcePortal) return false
        if (portalUuid != other.portalUuid) return false
        return true
    }

    override fun hashCode(): Int {
        return portalUuid.hashCode()
    }

    companion object {
        private val log = LoggerFactory.getLogger(SourcePortal::class.java)

        val portalMap: LoadingCache<String, SourcePortal> = CacheBuilder.newBuilder()
//            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build(object : CacheLoader<String, SourcePortal>() {
                override fun load(portalUuid: String): SourcePortal? {
                    return getPortal(portalUuid)
                }
            })

        fun ensurePortalActive(portal: SourcePortal) {
            log.debug("Keep alive portal: " + Objects.requireNonNull(portal).portalUuid)
            portalMap.refresh(portal.portalUuid)
            log.debug("Active portals: " + portalMap.size())
        }

        fun getInternalPortal(artifactQualifiedName: String): Optional<SourcePortal> {
            return Optional.ofNullable(portalMap.asMap().values.find {
                it.viewingPortalArtifact == artifactQualifiedName && !it.configuration.external
            })
        }

        fun getSimilarPortals(portal: SourcePortal): List<SourcePortal> {
            return portalMap.asMap().values.filter {
                it.viewingPortalArtifact == portal.viewingPortalArtifact &&
                        it.configuration.currentPage == portal.configuration.currentPage
            }
        }

        fun getExternalPortals(): List<SourcePortal> {
            return portalMap.asMap().values.filter { it.configuration.external }
        }

        fun getExternalPortals(artifactQualifiedName: String): List<SourcePortal> {
            return portalMap.asMap().values.filter {
                it.viewingPortalArtifact == artifactQualifiedName && it.configuration.external
            }
        }

        fun getPortals(artifactQualifiedName: String): List<SourcePortal> {
            return portalMap.asMap().values.filter {
                it.viewingPortalArtifact == artifactQualifiedName
            }
        }

        fun register(artifactQualifiedName: String, external: Boolean): String {
            return register(
                UUID.randomUUID().toString(), artifactQualifiedName, PortalConfiguration(external = external)
            )
        }

        fun register(
            artifactQualifiedName: String,
            configuration: PortalConfiguration = PortalConfiguration()
        ): String {
            return register(UUID.randomUUID().toString(), artifactQualifiedName, configuration)
        }

        fun register(
            portalUuid: String,
            artifactQualifiedName: String,
            configuration: PortalConfiguration = PortalConfiguration()
        ): String {
            val portal = SourcePortal(portalUuid, configuration)
            portal.viewingPortalArtifact = Objects.requireNonNull(artifactQualifiedName)

            portalMap.put(portalUuid, portal)
            log.info(
                "Registered SourceMarker Portal. Portal UUID: {} - Artifact: {}",
                portalUuid, artifactQualifiedName
            )
            log.debug("Active portals: " + portalMap.size())
            return portalUuid
        }

        fun getPortals(): List<SourcePortal> {
            return ArrayList(portalMap.asMap().values)
        }

        fun getPortal(portalUuid: String): SourcePortal? {
            return portalMap.getIfPresent(portalUuid)
        }
    }
}
