package com.sourceplusplus.protocol.artifact.exception

import com.sourceplusplus.protocol.instrument.LiveVariable
import com.sourceplusplus.protocol.utils.ArtifactNameUtils.getShortQualifiedClassName
import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class JvmStackTraceElement(
    val method: String,
    val source: String,
    val variables: MutableList<LiveVariable> = mutableListOf()
) {
    override fun toString(): String = toString(false)

    fun toString(shorten: Boolean): String {
        return if (shorten) {
            val shortName = "${shortQualifiedClassName()}.${methodName()}"
            val lineNumber = sourceAsLineNumber()
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

fun JvmStackTraceElement.sourceAsFilename(): String? {
    return if (source.contains(":")) {
        source.substring(0, source.indexOf(":"))
    } else {
        null
    }
}

fun JvmStackTraceElement.sourceAsLineNumber(): Int? {
    return if (source.contains(":")) {
        source.substring(source.indexOf(":") + 1).toInt()
    } else {
        null
    }
}

fun JvmStackTraceElement.qualifiedClassName(): String {
    return method.substring(0, method.lastIndexOf("."))
}

fun JvmStackTraceElement.shortQualifiedClassName(): String {
    return getShortQualifiedClassName(qualifiedClassName())
}

fun JvmStackTraceElement.methodName(): String {
    return if (method.contains("$")) {
        method.substring(method.lastIndexOf(".") + 1).substringBefore("$")
    } else {
        method.substring(method.lastIndexOf(".") + 1)
    }
}
