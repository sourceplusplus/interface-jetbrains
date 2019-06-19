package com.sourceplusplus.core.api

import com.sourceplusplus.api.client.SourceCoreClient
import com.sourceplusplus.api.model.application.SourceApplication
import io.vertx.core.Handler
import io.vertx.ext.unit.TestContext

/**
 * @version 0.2.0
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
abstract class SourceCoreAPITest {

    public static final String apiHost = System.getenv().getOrDefault(
            "SPP_API_HOST", System.getProperty("SPP_API_HOST", "localhost"))
    public static final int apiPort = Integer.parseInt(System.getenv().getOrDefault(
            "SPP_API_PORT", "" + System.getProperty("SPP_API_PORT", "8080")))
    public static final boolean apiSslEnabled = Boolean.parseBoolean(System.getenv().getOrDefault(
            "SPP_API_SSL_ENABLED", System.getProperty("SPP_API_SSL_ENABLED", "false")))
    public static SourceCoreClient coreClient = new SourceCoreClient(sppUrl)

    static String getSppUrl() {
        if (apiSslEnabled) {
            return "https://" + apiHost + ":" + apiPort;
        } else {
            return "http://" + apiHost + ":" + apiPort;
        }
    }

    static void createApplication(TestContext test, Handler<SourceApplication> handler) {
        coreClient.createApplication({
            if (it.failed()) {
                test.fail(it.cause())
            } else {
                handler.handle(it.result())
            }
        })
    }
}
