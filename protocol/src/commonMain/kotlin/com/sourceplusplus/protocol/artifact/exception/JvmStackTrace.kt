package com.sourceplusplus.protocol.artifact.exception

import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
class JvmStackTrace(
    var exceptionType: String,
    var message: String?,
    val elements: MutableList<JvmStackTraceElement>,
    val causedBy: JvmStackTrace? = null
) : Iterable<JvmStackTraceElement> {

    fun getElements(hideApacheSkywalking: Boolean): List<JvmStackTraceElement> {
        if (hideApacheSkywalking) {
            //skip skywalking interceptor element(s) and accompanying $original/$auxiliary elements
            val finalElements = mutableListOf<JvmStackTraceElement>()
            var skipTo = 0
            val reversedElements = elements.reversed()
            for (i in reversedElements.indices) {
                if (i < skipTo) continue
                val el = reversedElements[i]
                if (el.method == skywalkingInterceptor) {
                    val needsUpdateEl = reversedElements[i - 1]
                    var x = i
                    while (x++ < reversedElements.size) {
                        val tillEl = reversedElements[x]
                        if (tillEl.sourceAsLineNumber() != null) {
                            //copy over source line number
                            finalElements[finalElements.size - 1] = needsUpdateEl.copy(source = tillEl.source)
                            skipTo = x + 1
                            break
                        }
                    }
                } else {
                    finalElements.add(el)
                }
            }
            return finalElements.reversed()
        } else {
            return elements
        }
    }

    companion object {
        private const val skywalkingInterceptor =
            "org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstMethodsInter.intercept"
        private val frameRegex = Regex(
            "(?:\\s*at\\s+)((?:[\\w\\s](?:\\\$+|\\.|\\/)?)+)" +
                    "\\.([\\w|_|\\\$|\\s|<|>]+)\\s*\\(([^\\(\\)]+(?:\\([^\\)]*\\))?)\\)"
        )

        fun fromString(data: String): JvmStackTrace? {
            if (!frameRegex.containsMatchIn(data)) {
                return null
            }

            val logLines = data.split("\n")
            var message: String? = null
            val exceptionClass = if (logLines[0].contains(":")) {
                message = logLines[0].substring(logLines[0].indexOf(":") + 2)
                logLines[0].substring(0, logLines[0].indexOf(":"))
            } else {
                logLines[0]
            }
            val elements = mutableListOf<JvmStackTraceElement>()
            for (el in frameRegex.findAll(data)) {
                val clazz = el.groupValues[1]
                val method = el.groupValues[2]
                val source = el.groupValues[3]
                elements.add(JvmStackTraceElement("$clazz.$method", source))
            }
            return JvmStackTrace(exceptionClass, message, elements)
        }
    }

    override fun iterator(): Iterator<JvmStackTraceElement> {
        return elements.iterator()
    }

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
