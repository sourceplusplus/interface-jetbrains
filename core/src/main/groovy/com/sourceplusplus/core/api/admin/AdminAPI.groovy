package com.sourceplusplus.core.api.admin

import com.sourceplusplus.api.model.error.SourceAPIError
import com.sourceplusplus.api.model.error.SourceAPIErrors
import com.sourceplusplus.api.model.integration.IntegrationInfo
import com.sourceplusplus.core.SourceCore
import com.sourceplusplus.core.integration.apm.skywalking.config.SkywalkingEndpointIdDetector
import com.sourceplusplus.core.storage.elasticsearch.ElasticsearchDAO
import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * todo: description
 *
 * @version 0.2.3
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class AdminAPI extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(this.name)

    private final SourceCore core

    AdminAPI(SourceCore core) {
        this.core = Objects.requireNonNull(core)
    }

    @Override
    void start() throws Exception {
        core.baseRouter.get("/admin/integrations").handler(this.&getIntegrationsRoute)
        core.baseRouter.put("/admin/integrations/:integrationId").handler(this.&updateIntegrationInfoRoute)
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
                def restartServer = routingContext.request().getParam("restartServer")
                shutdown(restartServer && Boolean.valueOf(restartServer))
            } else {
                log.error("Failed to shutdown Source++ Core", it.cause())
            }
        })
    }

    private getIntegrationsRoute(RoutingContext routingContext) {
        routingContext.response().setStatusCode(200).end(Json.encode(core.getIntegrations()))
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
        def currentInfo = core.integrations.find { it.id() == integrationInfo.id() }
        if (currentInfo) {
            integrationInfo = integrationInfo
                    .withCategory(currentInfo.category())
                    .withVersion(currentInfo.version())
            if (!integrationInfo.connections()) {
                integrationInfo = integrationInfo.withConnections(currentInfo.connections())
            }
            if (!integrationInfo.config()) {
                integrationInfo = integrationInfo.withConfig(currentInfo.config())
            }

            def integrations = config().getJsonArray("integrations")
            for (int i = 0; i < integrations.size(); i++) {
                def integration = integrations.getJsonObject(i)
                if (integration.getString("id") == integrationInfo.id()) {
                    integrations.getJsonObject(i).mergeIn(JsonObject.mapFrom(integrationInfo))
                    break
                }
            }

            vertx.eventBus().send(SourceCore.UPDATE_INTEGRATIONS, integrations, {
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
        vertx.eventBus().send(SkywalkingEndpointIdDetector.SEARCH_FOR_NEW_ENDPOINTS, true, {
            routingContext.response().setStatusCode(200).end()
        })
    }

    private void refreshStorageRoute(RoutingContext routingContext) {
        if (core.storage.needsManualRefresh()) {
            //todo: not hardcode elasticsearch
            vertx.eventBus().send(ElasticsearchDAO.REFRESH_STORAGE, true, {
                routingContext.response().setStatusCode(200).end()
            })
        } else {
            routingContext.response().setStatusCode(200).end()
        }
    }

    private static void shutdown(boolean restartServer) throws RuntimeException, IOException {
        if (restartServer) {
            String shutdownCommand
            String operatingSystem = System.getProperty("os.name")
            if ("Linux" == operatingSystem || "Mac OS X" == operatingSystem) {
                shutdownCommand = "shutdown -r now"
            } else if ("Windows" == operatingSystem) {
                shutdownCommand = "shutdown.exe -s -t 0"
            } else {
                throw new RuntimeException("Unsupported operating system.")
            }
            Runtime.getRuntime().exec(shutdownCommand)
        }
        System.exit(0)
    }
}
