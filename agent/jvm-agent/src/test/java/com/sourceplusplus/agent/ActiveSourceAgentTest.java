package com.sourceplusplus.agent;

import com.ea.agentloader.AgentLoader;
import com.sourceplusplus.api.client.SourceCoreClient;
import com.sourceplusplus.api.model.config.SourceAgentConfig;
import org.apache.skywalking.apm.agent.core.conf.Config;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.1.1
 * @since 0.1.0
 */
public class ActiveSourceAgentTest {

    public static final String apiHost = System.getenv().getOrDefault(
            "SPP_API_HOST", System.getProperty("SPP_API_HOST", "localhost"));
    public static final int apiPort = Integer.parseInt(System.getenv().getOrDefault(
            "SPP_API_PORT", "" + System.getProperty("SPP_API_PORT", "8080")));
    public static final boolean apiSslEnabled = Boolean.parseBoolean(System.getenv().getOrDefault(
            "SPP_API_SSL_ENABLED", System.getProperty("SPP_API_SSL_ENABLED", "false")));
    public static SourceCoreClient coreClient = new SourceCoreClient(getSppUrl());

    @BeforeClass
    public static void setup() {
        if (!SourceAgent.isAgentInitialized()) {
            AgentLoader.loadAgentClass(SourceAgent.class.getName(), null);
        }
    }

    @Test
    public void sourceAgentInitializeTest() {
        assertTrue(SourceAgent.isAgentInitialized());
    }

    public static void usingAppUuid(String appUuid) {
        SourceAgentConfig.current.appUuid = appUuid;
        Config.Agent.SERVICE_NAME = SourceAgentConfig.current.appUuid;
    }

    static String getSppUrl() {
        if (apiSslEnabled) {
            return "https://" + apiHost + ":" + apiPort;
        } else {
            return "http://" + apiHost + ":" + apiPort;
        }
    }
}