package com.sourceplusplus.protocol.instrument

import kotlinx.datetime.Instant

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface TrackedLiveEvent {
    val occurredAt: Instant
}
