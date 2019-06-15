package com.sourceplusplus.core.api.admin

import com.sourceplusplus.api.model.error.SourceAPIError
import com.sourceplusplus.api.model.error.SourceAPIErrors
import com.sourceplusplus.api.model.info.IntegrationInfo
import com.sourceplusplus.core.SourceCore
import com.sourceplusplus.core.integration.apm.skywalking.config.SkywalkingEndpointIdDetector
import com.sourceplusplus.core.storage.elasticsearch.ElasticsearchDAO
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.Json
import io.vertx.ext.web.RoutingContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * todo: description
 *
 * @version 0.2.0
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
        core.baseRouter.get("/admin/integrations/:integrationId").handler(this.&configureIntegrationRoute)
        core.baseRouter.get("/admin/integrations/skywalking/searchForNewEndpoints")
                .handler(this.&searchForNewEndpointsRoute)
        core.baseRouter.get("/admin/storage/refresh")
                .handler(this.&refreshStorage)
        log.info("{} started", getClass().getSimpleName())
    }

    private getIntegrationsRoute(RoutingContext routingContext) {
        routingContext.response().setStatusCode(200).end(Json.encode(core.getIntegrations()))
    }

    private void configureIntegrationRoute(RoutingContext routingContext) {
        def integrationId = routingContext.request().getParam("integrationId")
        if (!integrationId) {
            routingContext.response().setStatusCode(400)
                    .end(Json.encode(new SourceAPIError().addError(SourceAPIErrors.INVALID_INPUT)))
        } else {

        }
    }

    private IntegrationInfo configureIntegration(String integrationId) {

    }

    private void searchForNewEndpointsRoute(RoutingContext routingContext) {
        vertx.eventBus().send(SkywalkingEndpointIdDetector.SEARCH_FOR_NEW_ENDPOINTS, true, {
            routingContext.response().setStatusCode(200).end()
        })
    }

    private void refreshStorage(RoutingContext routingContext) {
        if (core.storage.needsManualRefresh()) {
            //todo: not hardcode elasticsearch
            vertx.eventBus().send(ElasticsearchDAO.REFRESH_STORAGE, true, {
                routingContext.response().setStatusCode(200).end()
            })
        } else {
            routingContext.response().setStatusCode(200).end()
        }
    }
}
