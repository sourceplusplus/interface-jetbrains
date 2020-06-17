package com.sourceplusplus.core.api.trace.track

import com.sourceplusplus.api.bridge.PluginBridgeEndpoints
import com.sourceplusplus.api.model.QueryTimeFrame
import com.sourceplusplus.api.model.SourceMessage
import com.sourceplusplus.api.model.application.SourceApplication
import com.sourceplusplus.api.model.artifact.SourceArtifactConfig
import com.sourceplusplus.api.model.trace.ArtifactTraceResult
import com.sourceplusplus.api.model.trace.ArtifactTraceSubscribeRequest
import com.sourceplusplus.api.model.trace.TraceOrderType
import com.sourceplusplus.core.api.SourceCoreAPITest
import io.vertx.core.Vertx
import io.vertx.ext.unit.TestSuite
import org.junit.Test

/**
 * @version 0.3.0
 * @since 0.3.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class TracePublishListenerTest extends SourceCoreAPITest {

    @Test
    void "listen_to_published_artifact_traces"() {
        SourceApplication application
        TestSuite.create("listen_to_published_artifact_traces-setup").before({ test ->
            def async = test.async()
            createApplication(test, {
                application = it

                coreClient.createOrUpdateArtifactConfig(it.appUuid(), "com.company.TestClass.testMethod()",
                        SourceArtifactConfig.builder().endpoint(true).addEndpointIds("1").build(), {
                    if (it.succeeded()) {
                        def traceSubscribeRequest = ArtifactTraceSubscribeRequest.builder()
                                .addOrderTypes(TraceOrderType.LATEST_TRACES)
                                .timeFrame(QueryTimeFrame.LAST_5_MINUTES)
                                .appUuid(application.appUuid())
                                .artifactQualifiedName("com.company.TestClass.testMethod()").build()
                        coreClient.subscribeToArtifact(traceSubscribeRequest, {
                            if (it.succeeded()) {
                                async.complete()
                            } else {
                                test.fail(it.cause())
                            }
                        })
                    } else {
                        test.fail(it.cause())
                    }
                })
            })
        }).test("listen_to_published_artifact_traces", { test ->
            def vertx = Vertx.vertx()
            SourceMessage.registerCodecs(vertx)

            def async = test.async(2)
            vertx.eventBus().consumer(PluginBridgeEndpoints.ARTIFACT_TRACE_UPDATED.address, {
                def traceResult = it.body() as ArtifactTraceResult
                test.assertEquals(application.appUuid(), traceResult.appUuid())
                test.assertEquals("com.company.TestClass.testMethod()", traceResult.artifactQualifiedName())
                test.assertEquals(TraceOrderType.LATEST_TRACES, traceResult.orderType())
                async.countDown()
            })

            coreClient.attachBridge(vertx, apiHost, apiPort, apiSslEnabled)
        }).run().awaitSuccess()
    }
}
