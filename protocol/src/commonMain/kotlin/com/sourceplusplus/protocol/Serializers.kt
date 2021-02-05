package com.sourceplusplus.protocol

import com.sourceplusplus.protocol.artifact.ArtifactType
import com.sourceplusplus.protocol.portal.PageType
import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Used to serialize/deserialize protocol messages.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class Serializers {

    /**
     * Used to serialize/deserialize [Instant] classes.
     *
     * @since 0.1.0
     */
    class InstantKSerializer : KSerializer<Instant> {

        override val descriptor = PrimitiveSerialDescriptor(
            "com.sourceplusplus.protocol.InstantKSerializer",
            PrimitiveKind.LONG
        )

        override fun deserialize(decoder: Decoder) = Instant.fromEpochMilliseconds(decoder.decodeLong())
        override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeLong(value.toEpochMilliseconds())
    }

    /**
     * Used to serialize/deserialize [ArtifactType] classes.
     *
     * @since 0.1.0
     */
    class ArtifactTypeSerializer : KSerializer<ArtifactType> {

        override val descriptor = PrimitiveSerialDescriptor(
            "com.sourceplusplus.protocol.ArtifactTypeSerializer",
            PrimitiveKind.STRING
        )

        override fun deserialize(decoder: Decoder) = ArtifactType.valueOf(decoder.decodeString())
        override fun serialize(encoder: Encoder, value: ArtifactType) = encoder.encodeString(value.name)
    }

    /**
     * Used to serialize/deserialize [PageType] classes.
     *
     * @since 0.2.0
     */
    class PageTypeSerializer : KSerializer<PageType> {

        override val descriptor = PrimitiveSerialDescriptor(
            "com.sourceplusplus.protocol.PageTypeSerializer",
            PrimitiveKind.STRING
        )

        override fun deserialize(decoder: Decoder) = PageType.valueOf(decoder.decodeString())
        override fun serialize(encoder: Encoder, value: PageType) = encoder.encodeString(value.name)
    }
}
