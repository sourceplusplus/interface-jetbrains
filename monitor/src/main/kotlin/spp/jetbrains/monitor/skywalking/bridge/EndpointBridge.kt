package spp.jetbrains.monitor.skywalking.bridge

import spp.jetbrains.monitor.skywalking.SkywalkingClient
import spp.jetbrains.monitor.skywalking.SkywalkingClient.LocalMessageCodec
import io.vertx.core.Vertx
import io.vertx.core.eventbus.ReplyException
import io.vertx.core.eventbus.ReplyFailure
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import monitor.skywalking.protocol.metadata.SearchEndpointQuery
import org.slf4j.LoggerFactory

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("MagicNumber")
class EndpointBridge(private val skywalkingClient: SkywalkingClient) : CoroutineVerticle() {

    override suspend fun start() {
        val isRegisteredMap = vertx.sharedData().getLocalMap<String, Boolean>("registered_codecs")
        if (!isRegisteredMap.getOrDefault("EndpointBridge", false)) {
            vertx.eventBus().registerDefaultCodec(EndpointQuery::class.java, LocalMessageCodec())
            isRegisteredMap["EndpointBridge"] = true
        }

        vertx.eventBus().localConsumer<String>(searchExactEndpointAddress) {
            launch(vertx.dispatcher()) {
                val service = try {
                    ServiceBridge.getCurrentService(vertx)
                } catch (ex: ReplyException) {
                    if (ex.failureType() == ReplyFailure.TIMEOUT) {
                        log.debug("Timed out looking for current service")
                        it.reply(null)
                        return@launch
                    } else {
                        ex.printStackTrace()
                        it.fail(500, ex.message)
                        return@launch
                    }
                } catch (throwable: Throwable) {
                    it.fail(404, "Apache SkyWalking current service unavailable")
                    return@launch
                }

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
            }
        }

        vertx.eventBus().localConsumer<EndpointQuery>(getEndpointsAddress) {
            launch(vertx.dispatcher()) {
                val service = try {
                    ServiceBridge.getCurrentService(vertx)
                } catch (ex: ReplyException) {
                    if (ex.failureType() == ReplyFailure.TIMEOUT) {
                        log.debug("Timed out looking for current service")
                        it.reply(null)
                        return@launch
                    } else {
                        ex.printStackTrace()
                        it.fail(500, ex.message)
                        return@launch
                    }
                } catch (throwable: Throwable) {
                    throwable.printStackTrace()
                    it.fail(404, "Apache SkyWalking current service unavailable")
                    return@launch
                }

                val endpointQuery = it.body()
                val endpoints = skywalkingClient.searchEndpoint(
                    "", endpointQuery.serviceId ?: service.id, endpointQuery.limit
                )
                it.reply(endpoints)
            }
        }
    }

    /**
     * todo: description.
     *
     * @since 0.1.0
     * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
     */
    companion object {
        private val log = LoggerFactory.getLogger(EndpointBridge::class.java)
        private const val rootAddress = "monitor.skywalking.endpoint"
        private const val searchExactEndpointAddress = "$rootAddress.searchExactEndpoint"
        private const val getEndpointsAddress = "$rootAddress.getEndpoints"

        suspend fun searchExactEndpoint(keyword: String, vertx: Vertx): SearchEndpointQuery.Result? {
            return vertx.eventBus()
                .request<SearchEndpointQuery.Result?>(searchExactEndpointAddress, keyword)
                .await().body()
        }

        suspend fun getEndpoints(
            serviceId: String? = null,
            limit: Int,
            vertx: Vertx
        ): List<SearchEndpointQuery.Result> {
            return vertx.eventBus()
                .request<List<SearchEndpointQuery.Result>>(
                    getEndpointsAddress, EndpointQuery(
                        serviceId = serviceId,
                        limit = limit
                    )
                ).await().body()
        }
    }

    /**
     * todo: description.
     *
     * @since 0.1.0
     * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
     */
    data class EndpointQuery(
        val serviceId: String? = null,
        val limit: Int
    )
}
