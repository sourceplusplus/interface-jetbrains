package com.sourceplusplus.mentor.base

/**
 * Used by [MentorTaskContext] for storage and type casting.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("unused")
class ContextKey<T>(private val name: String) {
    override fun toString(): String = name
}
