package com.sourceplusplus.protocol.artifact.debugger

import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class SourceLocation(
    val source: String,
    val line: Int,
    val commitId: String? = null,
    val fileChecksum: String? = null
) : Comparable<SourceLocation> {

    override fun compareTo(other: SourceLocation): Int {
        val sourceCompare = source.compareTo(other.source)
        if (sourceCompare != 0) return sourceCompare
        return line.compareTo(other.line)
    }
}
