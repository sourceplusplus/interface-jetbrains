package com.sourceplusplus.mentor.base

/**
 * Represents events [MentorJob]s may emit.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
enum class MentorJobEvent {
    TASK_COMPLETE,
    JOB_COMPLETE,
    JOB_RESCHEDULED,
    CONTEXT_SHARED,
    CONTEXT_REUSED
}
