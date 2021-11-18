package spp.jetbrains.marker.source.mark.api.event

import spp.jetbrains.marker.source.mark.api.SourceMark

/**
 * Used to listen for events produced by [SourceMark]s.
 * Calls handleEvent synchronously.
 *
 * @since 0.2.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
fun interface SynchronousSourceMarkEventListener : SourceMarkEventListener {
    override fun handleEvent(event: SourceMarkEvent)
}
