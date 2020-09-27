package com.sourceplusplus.monitor.skywalking.track

import com.sourceplusplus.monitor.skywalking.SkywalkingClient
import io.vertx.core.Vertx
import io.vertx.kotlin.core.eventbus.requestAwait
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import monitor.skywalking.protocol.metadata.SearchEndpointQuery

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class EndpointTracker(private val skywalkingClient: SkywalkingClient) : CoroutineVerticle() {

    override suspend fun start() {
        vertx.eventBus().localConsumer<String>(searchExactEndpointAddress) {
            launch(vertx.dispatcher()) {
                val service = ServiceTracker.getCurrentService(vertx)
                if (service != null) {
                    val endpointName = it.body()
                    val endpoints = skywalkingClient.searchEndpoint(endpointName, service.id, 10)
                    if (endpoints.isNotEmpty()) {
                        val exactEndpoint = endpoints.find { it.name == endpointName }
                        if (exactEndpoint != null) {
                            it.reply(exactEndpoint)
                        } else {
                            it.reply(null)
                        }
                    } else {
                        it.reply(null)
                    }
                } else {
                    it.reply(null)
                }
            }
        }
    }

    companion object {
        private const val rootAddress = "monitor.skywalking.endpoint"
        private const val searchExactEndpointAddress = "$rootAddress.searchExactEndpoint"

        suspend fun searchExactEndpoint(keyword: String, vertx: Vertx): SearchEndpointQuery.Result? {
            return vertx.eventBus()
                .requestAwait<SearchEndpointQuery.Result?>(searchExactEndpointAddress, keyword)
                .body()
        }
    }
}
