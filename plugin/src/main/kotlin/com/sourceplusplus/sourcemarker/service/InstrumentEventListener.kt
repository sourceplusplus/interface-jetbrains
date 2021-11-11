package com.sourceplusplus.sourcemarker.service

import spp.protocol.instrument.LiveInstrumentEvent

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
fun interface InstrumentEventListener {
    fun accept(event: LiveInstrumentEvent)
}
