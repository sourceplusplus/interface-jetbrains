package com.sourceplusplus.api.model.config;

import io.vertx.core.json.JsonObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.1.3
 * @since 0.1.0
 */
public class SourceAgentConfigTest {

    @Test
    public void configWithCustomAPI() {
        SourceAgentConfig agentConfig = new SourceAgentConfig();
        agentConfig.apiVersion = "v2";
        agentConfig.apiHost = "google.com";
        agentConfig.apiPort = 900;
        agentConfig.apiSslEnabled = true;
        agentConfig.apiKey = "my-key";

        JsonObject json = agentConfig.toJsonObject();
        JsonObject apiConfig = json.getJsonObject("api");
        assertNotNull(apiConfig);

        assertEquals(agentConfig.apiVersion, apiConfig.getString("version"));
        assertEquals(agentConfig.apiHost, apiConfig.getString("host"));
        assertEquals(agentConfig.apiPort, apiConfig.getInteger("port"));
        assertEquals(agentConfig.apiSslEnabled, apiConfig.getBoolean("ssl"));
        assertEquals(agentConfig.apiKey, apiConfig.getString("key"));
    }
}