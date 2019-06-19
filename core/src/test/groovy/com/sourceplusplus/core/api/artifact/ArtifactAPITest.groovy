package com.sourceplusplus.core.api.artifact

import com.sourceplusplus.api.model.application.SourceApplication
import com.sourceplusplus.api.model.artifact.SourceArtifact
import com.sourceplusplus.api.model.artifact.SourceArtifactConfig
import com.sourceplusplus.core.api.SourceCoreAPITest
import io.vertx.core.json.Json
import io.vertx.ext.unit.TestSuite
import org.junit.Test

/**
 * @version 0.2.1
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class ArtifactAPITest extends SourceCoreAPITest {

    @Test
    void createArtifactTest() {
        SourceApplication application = null

        TestSuite.create("ArtifactAPITest-createArtifactTest").before({ test ->
            //create application to use in test
            def async = test.async()
            coreClient.createApplication({
                if (it.failed()) {
                    test.fail(it.cause())
                }
                application = it.result()
                async.complete()
            })
        }).test("createArtifactTest", { test ->
            def async = test.async()
            def artifact = SourceArtifact.builder()
                    .artifactQualifiedName("test-name").build()
            coreClient.createArtifact(application.appUuid(), artifact, {
                if (it.failed()) {
                    test.fail(it.cause())
                }

                def createdArtifact = it.result()
                test.assertNotNull(createdArtifact)
                test.assertNotNull(createdArtifact.createDate())
                test.assertNotNull(createdArtifact.lastUpdated())
                test.assertTrue(createdArtifact.artifactQualifiedName() == "test-name")
                async.complete()
            })
        }).run().awaitSuccess()
    }

    @Test
    void getArtifactTest() {
        SourceApplication application = null
        SourceArtifact artifact = null

        TestSuite.create("ArtifactAPITest-getArtifactTest").before({ test ->
            //create application to use in test
            def async = test.async(2)
            coreClient.createApplication({
                if (it.failed()) {
                    test.fail(it.cause())
                }
                application = it.result()
                async.countDown()

                //create artifact to retrieve in test
                def createArtifact = SourceArtifact.builder()
                        .artifactQualifiedName("test-name").build()
                coreClient.createArtifact(application.appUuid(), createArtifact, {
                    if (it.failed()) {
                        test.fail(it.cause())
                    }
                    artifact = it.result()
                    async.countDown()
                })
            })
        }).test("getArtifactTest", { test ->
            def async = test.async()
            coreClient.getArtifact(application.appUuid(), artifact.artifactQualifiedName(), {
                if (it.failed()) {
                    test.fail(it.cause())
                }

                def fetchedArtifact = it.result()
                test.assertNotNull(fetchedArtifact)
                test.assertEquals(Json.encode(artifact), Json.encode(fetchedArtifact))
                async.complete()
            })
        }).run().awaitSuccess()
    }

    @Test
    void createArtifactConfigTest() {
        SourceApplication application = null

        TestSuite.create("ArtifactAPITest-createArtifactConfigTest").before({ test ->
            //create application to use in test
            def async = test.async()
            coreClient.createApplication({
                if (it.failed()) {
                    test.fail(it.cause())
                }
                application = it.result()
                async.complete()
            })
        }).test("createArtifactConfigTest", { test ->
            def async = test.async()
            def artifactConfig = SourceArtifactConfig.builder().endpoint(true)
                    .endpointName("test-endpoint-name").build()

            coreClient.createOrUpdateArtifactConfig(application.appUuid(), "test-name", artifactConfig, {
                if (it.failed()) {
                    test.fail(it.cause())
                }

                def createdArtifactConfig = it.result()
                test.assertNotNull(createdArtifactConfig)
                test.assertTrue(createdArtifactConfig.endpoint())
                test.assertNotNull(createdArtifactConfig.endpointName())
                test.assertTrue(createdArtifactConfig.endpointName() == "test-endpoint-name")
                async.complete()
            })
        }).run().awaitSuccess()
    }

    @Test
    void getArtifactConfigTest() {
        SourceApplication application = null
        SourceArtifactConfig artifactConfig = null

        TestSuite.create("ArtifactAPITest-getArtifactConfigTest").before({ test ->
            //create application to use in test
            def async = test.async(2)
            coreClient.createApplication({
                if (it.failed()) {
                    test.fail(it.cause())
                }
                application = it.result()
                async.countDown()

                //create artifact config to retrieve in test
                def createArtifactConfig = SourceArtifactConfig.builder()
                        .endpointName("test-endpoint-name").build()
                coreClient.createOrUpdateArtifactConfig(application.appUuid(), "test-name", createArtifactConfig, {
                    if (it.failed()) {
                        test.fail(it.cause())
                    }
                    artifactConfig = it.result()
                    async.countDown()
                })
            })
        }).test("getArtifactConfigTest", { test ->
            def async = test.async()
            coreClient.getArtifactConfig(application.appUuid(), "test-name", {
                if (it.failed()) {
                    test.fail(it.cause())
                }

                def fetchedArtifactConfig = it.result()
                test.assertNotNull(fetchedArtifactConfig)
                test.assertEquals(Json.encode(artifactConfig), Json.encode(fetchedArtifactConfig))
                async.complete()
            })
        }).run().awaitSuccess()
    }
}