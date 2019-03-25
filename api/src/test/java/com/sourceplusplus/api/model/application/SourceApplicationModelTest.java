package com.sourceplusplus.api.model.application;

import com.sourceplusplus.api.model.config.SourceAgentConfig;
import io.vertx.core.json.Json;
import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.1.3
 * @since 0.1.0
 */
public class SourceApplicationModelTest {

    @Test
    public void applicationWithAgentConfig() {
        SourceAgentConfig agentConfig = new SourceAgentConfig();
        agentConfig.packages = new ArrayList<>();
        agentConfig.packages.add("com.test.artifact");
        SourceApplication application = SourceApplication.builder()
                .createDate(Instant.now())
                .appUuid(UUID.randomUUID().toString())
                .agentConfig(agentConfig).build();

        String appAgentConfig = Json.encode(application.agentConfig());
        String actualConfig = agentConfig.toString();
        assertEquals(actualConfig, appAgentConfig);
    }
}