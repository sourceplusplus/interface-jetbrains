package com.sourceplusplus.core.api.trace.track

import com.sourceplusplus.api.model.QueryTimeFrame
import com.sourceplusplus.api.model.application.SourceApplication
import com.sourceplusplus.api.model.artifact.ArtifactSubscribeRequest
import com.sourceplusplus.api.model.trace.ArtifactTraceSubscribeRequest
import com.sourceplusplus.api.model.trace.ArtifactTraceUnsubscribeRequest
import com.sourceplusplus.core.api.SourceCoreAPITest
import io.vertx.ext.unit.TestSuite
import org.junit.Test

import static com.sourceplusplus.api.model.trace.TraceOrderType.*

/**
 * @version 0.3.2
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class TraceSubscriptionTrackerTest extends SourceCoreAPITest {

    @Test
    void "validate_subscription_create_date"() {
        SourceApplication application
        ArtifactTraceSubscribeRequest traceSubscribeRequest
        ArtifactSubscribeRequest originalSubscription
        TestSuite.create("validate_subscription_create_date-setup").before({ test ->
            def async = test.async()
            createApplication(test, {
                application = it
                async.complete()
            })
        }).test("subscribeToArtifact", { test ->
            def async = test.async()
            traceSubscribeRequest = ArtifactTraceSubscribeRequest.builder()
                    .appUuid(application.appUuid())
                    .artifactQualifiedName("manual-subscribe")
                    .timeFrame(QueryTimeFrame.LAST_5_MINUTES)
                    .addOrderTypes(LATEST_TRACES)
                    .build()
            coreClient.subscribeToArtifact(traceSubscribeRequest, {
                coreClient.getArtifactSubscriptions(application.appUuid(), "manual-subscribe", {
                    if (it.succeeded()) {
                        test.assertEquals(1, it.result().size())
                        test.assertNotNull(it.result()[0].subscribeDate())

                        originalSubscription = it.result()[0]
                        async.complete()
                    } else {
                        test.fail(it.cause())
                    }
                })
            })
        }).test("validate_subscription_create_date_stays_same", { test ->
            def async = test.async()
            coreClient.subscribeToArtifact(traceSubscribeRequest, {
                if (it.succeeded()) {
                    coreClient.getArtifactSubscriptions(application.appUuid(), "manual-subscribe", {
                        if (it.succeeded()) {
                            test.assertEquals(1, it.result().size())
                            test.assertNotNull(it.result()[0].subscribeDate())
                            test.assertEquals(originalSubscription, it.result()[0])
                            test.assertEquals(originalSubscription.subscribeDate(), it.result()[0].subscribeDate())
                            async.complete()
                        } else {
                            test.fail(it.cause())
                        }
                    })
                } else {
                    test.fail(it.cause())
                }
            })
        }).test("validate_subscription_create_date_updates", { test ->
            def async = test.async()
            coreClient.subscribeToArtifact(traceSubscribeRequest.withOrderTypes(LATEST_TRACES, SLOWEST_TRACES), {
                if (it.succeeded()) {
                    coreClient.getArtifactSubscriptions(application.appUuid(), "manual-subscribe", {
                        if (it.succeeded()) {
                            test.assertEquals(1, it.result().size())
                            test.assertNotNull(it.result()[0].subscribeDate())
                            test.assertNotEquals(originalSubscription, it.result()[0])
                            test.assertTrue(originalSubscription.subscribeDate().toEpochMilli() < it.result()[0].subscribeDate().toEpochMilli())
                            async.complete()
                        } else {
                            test.fail(it.cause())
                        }
                    })
                } else {
                    test.fail(it.cause())
                }
            })
        }).run().awaitSuccess()
    }

    @Test
    void "subscribe_to_artifact_traces"() {
        SourceApplication application
        TestSuite.create("subscribe_to_artifact_traces-setup").before({ test ->
            def async = test.async()
            createApplication(test, {
                application = it
                async.complete()
            })
        }).test("subscribe_to_artifact_traces", { test ->
            def async = test.async()
            def traceSubscribeRequest = ArtifactTraceSubscribeRequest.builder()
                    .timeFrame(QueryTimeFrame.LAST_5_MINUTES)
                    .addOrderTypes(LATEST_TRACES)
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
            def async = test.async()
            createApplication(test, {
                application = it

                def traceSubscribeRequest = ArtifactTraceSubscribeRequest.builder()
                        .timeFrame(QueryTimeFrame.LAST_5_MINUTES)
                        .addOrderTypes(LATEST_TRACES)
                        .appUuid(application.appUuid())
                        .artifactQualifiedName("com.company.TestClass.testMethod()").build()
                coreClient.subscribeToArtifact(traceSubscribeRequest, {
                    if (it.failed()) {
                        test.fail(it.cause())
                    }
                    test.assertTrue(it.result())
                    async.complete()
                })
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
                test.assertEquals(QueryTimeFrame.LAST_5_MINUTES, subscription.timeFrame())
                test.assertEquals(1, subscription.orderTypes().size())
                test.assertTrue(subscription.orderTypes().contains(LATEST_TRACES))
                async.countDown()
            })
        }).run().awaitSuccess()
    }

    @Test
    void "verify_unsubscribed_to_artifact_traces"() {
        SourceApplication application
        TestSuite.create("verify_unsubscribed_to_artifact_traces-setup").before({ test ->
            def async = test.async()
            createApplication(test, {
                application = it

                def traceSubscribeRequest = ArtifactTraceSubscribeRequest.builder()
                        .timeFrame(QueryTimeFrame.LAST_5_MINUTES)
                        .addOrderTypes(LATEST_TRACES)
                        .appUuid(application.appUuid())
                        .artifactQualifiedName("com.company.TestClass.testMethod()").build()
                coreClient.subscribeToArtifact(traceSubscribeRequest, {
                    if (it.failed()) {
                        test.fail(it.cause())
                    }
                    test.assertTrue(it.result())
                    async.complete()
                })
            })
        }).test("verify_unsubscribed_to_artifact_traces", { test ->
            def async = test.async()
            def unsubTraceRequest = ArtifactTraceUnsubscribeRequest.builder()
                    .appUuid(application.appUuid())
                    .addRemoveOrderTypes(LATEST_TRACES)
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
