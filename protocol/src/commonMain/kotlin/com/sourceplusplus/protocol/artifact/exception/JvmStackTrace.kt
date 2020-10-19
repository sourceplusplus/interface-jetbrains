package com.sourceplusplus.protocol.artifact.exception

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class JvmStackTrace(
    var exceptionType: String,
    var message: String?,
    val elements: MutableList<JvmStackTraceElement>,
    val causedBy: JvmStackTrace? = null
) {

    override fun toString(): String {
        val builder = StringBuilder()
        builder.append(exceptionType)
        if (message != null) {
            builder.append(": ")
            builder.append(message)
        }
        for (element in elements) {
            builder.append("\n\t ")
            builder.append(element.toString())
        }
        if (causedBy != null) {
            builder.append("\nCaused by: ")
            builder.append(causedBy.toString())
        }
        return builder.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is JvmStackTrace) return false
        if (exceptionType != other.exceptionType) return false
        if (message != other.message) return false
        if (elements != other.elements) return false
        if (causedBy != other.causedBy) return false
        return true
    }

    override fun hashCode(): Int {
        var result = exceptionType.hashCode()
        result = 31 * result + (message?.hashCode() ?: 0)
        result = 31 * result + elements.hashCode()
        result = 31 * result + (causedBy?.hashCode() ?: 0)
        return result
    }
}