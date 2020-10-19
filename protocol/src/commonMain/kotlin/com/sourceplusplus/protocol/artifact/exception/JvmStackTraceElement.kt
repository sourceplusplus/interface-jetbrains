package com.sourceplusplus.protocol.artifact.exception

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
data class JvmStackTraceElement(
    val method: String,
    val source: String
) {
    val sourceAsLineNumber: Int?
        get() = if (source.contains(":")) {
            source.substring(source.indexOf(":") + 1).toInt()
        } else {
            null
        }

    override fun toString(): String = "at $method($source)"
}