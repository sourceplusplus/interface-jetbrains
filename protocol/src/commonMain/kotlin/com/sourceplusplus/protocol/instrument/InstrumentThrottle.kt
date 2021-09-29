package com.sourceplusplus.protocol.instrument

import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class InstrumentThrottle(
    val limit: Int,
    val step: ThrottleStep,
) {
    companion object {
        val DEFAULT: InstrumentThrottle = InstrumentThrottle(1, ThrottleStep.SECOND)
    }
}
