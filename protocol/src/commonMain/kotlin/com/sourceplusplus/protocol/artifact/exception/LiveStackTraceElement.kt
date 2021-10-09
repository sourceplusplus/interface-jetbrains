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
data class LiveStackTraceElement(
    val method: String,
    val source: String,
    val variables: MutableList<LiveVariable> = mutableListOf(),
    var sourceCode: String? = null
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

fun LiveStackTraceElement.sourceAsFilename(): String? {
    return if (source.contains(":")) {
        source.substring(0, source.indexOf(":"))
    } else {
        null
    }
}

fun LiveStackTraceElement.sourceAsLineNumber(): Int? {
    return if (source.contains(":")) {
        source.substring(source.indexOf(":") + 1).toInt()
    } else {
        null
    }
}

fun LiveStackTraceElement.qualifiedClassName(): String {
    return method.substring(0, method.lastIndexOf("."))
}

fun LiveStackTraceElement.shortQualifiedClassName(): String {
    return getShortQualifiedClassName(qualifiedClassName())
}

fun LiveStackTraceElement.methodName(): String {
    return if (method.contains("$")) {
        method.substring(method.lastIndexOf(".") + 1).substringBefore("$")
    } else {
        method.substring(method.lastIndexOf(".") + 1)
    }
}
