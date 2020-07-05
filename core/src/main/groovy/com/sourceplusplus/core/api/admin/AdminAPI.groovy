package com.sourceplusplus.core.api.admin

import com.sourceplusplus.api.model.error.SourceAPIError
import com.sourceplusplus.api.model.error.SourceAPIErrors
import com.sourceplusplus.api.model.integration.IntegrationInfo
import com.sourceplusplus.core.SourceCore
import com.sourceplusplus.core.integration.apm.skywalking.config.SkywalkingEndpointIdDetector
import com.sourceplusplus.core.storage.CoreConfig
import groovy.util.logging.Slf4j
import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.Json
import io.vertx.ext.web.RoutingContext

/**
 * Used to control the core server.
 *
 * @version 0.3.1
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class AdminAPI extends AbstractVerticle {

    private final SourceCore core

    AdminAPI(SourceCore core) {
        this.core = Objects.requireNonNull(core)
    }

    @Override
    void start() throws Exception {
        core.baseRouter.get("/admin/integrations").handler(this.&getIntegrationsRoute)
        core.baseRouter.put("/admin/integrations/:integrationId")
                .handler(this.&updateIntegrationInfoRoute)
        core.baseRouter.get("/admin/integrations/apache_skywalking/searchForNewEndpoints")
                .handler(this.&searchForNewEndpointsRoute)
        core.baseRouter.get("/admin/storage/refresh")
                .handler(this.&refreshStorageRoute)
        core.baseRouter.delete("/admin/core/shutdown")
                .handler(this.&shutdownCoreRoute)
        log.info("{} started", getClass().getSimpleName())
    }

    private shutdownCoreRoute(RoutingContext routingContext) {
        log.info("Shutting down Source++ Core")
        routingContext.response().setStatusCode(200).end()

        vertx.close({
            if (it.succeeded()) {
                def shutdownServer = routingContext.request().getParam("shutdownServer")
                if (shutdownServer == null || shutdownServer.isBlank()) {
                    //todo: deprecate restartServer
                    shutdownServer = routingContext.request().getParam("restartServer")
                }
                shutdown(shutdownServer && Boolean.valueOf(shutdownServer))
            } else {
                log.error("Failed to shutdown Source++ Core", it.cause())
            }
        })
    }

    private static getIntegrationsRoute(RoutingContext routingContext) {
        routingContext.response().setStatusCode(200)
                .end(Json.encode(CoreConfig.INSTANCE.integrationCoreConfig.integrations))
    }

    private void updateIntegrationInfoRoute(RoutingContext routingContext) {
        def integrationId = routingContext.request().getParam("integrationId")
        if (!integrationId) {
            routingContext.response().setStatusCode(400)
                    .end(Json.encode(new SourceAPIError().addError(SourceAPIErrors.INVALID_INPUT)))
            return
        }

        IntegrationInfo request
        try {
            def requestOb = routingContext.getBodyAsJson()
                    .put("id", integrationId)
                    .putNull("name")
                    .putNull("category")
                    .putNull("version")
            if (!requestOb.getJsonObject("config")) {
                requestOb.putNull("config")
            }
            request = Json.decodeValue(requestOb.toString(), IntegrationInfo.class)
        } catch (all) {
            all.printStackTrace()
            routingContext.response().setStatusCode(400)
                    .end(Json.encode(new SourceAPIError().addError(SourceAPIErrors.INVALID_INPUT)))
            return
        }

        updateIntegrationInfo(request, {
            if (it.succeeded()) {
                routingContext.response().setStatusCode(200)
                        .end(Json.encode(it.result()))
            } else {
                routingContext.response().setStatusCode(400)
                        .end(Json.encode(new SourceAPIError().addError(it.cause().message)))
            }
        })
    }

    void updateIntegrationInfo(IntegrationInfo integrationInfo, Handler<AsyncResult<IntegrationInfo>> handler) {
        def availableIntegration = core.availableIntegrations.find { it.id() == integrationInfo.id() }
        if (availableIntegration) {
            integrationInfo = integrationInfo
                    .withName(availableIntegration.name())
                    .withCategory(availableIntegration.category())

            vertx.eventBus().request(SourceCore.UPDATE_INTEGRATIONS, integrationInfo, {
                if (it.succeeded()) {
                    handler.handle(Future.succeededFuture(integrationInfo))
                } else {
                    handler.handle(Future.failedFuture(it.cause()))
                }
            })
        } else {
            handler.handle(Future.failedFuture(
                    new IllegalStateException("Could not find integration: " + integrationInfo.id())))
        }
    }

    private void searchForNewEndpointsRoute(RoutingContext routingContext) {
        vertx.eventBus().request(SkywalkingEndpointIdDetector.SEARCH_FOR_NEW_ENDPOINTS, true, {
            routingContext.response().setStatusCode(200).end()
        })
    }

    private void refreshStorageRoute(RoutingContext routingContext) {
        if (core.storage.needsManualRefresh()) {
            core.storage.refreshDatabase({
                if (it.succeeded()) {
                    routingContext.response().setStatusCode(200).end()
                } else {
                    log.error("Failed to refresh storage", it.cause())
                    routingContext.response().setStatusCode(500).end()
                }
            })
        } else {
            routingContext.response().setStatusCode(200).end()
        }
    }

    private static void shutdown(boolean shutdownServer) throws RuntimeException, IOException {
        if (shutdownServer) {
            String shutdownCommand
            String operatingSystem = System.getProperty("os.name").toLowerCase()
            if (operatingSystem.startsWith("linux") || operatingSystem.startsWith("mac")) {
                shutdownCommand = "shutdown -r now"
            } else if (operatingSystem.startsWith("windows")) {
                shutdownCommand = "shutdown.exe -s -t 0"
            } else {
                throw new RuntimeException("Unsupported operating system: " + System.getProperty("os.name"))
            }
            Runtime.getRuntime().exec(shutdownCommand)
        }
        System.exit(0)
    }
}
