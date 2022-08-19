/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package spp.jetbrains.monitor.skywalking

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.network.okHttpClient
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import eu.geekplace.javapinning.JavaPinning
import eu.geekplace.javapinning.pin.Pin
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import monitor.skywalking.protocol.metadata.GetTimeInfoQuery
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.OkHttpClient
import okhttp3.Protocol
import spp.jetbrains.monitor.skywalking.bridge.*
import spp.jetbrains.monitor.skywalking.impl.SkywalkingMonitorServiceImpl
import spp.jetbrains.monitor.skywalking.service.SWLiveService
import spp.jetbrains.monitor.skywalking.service.SWLiveViewService
import spp.jetbrains.sourcemarker.status.SourceStatus.ConnectionError
import spp.jetbrains.sourcemarker.status.SourceStatusService
import spp.protocol.service.LiveInstrumentService
import spp.protocol.service.LiveService
import spp.protocol.service.LiveViewService
import java.io.IOException
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Proxy
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
    private val currentService: String? = null,
    private val project: Project
) : CoroutineVerticle() {

    @Suppress("unused", "FunctionName")
    companion object {
        private val log = logger<SkywalkingMonitor>()

        @JvmField
        val LIVE_SERVICE = Key.create<LiveService>("SPP_LIVE_SERVICE")
        fun getLIVE_SERVICE() = LIVE_SERVICE

        @JvmField
        val LIVE_VIEW_SERVICE = Key.create<LiveViewService>("SPP_LIVE_VIEW_SERVICE")
        fun getLIVE_VIEW_SERVICE() = LIVE_VIEW_SERVICE

        @JvmField
        val LIVE_INSTRUMENT_SERVICE = Key.create<LiveInstrumentService>("SPP_LIVE_INSTRUMENT_SERVICE")
        fun getLIVE_INSTRUMENT_SERVICE() = LIVE_INSTRUMENT_SERVICE
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
            .eventListener(object : EventListener() {
//                override fun callStart(call: Call) {
//                    log.debug("Call start: ${call.request()}")
//                }

                override fun connectFailed(
                    call: Call,
                    inetSocketAddress: InetSocketAddress,
                    proxy: Proxy,
                    protocol: Protocol?,
                    ioe: IOException
                ) {
                    SourceStatusService.getInstance(project).update(ConnectionError, ioe.message)
                }

                override fun callFailed(call: Call, ioe: IOException) {
                    if (ioe is ConnectException) {
                        SourceStatusService.getInstance(project).update(ConnectionError, ioe.message)
                    } else {
                        log.warn("Apache SkyWalking call failed. Request: ${call.request()}", ioe)
                    }
                }
            })
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
            vertx.deployVerticle(ServiceBridge(project, skywalkingClient, currentService)).await()
            vertx.deployVerticle(ServiceInstanceBridge(skywalkingClient)).await()
            vertx.deployVerticle(EndpointBridge(project, skywalkingClient)).await()
            vertx.deployVerticle(EndpointMetricsBridge(skywalkingClient)).await()
            vertx.deployVerticle(EndpointTracesBridge(skywalkingClient)).await()
            vertx.deployVerticle(LogsBridge(skywalkingClient)).await()

            if (project.getUserData(LIVE_SERVICE) == null) {
                val swLiveService = SWLiveService()
                vertx.deployVerticle(swLiveService).await()
                project.putUserData(LIVE_SERVICE, swLiveService)
            }
            if (project.getUserData(LIVE_VIEW_SERVICE) == null) {
                val swLiveViewService = SWLiveViewService()
                vertx.deployVerticle(swLiveViewService).await()
                project.putUserData(LIVE_VIEW_SERVICE, swLiveViewService)
            }

            project.putUserData(SkywalkingMonitorService.KEY, SkywalkingMonitorServiceImpl(skywalkingClient))
        }
    }
}
