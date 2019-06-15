package com.sourceplusplus.core

import com.sourceplusplus.api.model.info.AbstractIntegrationInfo
import com.sourceplusplus.api.model.info.IntegrationInfo
import com.sourceplusplus.api.model.info.IntegrationCategory
import com.sourceplusplus.core.api.admin.AdminAPI
import com.sourceplusplus.core.api.application.ApplicationAPI
import com.sourceplusplus.core.api.artifact.ArtifactAPI
import com.sourceplusplus.core.api.metric.MetricAPI
import com.sourceplusplus.core.api.trace.TraceAPI
import com.sourceplusplus.core.integration.apm.APMIntegration
import com.sourceplusplus.core.integration.apm.skywalking.SkywalkingIntegration
import com.sourceplusplus.core.storage.SourceStorage
import com.sourceplusplus.core.storage.elasticsearch.ElasticsearchDAO
import com.sourceplusplus.core.storage.h2.H2DAO
import groovy.util.logging.Slf4j
import io.vertx.core.AbstractVerticle
import io.vertx.core.CompositeFuture
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router

/**
 * todo: description
 *
 * @version 0.2.0
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class SourceCore extends AbstractVerticle {

    private final Router baseRouter
    private SourceStorage storage
    private AdminAPI adminAPI
    private ApplicationAPI applicationAPI
    private ArtifactAPI artifactAPI
    private MetricAPI metricAPI
    private TraceAPI traceAPI
    private APMIntegration apmIntegration

    SourceCore(Router baseRouter) {
        this.baseRouter = baseRouter
    }

    @Override
    void start(Future<Void> startFuture) throws Exception {
        log.info("Booting Source++ Core storage...")
        def storageConfig = config().getJsonObject("storage")
        switch (storageConfig.getString("type")) {
            case "elasticsearch":
                log.info("Using storage: Elasticsearch")
                storage = new ElasticsearchDAO(vertx.eventBus(), storageConfig.getJsonObject("elasticsearch"))
                break
            case "h2":
                log.info("Using storage: H2")
                storage = new H2DAO(vertx, storageConfig.getJsonObject("h2"))
                break
            default:
                throw new IllegalArgumentException("Unknown storage type: " + storageConfig.getString("type"))
        }

        log.info("Booting Source++ Core APIs...")
        adminAPI = new AdminAPI(baseRouter, this)
        applicationAPI = new ApplicationAPI(baseRouter, this)
        artifactAPI = new ArtifactAPI(baseRouter, this)
        metricAPI = new MetricAPI(vertx.sharedData(), this)
        traceAPI = new TraceAPI(vertx.sharedData(), this)

        def deploymentConfig = new DeploymentOptions().setConfig(config())
        def adminAPIFuture = Future.future()
        vertx.deployVerticle(adminAPI, deploymentConfig, adminAPIFuture.completer())
        def applicationAPIFuture = Future.future()
        vertx.deployVerticle(applicationAPI, deploymentConfig, applicationAPIFuture.completer())
        def artifactAPIFuture = Future.future()
        vertx.deployVerticle(artifactAPI, deploymentConfig, artifactAPIFuture.completer())
        CompositeFuture.all(adminAPIFuture, applicationAPIFuture, artifactAPIFuture)
                .setHandler({
            if (it.succeeded()) {
                log.info("Connecting Source++ integrations...")
                def succeeded = true
                def integrations = config().getJsonArray("integrations")
                def integrationFutures = []
                for (int i = 0; i < integrations.size(); i++) {
                    def future = Future.future()
                    integrationFutures.add(future)

                    def integration = integrations.getJsonObject(i)
                    switch (integration.getString("id")) {
                        case "apache_skywalking":
                            connectToApacheSkyWalking(integration, future)
                            break
                        default:
                            succeeded = false
                            startFuture.fail(new IllegalArgumentException(
                                    "Invalid integration: " + integration.getString("id")))
                    }
                }
                if (succeeded) {
                    CompositeFuture.all(integrationFutures).setHandler(startFuture.completer())
                }
            } else {
                startFuture.fail(it.cause())
            }
        })
    }

    private void connectToApacheSkyWalking(JsonObject integration, Future startFuture) {
        log.info("Connecting to Apache SkyWalking...")
        apmIntegration = new SkywalkingIntegration(artifactAPI, storage)
        vertx.deployVerticle(apmIntegration, new DeploymentOptions().setConfig(integration), {
            if (it.succeeded()) {
                def deploymentOptions = new DeploymentOptions().setConfig(config())
                def metricAPIFuture = Future.future()
                def traceAPIFuture = Future.future()
                vertx.deployVerticle(metricAPI, deploymentOptions, metricAPIFuture.completer())
                vertx.deployVerticle(traceAPI, deploymentOptions, traceAPIFuture.completer())
                CompositeFuture.all(metricAPIFuture, traceAPIFuture).setHandler(startFuture.completer())
            } else {
                startFuture.fail(it.cause())
            }
        })
    }

    Router getBaseRouter() {
        return baseRouter
    }

    AdminAPI getAdminAPI() {
        return adminAPI
    }

    ApplicationAPI getApplicationAPI() {
        return applicationAPI
    }

    ArtifactAPI getArtifactAPI() {
        return artifactAPI
    }

    MetricAPI getMetricAPI() {
        return metricAPI
    }

    TraceAPI getTraceAPI() {
        return traceAPI
    }

    SourceStorage getStorage() {
        return storage
    }

    APMIntegration getAPMIntegration() {
        return apmIntegration
    }

    List<IntegrationInfo> getActiveIntegrations() {
        def integrationInfos = []
        def integrations = config().getJsonArray("integrations")
        for (int i = 0; i < integrations.size(); i++) {
            def integration = integrations.getJsonObject(i)
            if (integration.getBoolean("enabled")) {
                def connection = integration.getJsonObject("connection")
                def connectionInfo = new AbstractIntegrationInfo.ConnectionInfo(
                        connection.getString("host"), connection.getInteger("port"))
                integrationInfos.add(IntegrationInfo.builder()
                        .name("Apache SkyWalking").category(IntegrationCategory.APM)
                        .version(integration.getString("version"))
                        .connection(connectionInfo).build())
            }
        }
        return integrationInfos
    }
}
