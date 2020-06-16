package com.sourceplusplus.core.api.admin

import com.sourceplusplus.api.model.QueryTimeFrame
import com.sourceplusplus.api.model.application.SourceApplication
import com.sourceplusplus.api.model.artifact.SourceArtifact
import com.sourceplusplus.api.model.artifact.SourceArtifactConfig
import com.sourceplusplus.api.model.artifact.SourceArtifactSubscriptionType
import com.sourceplusplus.api.model.metric.ArtifactMetricSubscribeRequest
import com.sourceplusplus.api.model.metric.MetricType
import com.sourceplusplus.core.api.SourceCoreAPITest
import io.vertx.ext.unit.TestSuite
import org.junit.Test

class AdminAPITest extends SourceCoreAPITest {

    @Test
    void getInfo() {
        TestSuite.create("AdminAPITest-getInfo").test("getInfoTest", { test ->
            def async = test.async()
            coreClient.info({
                if (it.failed()) {
                    test.fail(it.cause())
                }
                test.assertTrue(it.succeeded())
                test.assertNotNull(it.result())
                async.complete()
            })
        }).run().awaitSuccess()
    }

    @Test
    void searchForNewEndpointsTest() {
        TestSuite.create("AdminAPITest-searchForNewEndpointsTest").test("searchForNewEndpointsTest", { test ->
            def async = test.async()
            coreClient.searchForNewEndpoints({
                if (it.failed()) {
                    test.fail(it.cause())
                }
                test.assertTrue(it.succeeded())
                async.complete()
            })
        }).run().awaitSuccess()
    }

    @Test
    void getApplicationSubscriptionsTest() {
        SourceApplication application = null
        TestSuite.create("AdminAPITest-getApplicationSubscriptionsTest").before({ test ->
            def async = test.async()
            coreClient.createApplication({
                if (it.failed()) {
                    test.fail(it.cause())
                }
                application = it.result()
                async.complete()
            })
        }).test("subscribeToArtifacts", { test ->
            def artifact = SourceArtifact.builder()
                    .artifactQualifiedName("auto-subscribe")
                    .config(SourceArtifactConfig.builder().subscribeAutomatically(true).build())
                    .build()
            coreClient.upsertArtifact(application.appUuid(), artifact, test.asyncAssertSuccess())

            def metricSubscribeRequest = ArtifactMetricSubscribeRequest.builder()
                    .appUuid(application.appUuid())
                    .artifactQualifiedName("manual-subscribe")
                    .timeFrame(QueryTimeFrame.LAST_5_MINUTES)
                    .addMetricTypes(MetricType.Throughput_Average)
                    .build()
            coreClient.subscribeToArtifact(metricSubscribeRequest, test.asyncAssertSuccess())
        }).test("getApplicationSubscriptions", { test ->
            def async = test.async(2)
            coreClient.getApplicationSubscriptions(application.appUuid(), true, {
                if (it.failed()) {
                    test.fail(it.cause())
                }

                def artifactSubscriptions = it.result()
                test.assertEquals(2, artifactSubscriptions.size())

                def autoSubscribe = artifactSubscriptions.find { it.artifactQualifiedName() == "auto-subscribe" }
                test.assertNotNull(autoSubscribe)
                test.assertEquals(0, autoSubscribe.subscribers())
                test.assertEquals(0, autoSubscribe.types().size())
                test.assertTrue(autoSubscribe.automaticSubscription())

                def manualSubscribe = artifactSubscriptions.find { it.artifactQualifiedName() == "manual-subscribe" }
                test.assertNotNull(manualSubscribe)
                test.assertEquals(1, manualSubscribe.subscribers())
                test.assertEquals(1, manualSubscribe.types().size())
                test.assertNotNull(manualSubscribe.types().contains(SourceArtifactSubscriptionType.METRICS))
                test.assertFalse(manualSubscribe.automaticSubscription())
                async.countDown()
            })
            coreClient.getApplicationSubscriptions(application.appUuid(), false, {
                if (it.failed()) {
                    test.fail(it.cause())
                }

                def artifactSubscriptions = it.result()
                test.assertEquals(1, artifactSubscriptions.size())

                def manualSubscribe = artifactSubscriptions.find { it.artifactQualifiedName() == "manual-subscribe" }
                test.assertNotNull(manualSubscribe)
                test.assertEquals(1, manualSubscribe.subscribers())
                test.assertEquals(1, manualSubscribe.types().size())
                test.assertNotNull(manualSubscribe.types().contains(SourceArtifactSubscriptionType.METRICS))
                test.assertFalse(manualSubscribe.automaticSubscription())
                async.countDown()
            })
        }).run().awaitSuccess()
    }
}