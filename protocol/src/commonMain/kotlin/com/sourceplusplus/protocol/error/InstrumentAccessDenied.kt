package com.sourceplusplus.protocol.error

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class InstrumentAccessDenied : RuntimeException {

    private val instrumentLocation: String

    constructor(instrumentLocation: String) : this(instrumentLocation, "Instrument access denied: $instrumentLocation")

    private constructor(instrumentLocation: String, message: String) : super(message) {
        this.instrumentLocation = instrumentLocation
    }

    fun toEventBusException(): InstrumentAccessDenied {
        return InstrumentAccessDenied(
            instrumentLocation, "EventBusException:InstrumentAccessDenied[$instrumentLocation]"
        )
    }
}
