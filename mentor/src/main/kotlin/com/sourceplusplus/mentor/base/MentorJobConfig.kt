package com.sourceplusplus.mentor.base

/**
 * [MentorJob] processing configuration.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
data class MentorJobConfig(
    val repeatForever: Boolean = false,
    val repeatDelay: Long = 0
)