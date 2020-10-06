package com.sourceplusplus.mentor.task.analyze

import com.sourceplusplus.mentor.MentorJob
import com.sourceplusplus.mentor.MentorJob.ContextKey
import com.sourceplusplus.mentor.MentorTask
import com.sourceplusplus.protocol.advice.informative.ActiveExceptionAdvice
import com.sourceplusplus.protocol.artifact.ArtifactLocation
import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName
import com.sourceplusplus.protocol.artifact.ArtifactType
import com.sourceplusplus.protocol.artifact.exception.JvmStackTrace
import com.sourceplusplus.protocol.artifact.exception.JvmStackTraceElement
import com.sourceplusplus.protocol.artifact.trace.TraceSpanStackQueryResult

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class DetermineThrowableLocation(
    private val byTraceStacksContext: ContextKey<List<TraceSpanStackQueryResult>>,
    private val rootPackage: String
) : MentorTask() {

    companion object {
        private val frameRegex = Regex(
            "(?:\\s*at\\s+)((?:[\\w\\s](?:\\\$+|\\.|\\/)?)+)" +
                    "\\.([\\w|_|\\\$|\\s|<|>]+)\\s*\\(([^\\(\\)]+(?:\\([^\\)]*\\))?)\\)"
        )
        val ARTIFACT_LOCATION: ContextKey<ArtifactLocation> =
            ContextKey("DetermineThrowableLocation.ARTIFACT_LOCATION")
    }

    override val contextKeys = listOf(ARTIFACT_LOCATION)

    override suspend fun executeTask(job: MentorJob) {
        job.log("Task configuration\n\tbyTraceStacksContext: $byTraceStacksContext\n\trootPackage: $rootPackage")

        //todo: ArtifactLocation more appropriate naming than ArtifactQualifiedName
        val traceStacks = job.context.get(byTraceStacksContext)
        traceStacks.forEach { traceStack ->
            traceStack.traceSpans.forEach { span ->
                span.logs.forEach { logEntry ->
                    if (frameRegex.containsMatchIn(logEntry.data)) {
                        val logLines = logEntry.data.split("\n")
                        var message: String? = null
                        val exceptionClass = if (logLines[0].contains(":")) {
                            message = logLines[0].substring(logLines[0].indexOf(":") + 2)
                            logLines[0].substring(0, logLines[0].indexOf(":"))
                        } else {
                            logLines[0]
                        }
                        val elements = mutableListOf<JvmStackTraceElement>()
                        for (el in frameRegex.findAll(logEntry.data)) {
                            val clazz = el.groupValues[1]
                            val method = el.groupValues[2]
                            val source = el.groupValues[3]
                            elements.add(JvmStackTraceElement("$clazz.$method", source))
                        }
                        val stackTrace = JvmStackTrace(exceptionClass, message, elements)

                        val domainExceptionLine = stackTrace.elements
                            .find { it.method.startsWith(rootPackage) }
                        if (domainExceptionLine != null) {
                            val location = domainExceptionLine.method
                            val lineNumber = domainExceptionLine.sourceAsLineNumber!!

                            job.addAdvice(
                                ActiveExceptionAdvice(
                                    ArtifactQualifiedName(
                                        identifier = location,
                                        commitId = "todo", //todo: get commit id from service instance
                                        type = ArtifactType.STATEMENT,
                                        lineNumber = lineNumber
                                    ), stackTrace = stackTrace
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
