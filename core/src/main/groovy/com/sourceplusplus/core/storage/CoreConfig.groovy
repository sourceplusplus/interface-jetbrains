package com.sourceplusplus.core.storage

import com.sourceplusplus.api.model.integration.IntegrationInfo
import com.sourceplusplus.core.integration.IntegrationCoreConfig
import groovy.util.logging.Slf4j
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject

/**
 * Persistent configuration for the core system.
 *
 * @version 0.3.0
 * @since 0.3.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class CoreConfig {

    public static CoreConfig INSTANCE
    private static SourceStorage _storage
    private IntegrationCoreConfig integrationCoreConfig = new IntegrationCoreConfig()

    IntegrationCoreConfig getIntegrationCoreConfig() {
        return integrationCoreConfig
    }

    void setApmIntegrationConfig(IntegrationCoreConfig integrationCoreConfig) {
        this.integrationCoreConfig = integrationCoreConfig
        INSTANCE?.save()
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
        return Json.decodeValue(json, CoreConfig.class)
    }

    static void setupCoreConfig(JsonObject deployConfig, Optional<CoreConfig> currentConfig, SourceStorage storage) {
        _storage = storage

        CoreConfig coreConfig
        if (currentConfig.isPresent()) {
            coreConfig = currentConfig.get()
        } else {
            coreConfig = new CoreConfig()
            def integrations = deployConfig.getJsonArray("integrations")
            for (int i = 0; i < integrations.size(); i++) {
                def integration = integrations.getJsonObject(i)
                if (integration.fieldNames() - ["id", "name", "category"]) {
                    if (!integration.containsKey("config")) {
                        integration = integration.put("config", new JsonObject())
                    }
                    coreConfig.integrationCoreConfig.updateIntegration(Json.decodeValue(
                            integration.toString(), IntegrationInfo.class))
                }
            }
        }
        INSTANCE = coreConfig
    }
}