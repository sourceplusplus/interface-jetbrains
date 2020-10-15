package com.sourceplusplus.protocol.artifact.endpoint

import com.sourceplusplus.protocol.Serializers
import com.sourceplusplus.protocol.artifact.ArtifactSummarizedResult
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
data class EndpointResult(
    val appUuid: String,
    val timeFrame: QueryTimeFrame,
    @Serializable(with = Serializers.InstantKSerializer::class)
    val start: Instant,
    @Serializable(with = Serializers.InstantKSerializer::class)
    val stop: Instant,
    val step: String,
    val endpointMetrics: List<ArtifactSummarizedResult>
)
