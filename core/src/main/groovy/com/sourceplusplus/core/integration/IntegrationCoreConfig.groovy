package com.sourceplusplus.core.integration

import com.sourceplusplus.api.model.integration.IntegrationInfo
import com.sourceplusplus.core.integration.apm.APMIntegrationConfig
import com.sourceplusplus.core.storage.CoreConfig

/**
 * Persistent configuration for the core integrations.
 *
 * @version 0.3.0
 * @since 0.3.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class IntegrationCoreConfig {

    private Set<IntegrationInfo> integrations = new HashSet<>()
    private APMIntegrationConfig apmIntegrationConfig = new APMIntegrationConfig()

    APMIntegrationConfig getApmIntegrationConfig() {
        return apmIntegrationConfig
    }

    void setApmIntegrationConfig(APMIntegrationConfig apmIntegrationConfig) {
        this.apmIntegrationConfig = apmIntegrationConfig
        CoreConfig.INSTANCE?.save()
    }

    Set<IntegrationInfo> getIntegrations() {
        return integrations
    }

    void updateIntegration(IntegrationInfo integrationInfo) {
        integrations.add(integrationInfo)
        CoreConfig.INSTANCE?.save()
    }
}
