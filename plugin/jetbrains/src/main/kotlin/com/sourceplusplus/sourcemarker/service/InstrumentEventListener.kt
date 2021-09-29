package com.sourceplusplus.sourcemarker.service

import com.sourceplusplus.protocol.instrument.LiveInstrumentEvent

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
fun interface InstrumentEventListener {
    fun accept(event: LiveInstrumentEvent)
}
