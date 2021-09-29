package com.sourceplusplus.protocol.instrument.breakpoint.event

import kotlinx.datetime.Instant

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface TrackedLiveEvent {
    val occurredAt: Instant
}
