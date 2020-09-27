package com.sourceplusplus.marker.source.mark.api.event

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
fun interface SourceMarkEventListener {
    fun handleEvent(event: SourceMarkEvent)
}
