package com.sourceplusplus.protocol.artifact.log

import com.sourceplusplus.protocol.Serializers
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.2.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class LogCountSummary(
    @Serializable(with = Serializers.InstantKSerializer::class)
    val timestamp: Instant,
    val logCounts: Map<String, Int>
)
