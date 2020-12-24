package com.sourceplusplus.monitor.skywalking

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.coroutines.await
import com.sourceplusplus.monitor.skywalking.bridge.*
import io.vertx.kotlin.core.deployVerticleAwait
import io.vertx.kotlin.coroutines.CoroutineVerticle
import monitor.skywalking.protocol.metadata.GetTimeInfoQuery
import org.slf4j.LoggerFactory

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SkywalkingMonitor(private val serverUrl: String) : CoroutineVerticle() {

    companion object {
        private val log = LoggerFactory.getLogger(SkywalkingMonitor::class.java)
    }

    override suspend fun start() {
        log.debug("Setting up Apache SkyWalking monitor")
        setup()
        log.info("Successfully setup Apache SkyWalking monitor")
    }

    private suspend fun setup() {
        log.debug("Apache SkyWalking server: $serverUrl")
        val client = ApolloClient.builder()
            .serverUrl(serverUrl)
            .build()

        val response = client.query(GetTimeInfoQuery()).await()
        if (response.hasErrors()) {
            response.errors!!.forEach { log.error(it.message) }
            throw RuntimeException("Failed to get Apache SkyWalking time info")
        } else {
            val timezone = Integer.parseInt(response.data!!.result!!.timezone) / 100
            val skywalkingClient = SkywalkingClient(vertx, client, timezone)

            vertx.deployVerticleAwait(ServiceBridge(skywalkingClient))
            vertx.deployVerticleAwait(ServiceInstanceBridge(skywalkingClient))
            vertx.deployVerticleAwait(EndpointBridge(skywalkingClient))
            vertx.deployVerticleAwait(EndpointMetricsBridge(skywalkingClient))
            vertx.deployVerticleAwait(EndpointTracesBridge(skywalkingClient))
        }
    }
}
