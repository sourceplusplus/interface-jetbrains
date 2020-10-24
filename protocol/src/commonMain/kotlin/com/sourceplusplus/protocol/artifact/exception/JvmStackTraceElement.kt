package com.sourceplusplus.protocol.artifact.exception

import com.sourceplusplus.protocol.ArtifactNameUtils.getShortQualifiedClassName

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
    val sourceAsFilename: String?
        get() = if (source.contains(":")) {
            source.substring(0, source.indexOf(":"))
        } else {
            null
        }
    val sourceAsLineNumber: Int?
        get() = if (source.contains(":")) {
            source.substring(source.indexOf(":") + 1).toInt()
        } else {
            null
        }

    override fun toString(): String = toString(false)

    fun toString(shorten: Boolean): String {
        return if (shorten) {
            val shortName = getShortQualifiedClassName(method.substring(0, method.lastIndexOf("."))) +
                    method.substring(method.lastIndexOf("."))
            val lineNumber = sourceAsLineNumber
            if (lineNumber != null) {
                "at $shortName() line: $lineNumber"
            } else {
                "at $shortName($source)"
            }
        } else {
            "at $method($source)"
        }
    }
}
