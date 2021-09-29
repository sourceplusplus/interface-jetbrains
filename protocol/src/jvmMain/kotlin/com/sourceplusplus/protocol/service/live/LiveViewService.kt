package com.sourceplusplus.protocol.service.live

import com.sourceplusplus.protocol.view.LiveViewSubscription
import io.vertx.codegen.annotations.ProxyGen
import io.vertx.codegen.annotations.VertxGen
import io.vertx.core.AsyncResult
import io.vertx.core.Handler

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@ProxyGen
@VertxGen
interface LiveViewService {
    fun addLiveViewSubscription(subscription: LiveViewSubscription, handler: Handler<AsyncResult<LiveViewSubscription>>)
    fun removeLiveViewSubscription(subscriptionId: String, handler: Handler<AsyncResult<LiveViewSubscription>>)
    fun clearLiveViewSubscriptions(handler: Handler<AsyncResult<List<LiveViewSubscription>>>)
}
