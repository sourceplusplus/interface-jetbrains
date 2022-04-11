/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package spp.jetbrains.monitor.skywalking

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.network.okHttpClient
import eu.geekplace.javapinning.JavaPinning
import eu.geekplace.javapinning.pin.Pin
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import monitor.skywalking.protocol.metadata.GetTimeInfoQuery
import okhttp3.OkHttpClient
import org.slf4j.LoggerFactory
import spp.jetbrains.monitor.skywalking.bridge.*
import spp.jetbrains.monitor.skywalking.service.SWLiveService
import spp.jetbrains.monitor.skywalking.service.SWLiveViewService
import spp.protocol.SourceServices
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SkywalkingMonitor(
    private val serverUrl: String,
    private val jwtToken: String? = null,
    private val certificatePins: List<String> = emptyList(),
    private val verifyHost: Boolean,
    private val currentService: String? = null
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
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .addHeader("spp-skywalking-reroute", "true")
                        .build()
                )
            }
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
        if (serverUrl.startsWith("https") && !verifyHost) {
            val naiveTrustManager = object : X509TrustManager {
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) = Unit
                override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) = Unit
            }
            httpBuilder.sslSocketFactory(SSLContext.getInstance("TLSv1.2").apply {
                val trustAllCerts = arrayOf<TrustManager>(naiveTrustManager)
                init(null, trustAllCerts, SecureRandom())
            }.socketFactory, naiveTrustManager)
        } else if (certificatePins.isNotEmpty()) {
            httpBuilder.sslSocketFactory(
                JavaPinning.forPins(certificatePins.map { Pin.fromString("CERTSHA256:$it") }).socketFactory,
                JavaPinning.trustManagerForPins(certificatePins.map { Pin.fromString("CERTSHA256:$it") })
            )
        }
        val client = ApolloClient.Builder()
            .serverUrl(serverUrl)
            .okHttpClient(httpBuilder.build())
            .build()

        val response = client.query(GetTimeInfoQuery()).execute()
        if (response.hasErrors()) {
            response.errors!!.forEach { log.error(it.message) }
            throw RuntimeException("Failed to get Apache SkyWalking time info")
        } else {
            val timezone = Integer.parseInt(response.data!!.result!!.timezone) / 100
            val skywalkingClient = SkywalkingClient(vertx, client, timezone)

            vertx.deployVerticle(GeneralBridge(skywalkingClient)).await()
            vertx.deployVerticle(ServiceBridge(skywalkingClient, currentService)).await()
            vertx.deployVerticle(ServiceInstanceBridge(skywalkingClient)).await()
            vertx.deployVerticle(EndpointBridge(skywalkingClient)).await()
            vertx.deployVerticle(EndpointMetricsBridge(skywalkingClient)).await()
            vertx.deployVerticle(EndpointTracesBridge(skywalkingClient)).await()
            vertx.deployVerticle(LogsBridge(skywalkingClient)).await()

            if (SourceServices.Instance.liveService == null) {
                val swLiveService = SWLiveService()
                vertx.deployVerticle(swLiveService).await()
                SourceServices.Instance.liveService = swLiveService
            }
            if (SourceServices.Instance.liveView == null) {
                val swLiveViewService = SWLiveViewService()
                vertx.deployVerticle(swLiveViewService).await()
                SourceServices.Instance.liveView = swLiveViewService
            }
        }
    }
}
