package com.sourceplusplus.core.storage

import com.sourceplusplus.core.integration.apm.APMIntegrationConfig
import groovy.util.logging.Slf4j
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject

@Slf4j
class CoreConfig {

    public static CoreConfig INSTANCE
    private static SourceStorage _storage
    private APMIntegrationConfig apmIntegrationConfig

    APMIntegrationConfig getApmIntegrationConfig() {
        return apmIntegrationConfig
    }

    void setApmIntegrationConfig(APMIntegrationConfig apmIntegrationConfig) {
        this.apmIntegrationConfig = apmIntegrationConfig
        save()
    }

    void save() {
        log.debug("Saving updated core config")
        _storage.updateCoreConfig(this, {
            if (it.failed()) {
                log.error("Failed to update core config", it.cause())
            }
        })
    }

    @Override
    String toString() {
        return Json.encode(this)
    }

    static CoreConfig fromJson(String json) {
        CoreConfig coreConfig = Json.decodeValue(json, CoreConfig.class)
        if (coreConfig != null) {
            coreConfig.apmIntegrationConfig?.setupCoreConfig(coreConfig)
        }
        return coreConfig
    }

    static void setupCoreConfig(JsonObject deployConfig, Optional<CoreConfig> currentConfig, SourceStorage storage) {
        _storage = storage

        CoreConfig coreConfig
        if (currentConfig.ifPresent()) {
            coreConfig = currentConfig.get()
        } else {
            coreConfig = new CoreConfig()
            def integrations = deployConfig.getJsonArray("integrations")
            for (int i = 0; i < integrations.size(); i++) {
                def integration = integrations.getJsonObject(i)
                if (integration.getString("category") == "APM") {
                    coreConfig.apmIntegrationConfig = new APMIntegrationConfig()
                    coreConfig.apmIntegrationConfig.setupCoreConfig(coreConfig)
                    break
                }
            }
        }
        INSTANCE = coreConfig
    }
}