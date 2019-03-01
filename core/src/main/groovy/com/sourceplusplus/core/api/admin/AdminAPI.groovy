package com.sourceplusplus.core.api.admin

import com.sourceplusplus.core.integration.skywalking.config.SkywalkingEndpointIdDetector
import io.vertx.core.AbstractVerticle
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * todo: description
 *
 * @version 0.1.0
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class AdminAPI extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(this.name)

    private final Router baseRouter

    AdminAPI(Router baseRouter) {
        this.baseRouter = baseRouter
    }

    @Override
    void start() throws Exception {
        baseRouter.get("/admin/integrations/skywalking/searchForNewEndpoints").handler(this.&searchForNewEndpointsRoute)
        log.info("{} started", getClass().getSimpleName())
    }

    private void searchForNewEndpointsRoute(RoutingContext routingContext) {
        vertx.eventBus().send(SkywalkingEndpointIdDetector.SEARCH_FOR_NEW_ENDPOINTS, true, {
            routingContext.response().setStatusCode(200).end()
        })
    }
}
