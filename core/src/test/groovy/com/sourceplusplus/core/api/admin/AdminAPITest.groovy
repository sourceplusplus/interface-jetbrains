package com.sourceplusplus.core.api.admin

import com.sourceplusplus.core.api.SourceCoreAPITest
import io.vertx.ext.unit.TestSuite
import org.junit.Test

class AdminAPITest extends SourceCoreAPITest {

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
}