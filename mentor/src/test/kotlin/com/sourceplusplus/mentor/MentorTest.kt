package com.sourceplusplus.mentor

import com.sourceplusplus.monitor.skywalking.SkywalkingClient
import com.sourceplusplus.monitor.skywalking.SkywalkingMonitor
import com.sourceplusplus.monitor.skywalking.bridge.ServiceBridge
import com.sourceplusplus.protocol.artifact.trace.TraceResult
import com.sourceplusplus.protocol.artifact.trace.TraceSpanStackQueryResult
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.runBlocking
import org.junit.Before

open class MentorTest {
    val vertx: Vertx = Vertx.vertx()
    private var setup = false

    @Before
    fun setupAndWaitForApacheSkywalking() {
        if (!setup) {
            vertx.eventBus().registerDefaultCodec(
                TraceResult::class.java,
                SkywalkingClient.LocalMessageCodec<TraceResult>()
            )
            vertx.eventBus().registerDefaultCodec(
                TraceSpanStackQueryResult::class.java,
                SkywalkingClient.LocalMessageCodec<TraceSpanStackQueryResult>()
            )

            runBlocking(vertx.dispatcher()) {
                vertx.deployVerticle(SkywalkingMonitor("http://172.17.0.1:12800/graphql")).await()
                ServiceBridge.getCurrentServiceAwait(vertx)
            }
            setup = true
        }
    }
}
