package com.sourceplusplus.core.api.application

import com.sourceplusplus.api.model.application.SourceApplication
import com.sourceplusplus.core.api.SourceCoreAPITest
import io.vertx.ext.unit.Async
import io.vertx.ext.unit.TestSuite
import org.junit.Test

import java.util.concurrent.atomic.AtomicBoolean

import static org.junit.Assert.assertTrue

/**
 * @version 0.2.1
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class ApplicationAPITest extends SourceCoreAPITest {

    @Test
    void createApplicationTest() {
        TestSuite.create("ApplicationAPITest-createApplicationTest").test("createApplicationTest", { test ->
            def createRequest = SourceApplication.builder().isCreateRequest(true)
                    .appName("test-name").build()

            Async async = test.async()
            coreClient.createApplication(createRequest, {
                if (it.failed()) {
                    test.fail(it.cause())
                }
                test.assertTrue(it.result().appName() == createRequest.appName())
                async.complete()
            })
        }).run().awaitSuccess()
    }

    @Test
    void findApplicationByNameTest() {
        def testPassed = new AtomicBoolean(false)
        def createdAppUuid = ""
        def createdAppName = ""

        TestSuite.create("ApplicationAPITest-findApplicationByNameTest").before({ test ->
            //create application to find in test
            Async async = test.async()
            coreClient.createApplication({
                if (it.failed()) {
                    test.fail(it.cause())
                }
                createdAppUuid = it.result().appUuid()
                createdAppName = it.result().appName()
                async.complete()
            })
        }).test("findApplicationByName", { test ->
            Async async = test.async()
            coreClient.findApplicationByName(createdAppName, {
                if (it.failed()) {
                    test.fail(it.cause())
                }
                if (it.result().get().appUuid() == createdAppUuid) {
                    testPassed.set(true)
                }
                async.complete()
            })
        }).run().await()

        //final asserts
        assertTrue(testPassed.get())
    }

    @Test
    void getApplicationTest() {
        def testPassed = new AtomicBoolean(false)
        def createdAppUuid = ""

        TestSuite.create("ApplicationAPITest-getApplicationTest").before({ test ->
            //create application to retrieve in test
            Async async = test.async()
            coreClient.createApplication({
                if (it.failed()) {
                    test.fail(it.cause())
                }
                createdAppUuid = it.result().appUuid()
                async.complete()
            })
        }).test("getApplication", { test ->
            Async async = test.async()
            coreClient.getApplication(createdAppUuid, {
                if (it.failed()) {
                    test.fail(it.cause())
                }
                if (it.result().get().appUuid() == createdAppUuid) {
                    testPassed.set(true)
                }
                async.complete()
            })
        }).run().await()

        //final asserts
        assertTrue(testPassed.get())
    }

    @Test
    void getApplicationTest_Negative() {
        TestSuite.create("ApplicationAPITest-getApplicationTest_Negative").test("getApplication", { test ->
            Async async = test.async()
            coreClient.getApplication("INVALID-UUID", {
                test.assertTrue(it.succeeded())
                test.assertFalse(it.result().isPresent())
                async.complete()
            })
        }).run().awaitSuccess()
    }
}