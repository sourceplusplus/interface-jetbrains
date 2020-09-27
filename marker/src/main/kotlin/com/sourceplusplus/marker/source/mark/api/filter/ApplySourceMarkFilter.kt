package com.sourceplusplus.marker.source.mark.api.filter

import com.sourceplusplus.marker.source.mark.api.SourceMark
import java.util.function.Predicate

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
fun interface ApplySourceMarkFilter : Predicate<SourceMark> {
    companion object {
        @JvmStatic
        val ALL: ApplySourceMarkFilter = ApplySourceMarkFilter { true }

        @JvmStatic
        val NONE: ApplySourceMarkFilter = ApplySourceMarkFilter { false }
    }
}
