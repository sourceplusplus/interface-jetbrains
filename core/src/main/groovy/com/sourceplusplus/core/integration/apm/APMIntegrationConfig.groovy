package com.sourceplusplus.core.integration.apm

import com.sourceplusplus.core.storage.CoreConfig
import groovy.transform.EqualsAndHashCode

import java.time.Instant

/**
 * Persistent configuration for the APM integration.
 *
 * @version 0.3.0
 * @since 0.3.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class APMIntegrationConfig {

    private static CoreConfig _coreConfig
    private Set<SourceService> sourceServices = new HashSet<>()
    private EndpointDetection endpointDetection = new EndpointDetection()
    private FailedArtifactTracker failedArtifactTracker = new FailedArtifactTracker()

    @EqualsAndHashCode(includeFields = true, cache = true)
    static class SourceService {
        private String id
        private String appUuid

        SourceService(String id, String appUuid) {
            this.id = id
            this.appUuid = appUuid
        }

        String getId() {
            return id
        }

        String getAppUuid() {
            return appUuid
        }
    }

    static class EndpointDetection {
        private Instant latestSearchedService

        Instant getLatestSearchedService() {
            return latestSearchedService
        }

        void setLatestSearchedService(Instant latestSearchedService) {
            this.latestSearchedService = latestSearchedService
            _coreConfig.save()
        }
    }

    static class FailedArtifactTracker {
        private Instant latestSearchedService
        private Map<SourceService, Instant> serviceLatestSearchedFailingTraces = new HashMap<>()

        Instant getLatestSearchedService() {
            return latestSearchedService
        }

        void setLatestSearchedService(Instant latestSearchedService) {
            this.latestSearchedService = latestSearchedService
            _coreConfig.save()
        }

        Map<SourceService, Instant> getServiceLatestSearchedFailingTraces() {
            return serviceLatestSearchedFailingTraces
        }

        void addServiceLatestSearchedFailingTraces(SourceService sourceService, Instant latestSearchedFailingTraces) {
            serviceLatestSearchedFailingTraces.put(sourceService, latestSearchedFailingTraces)
            _coreConfig.save()
        }
    }

    void addSourceService(SourceService service) {
        if (this.sourceServices.add(service)) {
            _coreConfig.save()
        }
    }

    Set<SourceService> getSourceServices() {
        return sourceServices
    }

    EndpointDetection getEndpointDetection() {
        return endpointDetection
    }

    FailedArtifactTracker getFailedArtifactTracker() {
        return failedArtifactTracker
    }

    static void setupCoreConfig(CoreConfig coreConfig) {
        _coreConfig = coreConfig
    }
}