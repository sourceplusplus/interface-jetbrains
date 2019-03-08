package com.sourceplusplus.core.api.metric.track

import com.sourceplusplus.api.model.application.SourceApplication
import com.sourceplusplus.api.model.metric.ArtifactMetricSubscribeRequest
import com.sourceplusplus.api.model.metric.ArtifactMetricUnsubscribeRequest
import com.sourceplusplus.api.model.QueryTimeFrame
import com.sourceplusplus.core.api.SourceCoreAPITest
import io.vertx.ext.unit.TestSuite
import org.junit.Test

/**
 * @version 0.1.1
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class MetricSubscriptionTrackerTest extends SourceCoreAPITest {

    @Test
    void "subscribe_to_artifact_metrics"() {
        SourceApplication application
        TestSuite.create("subscribe_to_artifact_metrics-setup").before({ test ->
            def async = test.async(1)
            createApplication(test, {
                application = it
                async.countDown()
            })
        }).test("subscribe_to_artifact_metrics", { test ->
            def async = test.async()
            def metricSubscribeRequest = ArtifactMetricSubscribeRequest.builder()
                    .timeFrame(QueryTimeFrame.LAST_15_MINUTES)
                    .appUuid(application.appUuid())
                    .artifactQualifiedName("com.company.TestClass.testMethod()").build()
            coreClient.subscribeToArtifact(metricSubscribeRequest, {
                if (it.failed()) {
                    test.fail(it.cause())
                }
                test.assertTrue(it.result())
                async.countDown()
            })
        }).run().awaitSuccess()
    }

    @Test
    void "verify_unsubscribed_to_artifact_metrics"() {
        SourceApplication application
        TestSuite.create("verify_unsubscribed_to_artifact_metrics-setup").before({ test ->
            def async = test.async(2)
            createApplication(test, {
                application = it
                async.countDown()
            })

            def metricSubscribeRequest = ArtifactMetricSubscribeRequest.builder()
                    .timeFrame(QueryTimeFrame.LAST_15_MINUTES)
                    .appUuid(application.appUuid())
                    .artifactQualifiedName("com.company.TestClass.testMethod()").build()
            coreClient.subscribeToArtifact(metricSubscribeRequest, {
                if (it.failed()) {
                    test.fail(it.cause())
                }
                test.assertTrue(it.result())
                async.countDown()
            })
        }).test("verify_unsubscribed_to_artifact_metrics", { test ->
            def async = test.async()
            def unsubMetricRequest = ArtifactMetricUnsubscribeRequest.builder()
                    .appUuid(application.appUuid())
                    .addRemoveTimeFrames(QueryTimeFrame.LAST_15_MINUTES)
                    .artifactQualifiedName("com.company.TestClass.testMethod()").build()
            coreClient.unsubscribeFromArtifactMetrics(unsubMetricRequest, {
                if (it.failed()) {
                    test.fail(it.cause())
                }
                test.assertTrue(it.result())

                coreClient.getApplicationSubscriptions(application.appUuid(), true, {
                    if (it.failed()) {
                        test.fail(it.cause())
                    }
                    test.assertTrue(it.result().isEmpty())
                    async.complete()
                })
            })
        }).run().awaitSuccess()
    }
}
