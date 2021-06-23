package com.sourceplusplus.protocol.service.live

import io.vertx.codegen.annotations.ProxyGen
import io.vertx.codegen.annotations.VertxGen
import io.vertx.core.AsyncResult
import io.vertx.core.Handler

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@ProxyGen
@VertxGen
interface LiveViewService {
    fun addLiveViewSubscription(metricNames: List<String>, handler: Handler<AsyncResult<String>>)
    fun removeLiveViewSubscription(subscriptionId: String, handler: Handler<AsyncResult<Boolean>>)
    fun clearLiveViewSubscriptions(handler: Handler<AsyncResult<Boolean>>)
}
