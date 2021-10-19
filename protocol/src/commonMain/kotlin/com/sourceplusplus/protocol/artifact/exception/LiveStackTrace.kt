package com.sourceplusplus.protocol.artifact.exception

import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
class LiveStackTrace(
    var exceptionType: String,
    var message: String?,
    val elements: MutableList<LiveStackTraceElement>,
    val causedBy: LiveStackTrace? = null
) : Iterable<LiveStackTraceElement> {

    fun getElements(hideApacheSkywalking: Boolean): List<LiveStackTraceElement> {
        if (hideApacheSkywalking) {
            //skip skywalking interceptor element(s) and accompanying $original/$auxiliary elements
            val finalElements = mutableListOf<LiveStackTraceElement>()
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
            return finalElements.reversed().filterNot {
                it.source.contains("/skywalking/plugins/")
                        || it.source.contains("/nopdb/nopdb/")
                        || it.source.contains("/probe-python/ContextReceiver.py")
            }
        } else {
            return elements
        }
    }

    companion object {
        private const val skywalkingInterceptor =
            "org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstMethodsInter.intercept"
        private val frameRegex = Regex(
            "\\s*at\\s+((?:[\\w\\s](?:\\\$+|\\.|/)?)+)" +
                    "\\.([\\w|_$\\s<>]+)\\s*\\(([^()]+(?:\\([^)]*\\))?)\\)"
        )
        private val pythonFrameRegex = Regex(
            " {2}File \"(.+)\", line ([0-9]+), in (.+)\\n {4}(.+)"
        )

        fun fromString(data: String): LiveStackTrace? {
            return when {
                frameRegex.containsMatchIn(data) -> extractJvmStackTrace(data)
                pythonFrameRegex.containsMatchIn(data) -> extractPythonStackTrace(data)
                else -> null
            }
        }

        private fun extractPythonStackTrace(data: String): LiveStackTrace {
            val elements = mutableListOf<LiveStackTraceElement>()
            for (el in pythonFrameRegex.findAll(data).toList().reversed()) {
                val file = el.groupValues[1]
                val lineNumber = el.groupValues[2]
                val inLocation = el.groupValues[3]
                val sourceCode = el.groupValues[4]
                elements.add(LiveStackTraceElement(inLocation, "$file:$lineNumber", sourceCode = sourceCode))
            }
            return LiveStackTrace("n/a", "n/a", elements)
        }

        private fun extractJvmStackTrace(data: String): LiveStackTrace {
            val logLines = data.split("\n")
            var message: String? = null
            val exceptionClass = if (logLines[0].contains(":")) {
                message = logLines[0].substring(logLines[0].indexOf(":") + 2)
                logLines[0].substring(0, logLines[0].indexOf(":"))
            } else {
                logLines[0]
            }
            val elements = mutableListOf<LiveStackTraceElement>()
            for (el in frameRegex.findAll(data)) {
                val clazz = el.groupValues[1]
                val method = el.groupValues[2]
                val source = el.groupValues[3]
                elements.add(LiveStackTraceElement("$clazz.$method", source))
            }
            return LiveStackTrace(exceptionClass, message, elements)
        }
    }

    override fun iterator(): Iterator<LiveStackTraceElement> {
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
        if (other !is LiveStackTrace) return false
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
