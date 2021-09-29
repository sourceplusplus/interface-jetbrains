package com.sourceplusplus.protocol.instrument

import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class LiveInstrumentBatch(
    val instruments: List<LiveInstrument>
)
