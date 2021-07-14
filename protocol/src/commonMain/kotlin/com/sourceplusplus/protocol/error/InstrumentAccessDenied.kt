package com.sourceplusplus.protocol.error

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
