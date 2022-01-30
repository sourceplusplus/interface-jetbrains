package spp.jetbrains.marker.source.mark.api.filter

import spp.jetbrains.marker.source.mark.api.SourceMark
import java.util.function.Predicate

/**
 * Used to filter auto-applied [SourceMark]s.
 *
 * @since 0.1.0
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
