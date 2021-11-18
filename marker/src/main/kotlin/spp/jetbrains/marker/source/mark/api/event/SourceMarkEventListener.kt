package spp.jetbrains.marker.source.mark.api.event

import spp.jetbrains.marker.source.mark.api.SourceMark

/**
 * Used to listen for events produced by [SourceMark]s.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
fun interface SourceMarkEventListener {
    fun handleEvent(event: SourceMarkEvent)
}
