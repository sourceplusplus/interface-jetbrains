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
)
