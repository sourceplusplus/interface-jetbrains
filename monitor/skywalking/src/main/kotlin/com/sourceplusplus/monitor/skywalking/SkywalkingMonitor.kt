package com.sourceplusplus.monitor.skywalking

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.coroutines.await
import com.sourceplusplus.monitor.skywalking.bridge.*
import eu.geekplace.javapinning.JavaPinning
import eu.geekplace.javapinning.pin.Pin
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import monitor.skywalking.protocol.metadata.GetTimeInfoQuery
import okhttp3.OkHttpClient
import org.slf4j.LoggerFactory

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SkywalkingMonitor(
    private val serverUrl: String,
    private val jwtToken: String? = null,
    private val certificatePins: List<String> = emptyList()
) : CoroutineVerticle() {

    companion object {
        private val log = LoggerFactory.getLogger(SkywalkingMonitor::class.java)
    }

    override suspend fun start() {
        log.debug("Setting up Apache SkyWalking monitor")
        setup()
        log.info("Successfully setup Apache SkyWalking monitor")
    }

    @Suppress("MagicNumber")
    private suspend fun setup() {
        log.debug("Apache SkyWalking server: $serverUrl")
        val httpBuilder = OkHttpClient().newBuilder()
            .hostnameVerifier { _, _ -> true }
        if (!jwtToken.isNullOrEmpty()) {
            httpBuilder.addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("Authorization", "Bearer $jwtToken")
                        .build()
                )
            }
        }
        if (certificatePins.isNotEmpty()) {
            httpBuilder.sslSocketFactory(
                JavaPinning.forPins(certificatePins.map { Pin.fromString("CERTSHA256:$it") }).socketFactory,
                JavaPinning.trustManagerForPins(certificatePins.map { Pin.fromString("CERTSHA256:$it") })
            )
        }
        val client = ApolloClient.builder()
            .serverUrl(serverUrl)
            .okHttpClient(httpBuilder.build())
            .build()

        val response = client.query(GetTimeInfoQuery()).await()
        if (response.hasErrors()) {
            response.errors!!.forEach { log.error(it.message) }
            throw RuntimeException("Failed to get Apache SkyWalking time info")
        } else {
            val timezone = Integer.parseInt(response.data!!.result!!.timezone) / 100
            val skywalkingClient = SkywalkingClient(vertx, client, timezone)

            vertx.deployVerticle(ServiceBridge(skywalkingClient)).await()
            vertx.deployVerticle(ServiceInstanceBridge(skywalkingClient)).await()
            vertx.deployVerticle(EndpointBridge(skywalkingClient)).await()
            vertx.deployVerticle(EndpointMetricsBridge(skywalkingClient)).await()
            vertx.deployVerticle(EndpointTracesBridge(skywalkingClient)).await()
            vertx.deployVerticle(LogsBridge(skywalkingClient)).await()
        }
    }
}
