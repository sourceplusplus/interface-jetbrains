package com.sourceplusplus.core.integration.apm.skywalking

import com.fasterxml.jackson.datatype.guava.GuavaModule
import com.google.common.base.Charsets
import com.google.common.io.Resources
import com.sourceplusplus.api.model.artifact.SourceArtifact
import com.sourceplusplus.api.model.artifact.SourceArtifactConfig
import com.sourceplusplus.api.model.trace.TraceSpanStackQuery
import io.vertx.core.json.JsonObject
import io.vertx.core.json.jackson.DatabindCodec
import org.junit.BeforeClass
import org.junit.Test

import static com.sourceplusplus.core.integration.apm.skywalking.SkywalkingIntegration.processTraceStack
import static org.junit.Assert.*

/**
 * @version 0.3.0
 * @since 0.3.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class SkywalkingIntegrationTest {

    private static final String INNER_ERROR = Resources.toString(Resources.getResource(
            "integration/apm/skywalking/traceStack/innerError.json"), Charsets.UTF_8)
    private static final String SKIP_ENTRY_COMPONENT = Resources.toString(Resources.getResource(
            "integration/apm/skywalking/traceStack/skipEntryComponent.json"), Charsets.UTF_8)
    private static final String MULTIPLE_SEGMENTS = Resources.toString(Resources.getResource(
            "integration/apm/skywalking/traceStack/multipleSegments.json"), Charsets.UTF_8)
    private static final String INNER_QUERY = Resources.toString(Resources.getResource(
            "integration/apm/skywalking/traceStack/innerQuery.json"), Charsets.UTF_8)

    @BeforeClass
    static void setup() {
        DatabindCodec.mapper().registerModule(new GuavaModule())
    }

    @Test
    void innerError() {
        def topLevelArtifact = SourceArtifact.builder()
                .artifactQualifiedName("com.company.JavaErrorExample.invokesMethod()").build()
        def spanQuery = TraceSpanStackQuery.builder()
                .traceId("8.1.15908316452720103")
                .oneLevelDeep(true).build()
        def processedTraceStack = processTraceStack(new JsonObject(INNER_ERROR), spanQuery, topLevelArtifact, [:])

        assertNotNull(processedTraceStack)
        assertEquals(3, processedTraceStack.size())
        assertEquals(0L, processedTraceStack.get(0).spanId())
        assertEquals(true, processedTraceStack.get(0).isChildError())
        assertEquals(1L, processedTraceStack.get(1).spanId())
        assertEquals(true, processedTraceStack.get(1).isChildError())
        assertEquals(3L, processedTraceStack.get(2).spanId())
        assertEquals(false, processedTraceStack.get(2).isChildError())
    }

    @Test
    void followSpan() {
        def topLevelArtifact = SourceArtifact.builder()
                .artifactQualifiedName("com.company.JavaErrorExample.invokesMethod()").build()
        def spanQuery = TraceSpanStackQuery.builder()
                .traceId("8.1.15908316452720103").spanId(1)
                .oneLevelDeep(true).build()
        def processedTraceStack = processTraceStack(new JsonObject(INNER_ERROR), spanQuery, topLevelArtifact, [:])

        assertNotNull(processedTraceStack)
        assertEquals(2, processedTraceStack.size())
        assertEquals(1L, processedTraceStack.get(0).spanId())
        assertEquals(false, processedTraceStack.get(0).isError())
        assertEquals(true, processedTraceStack.get(0).isChildError())
        assertEquals(2L, processedTraceStack.get(1).spanId())
        assertEquals(true, processedTraceStack.get(1).isError())
        assertEquals(false, processedTraceStack.get(1).isChildError())
    }

    @Test
    void skipEntryComponent() {
        def topLevelArtifact = SourceArtifact.builder()
                .artifactQualifiedName("com.sourceplusplus.core.api.artifact.ArtifactAPI.getApplicationSourceArtifactsRoute(io.vertx.ext.web.RoutingContext)").build()
        def spanQuery = TraceSpanStackQuery.builder()
                .traceId("11.36.15908373537845481")
                .oneLevelDeep(true).build()
        def processedTraceStack = processTraceStack(new JsonObject(SKIP_ENTRY_COMPONENT), spanQuery, topLevelArtifact, [:])

        assertNotNull(processedTraceStack)
        assertEquals(2, processedTraceStack.size())
        assertEquals(4L, processedTraceStack.get(0).spanId())
        assertEquals(false, processedTraceStack.get(0).isError())
        assertEquals(false, processedTraceStack.get(0).isChildError())
        assertEquals(5L, processedTraceStack.get(1).spanId())
        assertEquals(false, processedTraceStack.get(1).isError())
        assertEquals(false, processedTraceStack.get(1).isChildError())
    }

    @Test
    void multipleSegments() {
        def skywalkingEndpoints = ["/todos": "2"]
        def topLevelArtifact = SourceArtifact.builder()
                .artifactQualifiedName("io.vertx.blueprint.todolist.verticle.RxTodoVerticle.handleGetAll(io.vertx.ext.web.RoutingContext)")
                .config(SourceArtifactConfig.builder().endpointIds(["2"]).build()).build()
        def spanQuery = TraceSpanStackQuery.builder()
                .traceId("5.33.15909406654770173")
                .oneLevelDeep(true).build()
        def processedTraceStack = processTraceStack(new JsonObject(MULTIPLE_SEGMENTS), spanQuery, topLevelArtifact, skywalkingEndpoints)

        assertNotNull(processedTraceStack)
        assertEquals(2, processedTraceStack.size())
        assertEquals(2L, processedTraceStack.get(0).spanId())
        assertEquals("5.33.15909406654770174", processedTraceStack.get(0).segmentId())
        assertEquals(3L, processedTraceStack.get(1).spanId())
        assertEquals("5.33.15909406654770174", processedTraceStack.get(1).segmentId())
    }

    @Test
    void childStackTest() {
        def topLevelArtifact = SourceArtifact.builder()
                .artifactQualifiedName("com.company.JavaErrorExample.invokesMethod2()").build()
        def spanQuery = TraceSpanStackQuery.builder()
                .oneLevelDeep(true).followExit(true)
                .traceId("5.33.15909406654770173")
                .segmentId("599d2618abd543ac8005e686b0db21e1.1.15921256845400350")
                .spanId(0).build()
        def processedTraceStack = processTraceStack(new JsonObject(INNER_QUERY), spanQuery, topLevelArtifact, [:])
        assertNotNull(processedTraceStack)
        assertEquals(4, processedTraceStack.size())
        assertEquals(0, processedTraceStack.get(0).spanId())
        assertNull(processedTraceStack.get(0).hasChildStack())
        assertEquals(1, processedTraceStack.get(1).spanId())
        assertTrue(processedTraceStack.get(1).hasChildStack())
        assertEquals(4, processedTraceStack.get(2).spanId())
        assertNull(processedTraceStack.get(2).hasChildStack())
        assertEquals(5, processedTraceStack.get(3).spanId())
        assertNull(processedTraceStack.get(3).hasChildStack())

        topLevelArtifact = SourceArtifact.builder()
                .artifactQualifiedName("com.company.JavaErrorExample.innerInvokesMethod()").build()
        processedTraceStack = processTraceStack(new JsonObject(INNER_QUERY), spanQuery.withSpanId(1), topLevelArtifact, [:])
        assertNotNull(processedTraceStack)
        assertEquals(2, processedTraceStack.size())
        assertEquals(1, processedTraceStack.get(0).spanId())
        assertNull(processedTraceStack.get(0).hasChildStack())
        assertEquals(2, processedTraceStack.get(1).spanId())
        assertTrue(processedTraceStack.get(1).hasChildStack())

        topLevelArtifact = SourceArtifact.builder()
                .artifactQualifiedName("com.company.JavaErrorExample.innerInnerThrowsException()").build()
        processedTraceStack = processTraceStack(new JsonObject(INNER_QUERY), spanQuery.withSpanId(2), topLevelArtifact, [:])
        assertNotNull(processedTraceStack)
        assertEquals(2, processedTraceStack.size())
        assertEquals(2, processedTraceStack.get(0).spanId())
        assertNull(processedTraceStack.get(0).hasChildStack())
        assertEquals(3, processedTraceStack.get(1).spanId())
        assertNull(processedTraceStack.get(1).hasChildStack())
    }

    //todo: hasChildStack multi-segment
    //todo: followExit test
}
