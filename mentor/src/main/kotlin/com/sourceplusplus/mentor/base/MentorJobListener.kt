package com.sourceplusplus.mentor.base

/**
 * Listen for [MentorJobEvent]s produced by processing [MentorJob]s.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
fun interface MentorJobListener {
    fun onEvent(event: MentorJobEvent, data: Any?)
}
