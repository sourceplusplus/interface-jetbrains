package com.sourceplusplus.mentor

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
fun interface MentorJobListener {
    fun onEvent(event: MentorJobEvent, data: Any?)
}
