package com.sourceplusplus.protocol

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.guava.GuavaModule
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName
import com.sourceplusplus.protocol.artifact.debugger.event.BreakpointHit
import com.sourceplusplus.protocol.artifact.debugger.HindsightBreakpoint
import com.sourceplusplus.protocol.artifact.debugger.SourceLocation
import com.sourceplusplus.protocol.artifact.log.LogCountSummary
import com.sourceplusplus.protocol.artifact.trace.TraceResult
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.core.json.jackson.DatabindCodec
import kotlinx.datetime.Instant
import java.util.*

/**
 * todo: description.
 *
 * @since 0.2.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object ProtocolMarshaller {
    init {
        try {
            //todo: verify if this init is needed
            DatabindCodec.mapper().registerModule(GuavaModule())
            DatabindCodec.mapper().registerModule(Jdk8Module())
            DatabindCodec.mapper().registerModule(JavaTimeModule())
            DatabindCodec.mapper().registerModule(KotlinModule())
            DatabindCodec.mapper().enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
            DatabindCodec.mapper().enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
        } catch (ignore: Throwable) {
        }
    }

    @JvmStatic
    fun serializeArtifactQualifiedName(value: ArtifactQualifiedName): JsonObject {
        return JsonObject(Json.encode(value))
    }

    @JvmStatic
    fun deserializeArtifactQualifiedName(value: JsonObject): ArtifactQualifiedName {
        return value.mapTo(ArtifactQualifiedName::class.java)
    }

    @JvmStatic
    fun serializeTraceResult(value: TraceResult): JsonObject {
        return JsonObject(Json.encode(value))
    }

    @JvmStatic
    fun deserializeTraceResult(value: JsonObject): TraceResult {
        return value.mapTo(TraceResult::class.java)
    }

    @JvmStatic
    fun serializeInstant(value: Instant): String {
        return value.toEpochMilliseconds().toString()
    }

    @JvmStatic
    fun deserializeInstant(value: String): Instant {
        return Instant.fromEpochMilliseconds(value.toLong())
    }

    @JvmStatic
    fun serializeLogCountSummary(value: LogCountSummary): JsonObject {
        return JsonObject(Json.encode(value))
    }

    @JvmStatic
    fun deserializeLogCountSummary(value: JsonObject): LogCountSummary {
        return value.mapTo(LogCountSummary::class.java)
    }

    @JvmStatic
    fun serializeHindsightBreakpoint(value: HindsightBreakpoint): JsonObject {
        return JsonObject(Json.encode(value))
    }

    @JvmStatic
    fun deserializeHindsightBreakpoint(value: JsonObject): HindsightBreakpoint {
        return value.mapTo(HindsightBreakpoint::class.java)
    }

    @JvmStatic
    fun serializeSourceLocation(value: SourceLocation): JsonObject {
        return JsonObject(Json.encode(value))
    }

    @JvmStatic
    fun deserializeSourceLocation(value: JsonObject): SourceLocation {
        return value.mapTo(SourceLocation::class.java)
    }

    @JvmStatic
    fun setupCodecs(vertx: Vertx) {
        vertx.eventBus().registerDefaultCodec(BreakpointHit::class.java, ProtocolMessageCodec())
    }

    class ProtocolMessageCodec<T> : MessageCodec<T, T> {
        override fun encodeToWire(buffer: Buffer?, s: T?) {
            val value = Json.encode(s).toByteArray()
            buffer?.appendInt(value.size)
            buffer?.appendBytes(value)
        }

        override fun decodeFromWire(pos: Int, buffer: Buffer?): T? {
            val len = buffer?.getInt(pos) ?: return null
            val bytes = buffer.getBytes(pos + 4, pos + 4 + len)
            return cast(Json.decodeValue(String(bytes)))
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T> cast(obj: Any?): T? {
            return obj as? T
        }

        override fun transform(o: T): T = o
        override fun name(): String = UUID.randomUUID().toString()
        override fun systemCodecID(): Byte = -1
    }
}
