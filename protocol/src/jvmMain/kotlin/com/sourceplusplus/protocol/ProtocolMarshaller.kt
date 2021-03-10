package com.sourceplusplus.protocol

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.guava.GuavaModule
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName
import com.sourceplusplus.protocol.artifact.log.LogCountSummary
import com.sourceplusplus.protocol.artifact.trace.TraceResult
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.core.json.jackson.DatabindCodec
import kotlinx.datetime.Instant

/**
 * todo: description.
 *
 * @since 0.2.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object ProtocolMarshaller {
    init {
        try {
//                val module = SimpleModule()
//                module.addSerializer(Instant::class.java, Serializers.KotlinInstantSerializer())
//                module.addDeserializer(Instant::class.java, Serializers.KotlinInstantDeserializer())
//                DatabindCodec.mapper().registerModule(module)

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
}
