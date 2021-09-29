package com.sourceplusplus.protocol.instrument.log

import com.sourceplusplus.protocol.instrument.InstrumentThrottle
import com.sourceplusplus.protocol.instrument.LiveInstrument
import com.sourceplusplus.protocol.instrument.LiveInstrumentType
import com.sourceplusplus.protocol.instrument.LiveSourceLocation
import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class LiveLog(
    val logFormat: String,
    val logArguments: List<String> = emptyList(),
    override val location: LiveSourceLocation,
    override val condition: String? = null,
    override val expiresAt: Long? = null,
    override val hitLimit: Int = 1,
    override val id: String? = null,
    override val applyImmediately: Boolean = false,
    override val applied: Boolean = false,
    override val pending: Boolean = false,
    override val throttle: InstrumentThrottle = InstrumentThrottle.DEFAULT
) : LiveInstrument() {
    override val type: LiveInstrumentType = LiveInstrumentType.LOG
}
