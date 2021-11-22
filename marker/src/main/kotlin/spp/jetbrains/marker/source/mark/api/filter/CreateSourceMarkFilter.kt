package spp.jetbrains.marker.source.mark.api.filter

import spp.jetbrains.marker.source.mark.api.SourceMark
import java.util.function.Predicate

/**
 * Used to filter auto-created [SourceMark]s.
 *
 * @since 0.1.0
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