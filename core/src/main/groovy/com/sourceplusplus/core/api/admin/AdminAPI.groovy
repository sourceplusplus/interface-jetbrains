package com.sourceplusplus.core.api.admin

import com.sourceplusplus.core.integration.skywalking.config.SkywalkingEndpointIdDetector
import com.sourceplusplus.core.storage.AbstractSourceStorage
import com.sourceplusplus.core.storage.ElasticsearchDAO
import io.vertx.core.AbstractVerticle
import io.vertx.ext.web.Router
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

    private final Router baseRouter
    private final AbstractSourceStorage storage

    AdminAPI(Router baseRouter, AbstractSourceStorage storage) {
        this.baseRouter = baseRouter
        this.storage = storage
    }

    @Override
    void start() throws Exception {
        baseRouter.get("/admin/integrations/skywalking/searchForNewEndpoints")
                .handler(this.&searchForNewEndpointsRoute)
        baseRouter.get("/admin/storage/refresh")
                .handler(this.&refreshStorage)
        log.info("{} started", getClass().getSimpleName())
    }

    private void searchForNewEndpointsRoute(RoutingContext routingContext) {
        vertx.eventBus().send(SkywalkingEndpointIdDetector.SEARCH_FOR_NEW_ENDPOINTS, true, {
            routingContext.response().setStatusCode(200).end()
        })
    }

    private void refreshStorage(RoutingContext routingContext) {
        if (storage.needsManualRefresh()) {
            //todo: not hardcode elasticsearch
            vertx.eventBus().send(ElasticsearchDAO.REFRESH_STORAGE, true, {
                routingContext.response().setStatusCode(200).end()
            })
        } else {
            routingContext.response().setStatusCode(200).end()
        }
    }
}
