package com.sourceplusplus.core.api.metric.track

import com.sourceplusplus.api.model.QueryTimeFrame
import com.sourceplusplus.api.model.application.SourceApplication
import com.sourceplusplus.api.model.artifact.SourceArtifactSubscriptionType
import com.sourceplusplus.api.model.metric.ArtifactMetricSubscribeRequest
import com.sourceplusplus.api.model.metric.ArtifactMetricUnsubscribeRequest
import com.sourceplusplus.api.model.metric.TimeFramedMetricType
import com.sourceplusplus.core.api.SourceCoreAPITest
import io.vertx.core.CompositeFuture
import io.vertx.core.Promise
import io.vertx.ext.unit.TestSuite
import org.junit.Test

import static com.sourceplusplus.api.model.metric.MetricType.*

/**
 * @version 0.2.6
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class MetricSubscriptionTrackerTest extends SourceCoreAPITest {

    @Test
    void "update_subscribed_artifact_metrics"() {
        SourceApplication application
        TestSuite.create("update_subscribed_artifact_metrics-setup").before({ test ->
            def async = test.async()
            createApplication(test, {
                application = it
                async.complete()
            })
        }).test("update_subscribed_artifact_metrics", { test ->
            def async = test.async()
            def metricSubscribeRequest = ArtifactMetricSubscribeRequest.builder()
                    .addMetricTypes(Throughput_Average)
                    .timeFrame(QueryTimeFrame.LAST_15_MINUTES)
                    .appUuid(application.appUuid())
                    .artifactQualifiedName("com.company.TestClass.testMethod()").build()
            coreClient.subscribeToArtifact(metricSubscribeRequest, {
                if (it.succeeded()) {
                    coreClient.subscribeToArtifact(metricSubscribeRequest.withMetricTypes(ResponseTime_Average), {
                        if (it.succeeded()) {
                            coreClient.getSubscriberApplicationSubscriptions(application.appUuid(), {
                                if (it.failed()) {
                                    test.fail(it.cause())
                                }

                                def subscriptions = it.result()
                                test.assertEquals(1, subscriptions.size())
                                test.assertEquals(ArtifactMetricSubscribeRequest.class, subscriptions.get(0).class)

                                def subscription = subscriptions.get(0) as ArtifactMetricSubscribeRequest
                                test.assertEquals("com.company.TestClass.testMethod()", subscription.artifactQualifiedName())
                                test.assertEquals(SourceArtifactSubscriptionType.METRICS, subscription.type)
                                test.assertEquals(2, subscription.metricTypes().size())
                                test.assertTrue(subscription.metricTypes().contains(Throughput_Average))
                                test.assertTrue(subscription.metricTypes().contains(ResponseTime_Average))
                                async.complete()
                            })
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
                    .addMetricTypes(Throughput_Average)
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
    void "verify_subscribed_to_artifact_metrics"() {
        SourceApplication application
        TestSuite.create("verify_subscribed_to_artifact_metrics-setup").before({ test ->
            def async = test.async(2)
            createApplication(test, {
                application = it
                async.countDown()
            })

            def metricSubscribeRequest = ArtifactMetricSubscribeRequest.builder()
                    .addMetricTypes(Throughput_Average)
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
        }).test("verify_subscribed_to_artifact_metrics", { test ->
            def async = test.async()
            coreClient.getSubscriberApplicationSubscriptions(application.appUuid(), {
                if (it.failed()) {
                    test.fail(it.cause())
                }

                def subscriptions = it.result()
                test.assertEquals(1, subscriptions.size())
                test.assertEquals("com.company.TestClass.testMethod()", subscriptions.get(0).artifactQualifiedName())
                test.assertEquals(SourceArtifactSubscriptionType.METRICS, subscriptions.get(0).type)
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
                    .addMetricTypes(Throughput_Average)
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
                    .addRemoveMetricTypes(Throughput_Average)
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

    @Test
    void "unsubscribe_artifact_metric_type"() {
        SourceApplication application
        TestSuite.create("unsubscribe_artifact_metric_type-setup").before({ test ->
            def async = test.async()
            createApplication(test, {
                application = it
                async.complete()
            })
        }).test("unsubscribe_artifact_metric_type", { test ->
            def async = test.async()
            def metricSubscribeRequest = ArtifactMetricSubscribeRequest.builder()
                    .addMetricTypes(Throughput_Average, ResponseTime_Average)
                    .timeFrame(QueryTimeFrame.LAST_15_MINUTES)
                    .appUuid(application.appUuid())
                    .artifactQualifiedName("com.company.TestClass.testMethod()").build()
            coreClient.subscribeToArtifact(metricSubscribeRequest, {
                if (it.succeeded()) {
                    def metricUnsubscribeRequest = ArtifactMetricUnsubscribeRequest.builder()
                            .addRemoveMetricTypes(ResponseTime_Average)
                            .appUuid(application.appUuid())
                            .artifactQualifiedName("com.company.TestClass.testMethod()").build()

                    coreClient.unsubscribeFromArtifactMetrics(metricUnsubscribeRequest, {
                        if (it.succeeded()) {
                            coreClient.getSubscriberApplicationSubscriptions(application.appUuid(), {
                                if (it.failed()) {
                                    test.fail(it.cause())
                                }

                                def subscriptions = it.result()
                                test.assertEquals(1, subscriptions.size())
                                test.assertEquals(ArtifactMetricSubscribeRequest.class, subscriptions.get(0).class)

                                def subscription = subscriptions.get(0) as ArtifactMetricSubscribeRequest
                                test.assertEquals("com.company.TestClass.testMethod()", subscription.artifactQualifiedName())
                                test.assertEquals(SourceArtifactSubscriptionType.METRICS, subscription.type)
                                test.assertEquals(1, subscription.metricTypes().size())
                                test.assertTrue(subscription.metricTypes().contains(Throughput_Average))
                                async.complete()
                            })
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
    void "unsubscribe_artifact_metric_time_frame"() {
        SourceApplication application
        TestSuite.create("unsubscribe_artifact_metric_time_frame-setup").before({ test ->
            def async = test.async()
            createApplication(test, {
                application = it
                async.complete()
            })
        }).test("unsubscribe_artifact_metric_time_frame", { test ->
            def async = test.async()
            def subRequest1 = Promise.promise().future()
            def metricSubscribeRequest = ArtifactMetricSubscribeRequest.builder()
                    .addMetricTypes(Throughput_Average)
                    .timeFrame(QueryTimeFrame.LAST_15_MINUTES)
                    .appUuid(application.appUuid())
                    .artifactQualifiedName("com.company.TestClass.testMethod()").build()
            coreClient.subscribeToArtifact(metricSubscribeRequest, subRequest1)
            def subRequest2 = Promise.promise().future()
            coreClient.subscribeToArtifact(metricSubscribeRequest.withTimeFrame(QueryTimeFrame.LAST_5_MINUTES), subRequest2)

            CompositeFuture.all(subRequest1, subRequest2).onComplete({
                if (it.succeeded()) {
                    def metricUnsubscribeRequest = ArtifactMetricUnsubscribeRequest.builder()
                            .addRemoveTimeFrames(QueryTimeFrame.LAST_5_MINUTES)
                            .appUuid(application.appUuid())
                            .artifactQualifiedName("com.company.TestClass.testMethod()").build()

                    coreClient.unsubscribeFromArtifactMetrics(metricUnsubscribeRequest, {
                        if (it.succeeded()) {
                            coreClient.getSubscriberApplicationSubscriptions(application.appUuid(), {
                                if (it.failed()) {
                                    test.fail(it.cause())
                                }

                                def subscriptions = it.result()
                                test.assertEquals(1, subscriptions.size())
                                test.assertEquals(ArtifactMetricSubscribeRequest.class, subscriptions.get(0).class)

                                def subscription = subscriptions.get(0) as ArtifactMetricSubscribeRequest
                                test.assertEquals("com.company.TestClass.testMethod()", subscription.artifactQualifiedName())
                                test.assertEquals(SourceArtifactSubscriptionType.METRICS, subscription.type)
                                test.assertEquals(QueryTimeFrame.LAST_15_MINUTES, subscription.timeFrame())
                                async.complete()
                            })
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
    void "unsubscribe_artifact_metric_time_framed_metric_type"() {
        SourceApplication application
        TestSuite.create("unsubscribe_artifact_metric_time_framed_metric_type-setup").before({ test ->
            def async = test.async()
            createApplication(test, {
                application = it
                async.complete()
            })
        }).test("unsubscribe_artifact_metric_time_framed_metric_type", { test ->
            def async = test.async()
            def metricSubscribeRequest = ArtifactMetricSubscribeRequest.builder()
                    .addMetricTypes(Throughput_Average, ResponseTime_Average)
                    .timeFrame(QueryTimeFrame.LAST_15_MINUTES)
                    .appUuid(application.appUuid())
                    .artifactQualifiedName("com.company.TestClass.testMethod()").build()
            coreClient.subscribeToArtifact(metricSubscribeRequest, {
                if (it.succeeded()) {
                    def metricUnsubscribeRequest = ArtifactMetricUnsubscribeRequest.builder()
                            .addRemoveTimeFramedMetricTypes(TimeFramedMetricType.builder()
                                    .metricType(ResponseTime_Average).timeFrame(QueryTimeFrame.LAST_15_MINUTES).build())
                            .appUuid(application.appUuid())
                            .artifactQualifiedName("com.company.TestClass.testMethod()").build()

                    coreClient.unsubscribeFromArtifactMetrics(metricUnsubscribeRequest, {
                        if (it.succeeded()) {
                            coreClient.getSubscriberApplicationSubscriptions(application.appUuid(), {
                                if (it.failed()) {
                                    test.fail(it.cause())
                                }

                                def subscriptions = it.result()
                                test.assertEquals(1, subscriptions.size())
                                test.assertEquals(ArtifactMetricSubscribeRequest.class, subscriptions.get(0).class)

                                def subscription = subscriptions.get(0) as ArtifactMetricSubscribeRequest
                                test.assertEquals("com.company.TestClass.testMethod()", subscription.artifactQualifiedName())
                                test.assertEquals(SourceArtifactSubscriptionType.METRICS, subscription.type)
                                test.assertEquals(QueryTimeFrame.LAST_15_MINUTES, subscription.timeFrame())
                                test.assertEquals(1, subscription.metricTypes().size())
                                test.assertTrue(subscription.metricTypes().contains(Throughput_Average))
                                async.complete()
                            })
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
}
