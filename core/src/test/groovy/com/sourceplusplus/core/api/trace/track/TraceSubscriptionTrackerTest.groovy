package com.sourceplusplus.core.api.trace.track

import com.sourceplusplus.api.model.QueryTimeFrame
import com.sourceplusplus.api.model.application.SourceApplication
import com.sourceplusplus.api.model.trace.ArtifactTraceSubscribeRequest
import com.sourceplusplus.api.model.trace.ArtifactTraceUnsubscribeRequest
import com.sourceplusplus.api.model.trace.TraceOrderType
import com.sourceplusplus.core.api.SourceCoreAPITest
import io.vertx.ext.unit.TestSuite
import org.junit.Test

/**
 * @version 0.2.6
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class TraceSubscriptionTrackerTest extends SourceCoreAPITest {

    @Test
    void "subscribe_to_artifact_traces"() {
        SourceApplication application
        TestSuite.create("subscribe_to_artifact_traces-setup").before({ test ->
            def async = test.async(1)
            createApplication(test, {
                application = it
                async.countDown()
            })
        }).test("subscribe_to_artifact_traces", { test ->
            def async = test.async()
            def traceSubscribeRequest = ArtifactTraceSubscribeRequest.builder()
                    .addOrderTypes(TraceOrderType.LATEST_TRACES)
                    .appUuid(application.appUuid())
                    .artifactQualifiedName("com.company.TestClass.testMethod()").build()
            coreClient.subscribeToArtifact(traceSubscribeRequest, {
                if (it.failed()) {
                    test.fail(it.cause())
                }
                test.assertTrue(it.result())
                async.countDown()
            })
        }).run().awaitSuccess()
    }

    @Test
    void "verify_subscribed_to_artifact_traces"() {
        SourceApplication application
        TestSuite.create("verify_subscribed_to_artifact_traces-setup").before({ test ->
            def async = test.async(2)
            createApplication(test, {
                application = it
                async.countDown()
            })

            def traceSubscribeRequest = ArtifactTraceSubscribeRequest.builder()
                    .timeFrame(QueryTimeFrame.LAST_15_MINUTES)
                    .addOrderTypes(TraceOrderType.LATEST_TRACES)
                    .appUuid(application.appUuid())
                    .artifactQualifiedName("com.company.TestClass.testMethod()").build()
            coreClient.subscribeToArtifact(traceSubscribeRequest, {
                if (it.failed()) {
                    test.fail(it.cause())
                }
                test.assertTrue(it.result())
                async.countDown()
            })
        }).test("verify_subscribed_to_artifact_traces", { test ->
            def async = test.async()
            coreClient.getSubscriberApplicationSubscriptions(application.appUuid(), {
                if (it.failed()) {
                    test.fail(it.cause())
                }

                def subscriptions = it.result()
                test.assertEquals(1, subscriptions.size())
                test.assertEquals(ArtifactTraceSubscribeRequest.class, subscriptions.get(0).class)

                def subscription = subscriptions.get(0) as ArtifactTraceSubscribeRequest
                test.assertEquals("com.company.TestClass.testMethod()", subscription.artifactQualifiedName())
                test.assertEquals(QueryTimeFrame.LAST_15_MINUTES, subscription.timeFrame())
                test.assertEquals(1, subscription.orderTypes().size())
                test.assertTrue(subscription.orderTypes().contains(TraceOrderType.LATEST_TRACES))
                async.countDown()
            })
        }).run().awaitSuccess()
    }

    @Test
    void "verify_unsubscribed_to_artifact_traces"() {
        SourceApplication application
        TestSuite.create("verify_unsubscribed_to_artifact_traces-setup").before({ test ->
            def async = test.async(2)
            createApplication(test, {
                application = it
                async.countDown()
            })

            def traceSubscribeRequest = ArtifactTraceSubscribeRequest.builder()
                    .timeFrame(QueryTimeFrame.LAST_15_MINUTES)
                    .addOrderTypes(TraceOrderType.LATEST_TRACES)
                    .appUuid(application.appUuid())
                    .artifactQualifiedName("com.company.TestClass.testMethod()").build()
            coreClient.subscribeToArtifact(traceSubscribeRequest, {
                if (it.failed()) {
                    test.fail(it.cause())
                }
                test.assertTrue(it.result())
                async.countDown()
            })
        }).test("verify_unsubscribed_to_artifact_traces", { test ->
            def async = test.async()
            def unsubTraceRequest = ArtifactTraceUnsubscribeRequest.builder()
                    .appUuid(application.appUuid())
                    .addRemoveOrderTypes(TraceOrderType.LATEST_TRACES)
                    .artifactQualifiedName("com.company.TestClass.testMethod()").build()

            coreClient.unsubscribeFromArtifactTraces(unsubTraceRequest, {
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
