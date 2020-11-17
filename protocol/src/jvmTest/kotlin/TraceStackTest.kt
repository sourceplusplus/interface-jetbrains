package com.sourceplusplus.protocol.artifact.trace

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.guava.GuavaModule
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.common.io.Resources
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.jackson.DatabindCodec
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TraceStackTest {

    @Before
    fun setUp() {
        val module = SimpleModule()
        module.addSerializer(Instant::class.java, KSerializers.KotlinInstantSerializer())
        module.addDeserializer(Instant::class.java, KSerializers.KotlinInstantDeserializer())
        DatabindCodec.mapper().registerModule(module)

        DatabindCodec.mapper().registerModule(GuavaModule())
        DatabindCodec.mapper().registerModule(Jdk8Module())
        DatabindCodec.mapper().registerModule(JavaTimeModule())
        DatabindCodec.mapper().registerModule(KotlinModule())
        DatabindCodec.mapper().enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
        DatabindCodec.mapper().enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
    }

    @Test
    fun `dual segment trace stack`() {
        val jsonTraceStack = JsonArray(
            Resources.toString(Resources.getResource("dualSegmentTraceStack.json"), Charsets.UTF_8)
        )
        val traceSpans = mutableListOf<TraceSpan>()
        for (i in 0 until jsonTraceStack.size()) {
            traceSpans.add(Json.decodeValue(jsonTraceStack.getJsonObject(i).toString(), TraceSpan::class.java))
        }
        val traceStack = TraceStack(traceSpans)
        assertEquals(2, traceStack.segments)

        val exitSegment = traceStack.getSegment("4dc611c1901e4f9db1c6cc3a8d1bed45.73.16054873308541224")
        assertEquals(1, exitSegment.size)
        assertEquals(1, exitSegment.depth)
        assertEquals(0, exitSegment.getChildren(0).size)
        assertNull(exitSegment.getParent(0))

        val entrySegment = traceStack.getSegment("4dc611c1901e4f9db1c6cc3a8d1bed45.59.16054873308541420")
        assertEquals(5, entrySegment.size)
        assertEquals(3, entrySegment.depth)
        assertEquals(1, entrySegment.getChildren(0).size)
        assertEquals(3, entrySegment.getChildren(1).size)
        assertEquals(0, entrySegment.getChildren(2).size)
        assertEquals(0, entrySegment.getChildren(3).size)
        assertEquals(0, entrySegment.getChildren(4).size)
        assertNull(entrySegment.getParent(0))
        assertEquals(0, entrySegment.getParent(1)!!.spanId)
        assertEquals(1, entrySegment.getParent(2)!!.spanId)
        assertEquals(1, entrySegment.getParent(3)!!.spanId)
        assertEquals(1, entrySegment.getParent(4)!!.spanId)
    }

    private class KSerializers {
        class KotlinInstantSerializer : JsonSerializer<Instant>() {
            override fun serialize(value: Instant, jgen: JsonGenerator, provider: SerializerProvider) =
                jgen.writeNumber(value.toEpochMilliseconds())
        }

        class KotlinInstantDeserializer : JsonDeserializer<Instant>() {
            override fun deserialize(p: JsonParser, p1: DeserializationContext): Instant =
                Instant.fromEpochMilliseconds((p.codec.readTree(p) as JsonNode).longValue())
        }
    }
}
