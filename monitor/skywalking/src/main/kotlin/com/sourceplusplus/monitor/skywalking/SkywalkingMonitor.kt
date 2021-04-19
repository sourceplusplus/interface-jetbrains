package com.sourceplusplus.monitor.skywalking

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.coroutines.await
import com.sourceplusplus.monitor.skywalking.bridge.*
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import monitor.skywalking.protocol.metadata.GetTimeInfoQuery
import okhttp3.OkHttpClient
import org.slf4j.LoggerFactory
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
    private val jwtToken: String? = null
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
        val client = if (jwtToken == null) {
            ApolloClient.builder()
                .serverUrl(serverUrl)
                .build()
        } else {
            ApolloClient.builder()
                .serverUrl(serverUrl)
                .okHttpClient(getUnsafeOkHttpClient().newBuilder().addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .header("Authorization", "Bearer $jwtToken")
                            .build()
                    )
                }.build())
                .build()
        }

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

    //todo: remove when possible
    private fun getUnsafeOkHttpClient(): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun getAcceptedIssuers() = arrayOf<X509Certificate>()
        })
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        val sslSocketFactory = sslContext.socketFactory
        return OkHttpClient.Builder()
            .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }.build()
    }
}
