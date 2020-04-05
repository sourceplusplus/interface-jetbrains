package com.sourceplusplus.core.api.trace

import com.sourceplusplus.api.model.application.SourceApplication
import com.sourceplusplus.api.model.trace.ArtifactTraceSubscribeRequest
import com.sourceplusplus.api.model.trace.TraceOrderType
import com.sourceplusplus.api.model.trace.TraceQuery
import com.sourceplusplus.core.api.SourceCoreAPITest
import io.vertx.ext.unit.TestSuite
import org.junit.Test

import java.time.Instant
import java.time.temporal.ChronoUnit

class TraceAPITest extends SourceCoreAPITest {

    @Test
    void testSomething() {
        SourceApplication application
        TestSuite.create("subscribe_to_artifact_traces-setup").before({ test ->
            def async = test.async(1)
            coreClient.getApplication("99999999-9999-9999-9999-999999999999", {
                if (it.succeeded()) {
                    if (it.result().present) {
                        application = it.result().get()
                        async.countDown()
                    } else {
                        coreClient.createApplication(SourceApplication.builder().isCreateRequest(true)
                                .appUuid("99999999-9999-9999-9999-999999999999").build(), {
                            if (it.failed()) {
                                test.fail(it.cause())
                            }

                            application = it.result()
                            async.countDown()
                        })
                    }
                } else {
                    test.fail(it.cause())
                }
            })
        }).test("subscribe_to_artifact_traces", { test ->
            def async = test.async()
            def traceSubscribeRequest = ArtifactTraceSubscribeRequest.builder()
                    .addOrderTypes(TraceOrderType.LATEST_TRACES)
                    .appUuid(application.appUuid())
                    .artifactQualifiedName("com.sourceplusplus.core.api.trace.TesterClass.staticMethod()").build()
            coreClient.subscribeToArtifact(traceSubscribeRequest, {
                if (it.failed()) {
                    test.fail(it.cause())
                }

                for (int i = 0; i < 40; i++) {
                    TesterClass.staticMethod()
                }

                def traceQuery = TraceQuery.builder()
                        .appUuid("99999999-9999-9999-9999-999999999999")
                        .artifactQualifiedName("com.sourceplusplus.core.api.trace.TesterClass.staticMethod()")
                        .orderType(TraceOrderType.LATEST_TRACES)
                        .pageSize(10)
                        .durationStart(Instant.now().minus(14, ChronoUnit.MINUTES))
                        .durationStop(Instant.now())
                        .durationStep("SECOND").build()
                coreClient.getTraces(traceQuery, {
                    if (it.succeeded()) {
                        test.assertEquals(10, it.result().traces().size())
                        //todo: verify trace structure and stuff
                        //todo: lookup spans and stuff
                        async.countDown()
                    } else {
                        test.fail(it.cause())
                    }
                })
            })
        }).run().awaitSuccess()
    }
}
