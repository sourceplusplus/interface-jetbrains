package com.sourceplusplus.monitor.skywalking

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.coroutines.toDeferred
import com.sourceplusplus.monitor.skywalking.track.*
import io.vertx.kotlin.core.deployVerticleAwait
import io.vertx.kotlin.coroutines.CoroutineVerticle
import monitor.skywalking.protocol.metadata.GetTimeInfoQuery
import org.slf4j.LoggerFactory

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SkywalkingMonitor : CoroutineVerticle() {

    companion object {
        private val log = LoggerFactory.getLogger(SkywalkingMonitor::class.java)
    }

    override suspend fun start() {
        log.debug("Setting up Apache SkyWalking monitor")
        setup()
        log.info("Successfully setup Apache SkyWalking monitor")
    }

    private suspend fun setup() {
        val client = ApolloClient.builder()
            .serverUrl(config.getString("graphql_endpoint"))
            .build()

        val response = client.query(GetTimeInfoQuery()).toDeferred().await()
        if (response.hasErrors()) {
            log.error("Failed to get Apache SkyWalking time info. Response: $response")
            throw RuntimeException("Failed to get time info") //todo: throw more appropriate error
        } else {
            val timezone = Integer.parseInt(response.data!!.result!!.timezone)
            val skywalkingClient = SkywalkingClient(vertx, client, timezone)

            vertx.deployVerticleAwait(ServiceTracker(skywalkingClient))
            vertx.deployVerticleAwait(ServiceInstanceTracker(skywalkingClient))
            vertx.deployVerticleAwait(EndpointTracker(skywalkingClient))
            vertx.deployVerticleAwait(EndpointMetricsTracker(skywalkingClient))
            vertx.deployVerticleAwait(EndpointTracesTracker(skywalkingClient))
        }
    }
}
