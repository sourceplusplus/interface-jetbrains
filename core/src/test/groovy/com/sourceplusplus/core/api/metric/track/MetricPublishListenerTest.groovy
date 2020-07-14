package com.sourceplusplus.core.api.metric.track

import com.sourceplusplus.api.bridge.PluginBridgeEndpoints
import com.sourceplusplus.api.model.QueryTimeFrame
import com.sourceplusplus.api.model.SourceMessage
import com.sourceplusplus.api.model.application.SourceApplication
import com.sourceplusplus.api.model.artifact.SourceArtifactConfig
import com.sourceplusplus.api.model.metric.ArtifactMetricResult
import com.sourceplusplus.api.model.metric.ArtifactMetricSubscribeRequest
import com.sourceplusplus.core.api.SourceCoreAPITest
import io.vertx.core.Vertx
import io.vertx.ext.unit.TestSuite
import org.junit.Test

import static com.sourceplusplus.api.model.metric.MetricType.Throughput_Average

/**
 * @version 0.3.2
 * @since 0.3.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class MetricPublishListenerTest extends SourceCoreAPITest {

    @Test
    void "listen_to_published_artifact_metrics"() {
        SourceApplication application
        TestSuite.create("listen_to_published_artifact_metrics-setup").before({ test ->
            def async = test.async()
            createApplication(test, {
                application = it

                coreClient.createOrUpdateArtifactConfig(it.appUuid(), "com.company.TestClass.testMethod()",
                        SourceArtifactConfig.builder().endpoint(true).addEndpointIds("1").build(), {
                    if (it.succeeded()) {
                        def metricSubscribeRequest = ArtifactMetricSubscribeRequest.builder()
                                .addMetricTypes(Throughput_Average)
                                .timeFrame(QueryTimeFrame.LAST_5_MINUTES)
                                .appUuid(application.appUuid())
                                .artifactQualifiedName("com.company.TestClass.testMethod()").build()
                        coreClient.subscribeToArtifact(metricSubscribeRequest, {
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
        }).test("listen_to_published_artifact_metrics", { test ->
            def vertx = Vertx.vertx()
            SourceMessage.registerCodecs(vertx)

            def async = test.async(2)
            vertx.eventBus().consumer(PluginBridgeEndpoints.ARTIFACT_METRIC_UPDATED.address, {
                def metricResult = it.body() as ArtifactMetricResult
                test.assertEquals(application.appUuid(), metricResult.appUuid())
                test.assertEquals("com.company.TestClass.testMethod()", metricResult.artifactQualifiedName())
                test.assertEquals(QueryTimeFrame.LAST_5_MINUTES, metricResult.timeFrame())
                test.assertEquals(1, metricResult.artifactMetrics().size())
                test.assertEquals(Throughput_Average, metricResult.artifactMetrics()[0].metricType())
                async.countDown()
            })

            coreClient.attachBridge(vertx, apiHost, apiPort, apiSslEnabled)
        }).run().awaitSuccess()
    }
}
