package com.sourceplusplus.marker.source.mark.api.filter

import java.util.function.Predicate

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
fun interface CreateSourceMarkFilter : Predicate<String> {
    companion object {
        @JvmStatic
        val ALL: CreateSourceMarkFilter = CreateSourceMarkFilter { true }

        @JvmStatic
        val NONE: CreateSourceMarkFilter = CreateSourceMarkFilter { false }
    }
}
