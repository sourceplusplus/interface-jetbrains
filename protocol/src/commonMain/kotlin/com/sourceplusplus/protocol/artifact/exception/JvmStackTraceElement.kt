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

    fun toString(shortenName: Boolean): String {
        return if (shortenName) {
            val shortName = getShortQualifiedClassName(method.substring(0, method.lastIndexOf("."))) +
                    method.substring(method.lastIndexOf("."))
            "at $shortName($source)"
        } else {
            "at $method($source)"
        }
    }
}