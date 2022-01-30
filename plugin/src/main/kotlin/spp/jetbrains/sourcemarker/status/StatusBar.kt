package spp.jetbrains.sourcemarker.status

import spp.protocol.instrument.LiveInstrument

/**
 * todo: description.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface StatusBar {

    fun setLiveInstrument(liveInstrument: LiveInstrument)
}
