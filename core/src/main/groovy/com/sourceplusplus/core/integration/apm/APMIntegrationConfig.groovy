package com.sourceplusplus.core.integration.apm

import com.sourceplusplus.core.storage.CoreConfig
import groovy.transform.EqualsAndHashCode

import java.time.Instant

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
        private Instant latestSearchedServiceInstance

        Instant getLatestSearchedService() {
            return latestSearchedService
        }

        void setLatestSearchedService(Instant latestSearchedService) {
            this.latestSearchedService = latestSearchedService
            _coreConfig.save()
        }

        Instant getLatestSearchedServiceInstance() {
            return latestSearchedServiceInstance
        }

        void setLatestSearchedServiceInstance(Instant latestSearchedServiceInstance) {
            this.latestSearchedServiceInstance = latestSearchedServiceInstance
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