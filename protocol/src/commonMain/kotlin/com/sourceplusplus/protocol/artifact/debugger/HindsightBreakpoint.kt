package com.sourceplusplus.protocol.artifact.debugger

import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class HindsightBreakpoint(
    val location: SourceLocation? = null,
    val condition: String? = null,
    val expiresAt: Long? = null,
    val hitLimit: Int = 1,
    val id: String? = null
)
