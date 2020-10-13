package com.sourceplusplus.protocol.artifact

import com.sourceplusplus.protocol.Serializers
import com.sourceplusplus.protocol.portal.QueryTimeFrame
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class ArtifactMetricResult(
    val appUuid: String,
    val artifactQualifiedName: String,
    val timeFrame: QueryTimeFrame,
    @Serializable(with = Serializers.InstantKSerializer::class)
    val start: Instant,
    @Serializable(with = Serializers.InstantKSerializer::class)
    val stop: Instant,
    val step: String,
    val artifactMetrics: List<ArtifactMetrics>
)
