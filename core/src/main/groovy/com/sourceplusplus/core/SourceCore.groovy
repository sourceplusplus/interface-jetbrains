package com.sourceplusplus.core

import com.sourceplusplus.api.model.integration.IntegrationInfo
import com.sourceplusplus.core.api.admin.AdminAPI
import com.sourceplusplus.core.api.application.ApplicationAPI
import com.sourceplusplus.core.api.artifact.ArtifactAPI
import com.sourceplusplus.core.api.metric.MetricAPI
import com.sourceplusplus.core.api.trace.TraceAPI
import com.sourceplusplus.core.integration.apm.APMIntegration
import com.sourceplusplus.core.integration.apm.skywalking.SkywalkingIntegration
import com.sourceplusplus.core.storage.CoreConfig
import com.sourceplusplus.core.storage.SourceStorage
import com.sourceplusplus.core.storage.h2.H2DAO
import groovy.util.logging.Slf4j
import io.vertx.core.*
import io.vertx.core.eventbus.Message
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import static com.sourceplusplus.api.bridge.PluginBridgeEndpoints.*

/**
 * Used to setup storage, APIs, and integrations.
 *
 * @version 0.2.6
 * @since 0.2.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class SourceCore extends AbstractVerticle {

    public static final String UPDATE_INTEGRATIONS = "UpdateIntegrations"

    private final Set<String> deployedIntegrations = new HashSet<>()
    private final Set<String> deployedIntegrationAPIs = new HashSet<>()
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
    void start(Promise<Void> startFuture) throws Exception {
        vertx.eventBus().consumer(UPDATE_INTEGRATIONS, this.&updateIntegrations)

        log.info("Booting Source++ Core storage...")
        def storageCompleter = Promise.promise()
        def storageConfig = config().getJsonObject("storage")
        switch (storageConfig.getString("type")) {
            case "elasticsearch":
                log.info("Using storage: Elasticsearch")
                //storage = new ElasticsearchDAO(vertx.eventBus(), storageConfig.getJsonObject("elasticsearch"), storageCompleter)
                break
            case "h2":
                log.info("Using storage: H2")
                storage = new H2DAO(vertx, storageConfig.getJsonObject("h2"), storageCompleter)
                break
            default:
                throw new IllegalArgumentException("Unknown storage type: " + storageConfig.getString("type"))
        }

        (storageCompleter as Future).onComplete({
            if (it.succeeded()) {
                storage.getCoreConfig({
                    if (it.succeeded()) {
                        CoreConfig.setupCoreConfig(it.result(), storage)
                        bootCoreAPIs(startFuture)
                    } else {
                        startFuture.fail(it.cause())
                    }
                })
            } else {
                startFuture.fail(it.cause())
            }
        })
    }

    private void bootCoreAPIs(Promise<Void> startFuture) {
        log.info("Booting Source++ Core APIs...")
        adminAPI = new AdminAPI(this)
        applicationAPI = new ApplicationAPI(this)
        artifactAPI = new ArtifactAPI(this)
        metricAPI = new MetricAPI(this)
        traceAPI = new TraceAPI(this)

        def deploymentConfig = new DeploymentOptions().setConfig(config())
        def adminAPIFuture = Promise.promise().future()
        vertx.deployVerticle(adminAPI, deploymentConfig, adminAPIFuture)
        def applicationAPIFuture = Promise.promise().future()
        vertx.deployVerticle(applicationAPI, deploymentConfig, applicationAPIFuture)
        def artifactAPIFuture = Promise.promise().future()
        vertx.deployVerticle(artifactAPI, deploymentConfig, artifactAPIFuture)
        CompositeFuture.all(adminAPIFuture, applicationAPIFuture, artifactAPIFuture).onComplete({
            if (it.succeeded()) {
                log.info("Connecting Source++ integrations...")
                deployIntegrations(startFuture)
            } else {
                startFuture.fail(it.cause())
            }
        })
    }

    private void updateIntegrations(Message<IntegrationInfo> msg) {
        def integrationInfo = msg.body()
        CoreConfig.INSTANCE.integrationCoreConfig.updateIntegration(integrationInfo)
        vertx.eventBus().publish(INTEGRATION_INFO_UPDATED.address, integrationInfo)

        //undeploy integrations
        def undeployFutures = []
        deployedIntegrationAPIs.removeIf({
            def fut = Promise.promise().future()
            undeployFutures += fut
            vertx.undeploy(it, fut)
            return true
        })
        deployedIntegrations.removeIf({
            def fut = Promise.promise().future()
            undeployFutures += fut
            vertx.undeploy(it, fut)
            return true
        })
        CompositeFuture.all(undeployFutures).onComplete({
            if (it.succeeded()) {
                //redeploy integrations
                def fut = Promise.promise().future()
                fut.onComplete({
                    if (it.succeeded()) {
                        msg.reply(true)
                    } else {
                        it.cause().printStackTrace()
                        msg.fail(500, "Failed to deploy integrations")
                    }
                })
                deployIntegrations(fut)
            } else {
                it.cause().printStackTrace()
                msg.fail(500, "Failed to undeploy integrations")
            }
        })
    }

    private void deployIntegrations(Handler<AsyncResult<Void>> handler) {
        def succeeded = true
        def integrationFutures = []

        boolean deployMetricAPI = false
        boolean deployTraceAPI = false
        for (def integration : CoreConfig.INSTANCE.integrationCoreConfig.integrations) {
            if (integration.enabled()) {
                def future = Promise.promise()
                integrationFutures.add(future)

                switch (integration.id()) {
                    case "apache_skywalking":
                        deployMetricAPI = deployTraceAPI = true
                        connectToApacheSkyWalking(integration, future)
                        break
                    default:
                        succeeded = false
                        handler.handle(Future.failedFuture(new IllegalArgumentException(
                                "Invalid integration: " + integration.id())))
                }
            }
        }
        if (succeeded) {
            def deploymentOptions = new DeploymentOptions().setConfig(config())
            def metricAPIFuture = Promise.promise().future()
            def traceAPIFuture = Promise.promise().future()
            def futures = []
            if (deployMetricAPI) {
                futures.add(metricAPIFuture)
                vertx.deployVerticle(metricAPI, deploymentOptions, metricAPIFuture)
            }
            if (deployTraceAPI) {
                futures.add(traceAPIFuture)
                vertx.deployVerticle(traceAPI, deploymentOptions, traceAPIFuture)
            }
            CompositeFuture.all(futures).onComplete({
                if (it.succeeded()) {
                    it.result().list().each {
                        deployedIntegrationAPIs.add(it as String)
                    }
                    CompositeFuture.all(integrationFutures).onComplete(handler)
                } else {
                    handler.handle(Future.failedFuture(it.cause()))
                }
            })
        }
    }

    private void connectToApacheSkyWalking(IntegrationInfo integration, Handler<AsyncResult<Void>> handler) {
        log.info("Connecting to Apache SkyWalking...")
        apmIntegration = new SkywalkingIntegration(applicationAPI, artifactAPI, storage)
        vertx.deployVerticle(apmIntegration, new DeploymentOptions()
                .setConfig(new JsonObject(Json.encode(integration))), {
            if (it.succeeded()) {
                deployedIntegrations.add(it.result())
                handler.handle(Future.succeededFuture())
            } else {
                handler.handle(Future.failedFuture(it.cause()))
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

    List<IntegrationInfo> getAvailableIntegrations() {
        def integrationInfos = []
        def integrations = config().getJsonArray("integrations")
        for (int i = 0; i < integrations.size(); i++) {
            def integration = integrations.getJsonObject(i).put("config", new JsonObject())
            def integrationInfo = Json.decodeValue(integration.toString(), IntegrationInfo.class)
            switch (integrationInfo.id()) {
                case "apache_skywalking":
                    integrationInfos.add(integrationInfo.withName("Apache SkyWalking"))
                    break
                default:
                    throw new IllegalArgumentException("Invalid integration: " + integrationInfo.id())
            }
        }
        return integrationInfos
    }

    static List<IntegrationInfo> getActiveIntegrations() {
        return CoreConfig.INSTANCE.integrationCoreConfig.integrations.stream()
                .filter({ it -> it.enabled() })
                .map({ it -> IntegrationInfo.builder().id(it.id()).connections(it.connections()).build() })
                .collect()
    }
}
