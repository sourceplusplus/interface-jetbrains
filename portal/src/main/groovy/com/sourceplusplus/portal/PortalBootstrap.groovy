package com.sourceplusplus.portal

import com.codahale.metrics.MetricRegistry
import com.sourceplusplus.api.bridge.SourceBridgeClient
import com.sourceplusplus.api.client.SourceCoreClient
import com.sourceplusplus.api.model.SourceMessage
import com.sourceplusplus.api.model.application.SourceApplication
import com.sourceplusplus.api.model.artifact.SourceArtifactConfig
import com.sourceplusplus.api.model.config.SourcePortalConfig
import com.sourceplusplus.portal.coordinate.track.PortalViewTracker
import com.sourceplusplus.portal.display.PortalUI
import com.sourceplusplus.portal.display.tabs.ConfigurationTab
import com.sourceplusplus.portal.display.tabs.OverviewTab
import com.sourceplusplus.portal.display.tabs.TracesTab
import groovy.util.logging.Slf4j
import io.vertx.core.*
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.JsonObject
import io.vertx.core.net.JksOptions
import io.vertx.ext.bridge.PermittedOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import org.apache.commons.io.IOUtils

import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * Used to bootstrap the Source++ Portal.
 *
 * The portal is able to fetch and display runtime behavior without interacting with the Source++ Plugin.
 * This allows the portal to be independently outside the IDE.
 *
 * @version 0.3.0
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class PortalBootstrap extends AbstractVerticle {

    public static final MetricRegistry portalMetrics = new MetricRegistry()
    private final boolean pluginAvailable

    static void main(String[] args) {
        def configJSON
        def configFile = System.getenv("SOURCE_CONFIG")
        if (!configFile) {
            throw new RuntimeException("Missing SOURCE_CONFIG system environment!")
        }

        log.info("Using configuration file: $configFile")
        def configInputStream
        if (new File(configFile).exists()) {
            configInputStream = new File(configFile).newInputStream()
        } else {
            configInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("config/$configFile")
        }
        def configData = IOUtils.toString(configInputStream, StandardCharsets.UTF_8)
        configJSON = new JsonObject(configData)

        def vertxOptions = new VertxOptions()
//        if (BUILD.getString("version") == "dev") {
//            //allow debug pauses
//            vertxOptions.setBlockedThreadCheckInterval(Integer.MAX_VALUE)
//        }

        Vertx.vertx(vertxOptions).deployVerticle(new PortalBootstrap(false),
                new DeploymentOptions().setConfig(configJSON))
    }

    PortalBootstrap(boolean pluginAvailable) {
        this.pluginAvailable = pluginAvailable
    }

    @Override
    void start(Promise<Void> startFuture) throws Exception {
        if (pluginAvailable) {
            PortalUI.assignVertx(vertx)
        } else {
            SourceMessage.registerCodecs(vertx)
            SockJSHandler sockJSHandler = SockJSHandler.create(vertx)
            SockJSBridgeOptions portalBridgeOptions = new SockJSBridgeOptions()
                    .addInboundPermitted(new PermittedOptions().setAddressRegex(".+"))
                    .addOutboundPermitted(new PermittedOptions().setAddressRegex(".+"))
            sockJSHandler.bridge(portalBridgeOptions)

            Router router = Router.router(vertx)
            router.route("/eventbus/*").handler(sockJSHandler)
            if (config().getBoolean("bridge_ssl")) {
                vertx.createHttpServer(new HttpServerOptions().setSsl(true)
                        .setKeyStoreOptions(new JksOptions()
                                .setPath(config().getString("jks_path"))
                                .setPassword(config().getString("jks_password")))
                ).requestHandler(router)
                        .listen(config().getInteger("bridge_port"), config().getString("bridge_host"), {
                            if (it.succeeded()) {
                                log.info("Started portal ui bridge (using SSL). Using port: " + it.result().actualPort())
                            } else {
                                it.cause().printStackTrace()
                                log.error("Failed to start portal bridge (using SSL)", it.cause())
                                System.exit(-1)
                            }
                        })
            } else {
                vertx.createHttpServer().requestHandler(router).listen(config().getInteger("bridge_port"),
                        config().getString("bridge_host"), {
                    if (it.succeeded()) {
                        log.info("Started portal ui bridge. Using port: " + it.result().actualPort())
                    } else {
                        it.cause().printStackTrace()
                        log.error("Failed to start portal bridge", it.cause())
                        System.exit(-1)
                    }
                })
            }

            //setup connection to core
            def apiConfig = config().getJsonObject("api")
            def coreClient = new SourceCoreClient(
                    apiConfig.getString("host"), apiConfig.getInteger("port"), apiConfig.getBoolean("ssl"))
            if (apiConfig.getString("key")) {
                coreClient.setApiKey(apiConfig.getString("key"))
            }

            //setup bridge to core
            new SourceBridgeClient(vertx, apiConfig.getString("host"), apiConfig.getInteger("port"),
                    apiConfig.getBoolean("ssl")).setupSubscriptions()

            //register subscriptions
            def subscriptions = config().getJsonArray("artifact_subscriptions")
            for (int i = 0; i < subscriptions.size(); i++) {
                def sub = subscriptions.getJsonObject(i)
                def appUuid = sub.getString("app_uuid")
                def artifactQualifiedName = sub.getString("artifact_qualified_name")
                SourcePortalConfig.current.addCoreClient(appUuid, coreClient)

                if (sub.getBoolean("auto_subscribe", false)) {
                    //make sure application exists first (create if necessary), then subscribe
                    SourcePortalConfig.current.getCoreClient(appUuid).getApplication(appUuid, {
                        if (it.succeeded()) {
                            if (it.result().isPresent()) {
                                def artifactConfig = SourceArtifactConfig.builder().subscribeAutomatically(true).build()
                                SourcePortalConfig.current.getCoreClient(appUuid).createOrUpdateArtifactConfig(appUuid, artifactQualifiedName, artifactConfig, {
                                    if (it.failed()) {
                                        log.error("Failed to create artifact config", it.cause())
                                    }
                                })
                            } else {
                                def createApplication = SourceApplication.builder().isCreateRequest(true)
                                        .appUuid(appUuid).build()
                                SourcePortalConfig.current.getCoreClient(appUuid).createApplication(createApplication, {
                                    if (it.succeeded()) {
                                        def artifactConfig = SourceArtifactConfig.builder().subscribeAutomatically(true).build()
                                        SourcePortalConfig.current.getCoreClient(appUuid).createOrUpdateArtifactConfig(appUuid, artifactQualifiedName, artifactConfig, {
                                            if (it.failed()) {
                                                log.error("Failed to create artifact config", it.cause())
                                            }
                                        })
                                    } else {
                                        log.error("Failed to create application", it.cause())
                                    }
                                })
                            }
                        } else {
                            log.error("Failed to get application", it.cause())
                        }
                    })
                }

                //register portal
                SourcePortal.register(appUuid, artifactQualifiedName, true)
            }

            //keep subscriptions alive
            vertx.setPeriodic(TimeUnit.MINUTES.toMillis(2), {
                SourcePortalConfig.current.coreClients.each {
                    it.value.refreshSubscriberApplicationSubscriptions(it.key, {
                        if (it.succeeded()) {
                            log.debug("Refreshed subscriptions")
                        } else {
                            log.error("Failed to refresh subscriptions", it.cause())
                        }
                    })
                }
            })

            vertx.eventBus().consumer("REGISTER_PORTAL", {
                def request = it.body() as JsonObject
                def appUuid = request.getString("app_uuid")
                def artifactQualifiedName = request.getString("artifact_qualified_name")

                def activePortals = SourcePortal.getPortals(appUuid, artifactQualifiedName)
                log.info("Registering new portal")
                def portalUuid = SourcePortal.register(appUuid, artifactQualifiedName, true)
                if (activePortals) {
                    log.info("Registered new portal with cloned view from portal: " + activePortals[0].portalUuid)
                    SourcePortal.getPortal(portalUuid).portalUI.cloneUI(activePortals[0].portalUI)
                } else {
                    log.info("Registered new portal with blank view")
                }
                it.reply(new JsonObject().put("portal_uuid", portalUuid))
            })
        }

        def overviewTabFut = Promise.promise().future()
        def tracesTabFut = Promise.promise().future()
        def configurationTabFut = Promise.promise().future()
        vertx.deployVerticle(new OverviewTab(), new DeploymentOptions()
                .setConfig(config()).setWorker(true), overviewTabFut)
        vertx.deployVerticle(new TracesTab(), new DeploymentOptions()
                .setConfig(config()).setWorker(true), tracesTabFut)
        vertx.deployVerticle(new ConfigurationTab(pluginAvailable), new DeploymentOptions()
                .setConfig(config()).setWorker(true), configurationTabFut)
        CompositeFuture.all(overviewTabFut, tracesTabFut, configurationTabFut).onComplete({
            if (it.succeeded()) {
                //track
                vertx.deployVerticle(new PortalViewTracker(), new DeploymentOptions().setWorker(true), startFuture)
            } else {
                startFuture.fail(it.cause())
            }
        })

        vertx.eventBus().consumer("PortalLogger", {
            log.info("[PORTAL] - " + it.body())
        })
        log.info("{} started", getClass().getSimpleName())
    }
}
