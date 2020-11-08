package com.sourceplusplus.monitor.skywalking.bridge

import com.sourceplusplus.monitor.skywalking.SkywalkingClient
import com.sourceplusplus.monitor.skywalking.SkywalkingClient.LocalMessageCodec
import io.vertx.core.Vertx
import io.vertx.kotlin.core.eventbus.requestAwait
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import monitor.skywalking.protocol.metadata.SearchEndpointQuery

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class EndpointBridge(private val skywalkingClient: SkywalkingClient) : CoroutineVerticle() {

    override suspend fun start() {
        val isRegisteredMap = vertx.sharedData().getLocalMap<String, Boolean>("registered_codecs")
        if (!isRegisteredMap.getOrDefault("EndpointBridge", false)) {
            vertx.eventBus().registerDefaultCodec(EndpointQuery::class.java, LocalMessageCodec())
            isRegisteredMap["EndpointBridge"] = true
        }

        vertx.eventBus().localConsumer<String>(searchExactEndpointAddress) {
            launch(vertx.dispatcher()) {
                val service = ServiceBridge.getCurrentService(vertx)
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

        vertx.eventBus().localConsumer<EndpointQuery>(getEndpointsAddress) {
            launch(vertx.dispatcher()) {
                val service = ServiceBridge.getCurrentService(vertx)
                if (service != null) {
                    val endpointQuery = it.body()
                    val endpoints = skywalkingClient.searchEndpoint(
                        "", endpointQuery.serviceId ?: service.id, endpointQuery.limit
                    )
                    it.reply(endpoints)
                } else {
                    it.reply(null)
                }
            }
        }
    }

    companion object {
        private const val rootAddress = "monitor.skywalking.endpoint"
        private const val searchExactEndpointAddress = "$rootAddress.searchExactEndpoint"
        private const val getEndpointsAddress = "$rootAddress.getEndpoints"

        suspend fun searchExactEndpoint(keyword: String, vertx: Vertx): SearchEndpointQuery.Result? {
            return vertx.eventBus()
                .requestAwait<SearchEndpointQuery.Result?>(searchExactEndpointAddress, keyword)
                .body()
        }

        suspend fun getEndpoints(
            serviceId: String? = null,
            limit: Int,
            vertx: Vertx
        ): List<SearchEndpointQuery.Result> {
            return vertx.eventBus()
                .requestAwait<List<SearchEndpointQuery.Result>>(
                    getEndpointsAddress, EndpointQuery(
                        serviceId = serviceId,
                        limit = limit
                    )
                ).body()
        }
    }

    data class EndpointQuery(
        val serviceId: String? = null,
        val limit: Int
    )
}
