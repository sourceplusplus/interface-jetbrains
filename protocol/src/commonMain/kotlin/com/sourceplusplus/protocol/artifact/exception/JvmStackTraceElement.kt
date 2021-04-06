package com.sourceplusplus.protocol.artifact.exception

import com.sourceplusplus.protocol.artifact.debugger.TraceVariable
import com.sourceplusplus.protocol.utils.ArtifactNameUtils.getShortQualifiedClassName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
@Suppress("TRANSIENT_IS_REDUNDANT")
data class JvmStackTraceElement(
    val method: String,
    val source: String,
    val variables: MutableList<TraceVariable> = mutableListOf()
) {
    @Transient
    val sourceAsFilename: String?
        get() = if (source.contains(":")) {
            source.substring(0, source.indexOf(":"))
        } else {
            null
        }

    @Transient
    val sourceAsLineNumber: Int?
        get() = if (source.contains(":")) {
            source.substring(source.indexOf(":") + 1).toInt()
        } else {
            null
        }

    @Transient
    val qualifiedClassName: String by lazy {
        method.substring(0, method.lastIndexOf("."))
    }

    @Transient
    val shortQualifiedClassName: String by lazy {
        getShortQualifiedClassName(qualifiedClassName)
    }

    @Transient
    val methodName: String by lazy {
        method.substring(method.lastIndexOf(".") + 1)
    }

    override fun toString(): String = toString(false)

    fun toString(shorten: Boolean): String {
        return if (shorten) {
            val shortName = "$shortQualifiedClassName.$methodName"
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
