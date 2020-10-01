package com.sourceplusplus.mentor.task

import com.sourceplusplus.mentor.MentorJob
import com.sourceplusplus.mentor.MentorJob.ContextKey
import com.sourceplusplus.mentor.MentorTask
import com.sourceplusplus.protocol.artifact.ArtifactLocation
import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName
import com.sourceplusplus.protocol.artifact.ArtifactType
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
        val ARTIFACT_LOCATION: ContextKey<ArtifactLocation> =
            ContextKey("DetermineThrowableLocation.ARTIFACT_LOCATION")
    }

    override val contextKeys = listOf(ARTIFACT_LOCATION)

    override suspend fun executeTask(job: MentorJob) {
        job.log("Executing task: $this")
        job.log("Task configuration\n\tbyTraceStacksContext: $byTraceStacksContext\n\trootPackage: $rootPackage")

        //todo: ArtifactLocation more appropriate naming than ArtifactQualifiedName
        val domainExceptions = mutableMapOf<ArtifactQualifiedName, List<String>>()
        val traceStacks = job.context.get(byTraceStacksContext)
        traceStacks.forEach { traceStack ->
            traceStack.traceSpans.forEach { span ->
                span.logs.forEach { logEntry ->
                    val logLines = logEntry.data.split("\n")
                    val domainExceptionLine = logLines.find { it.startsWith("at $rootPackage") }
                    if (domainExceptionLine != null) {
                        val location = domainExceptionLine.substring(3, domainExceptionLine.indexOf("("))
                        val lineNumber = domainExceptionLine.substring(
                            domainExceptionLine.indexOf(":") + 1,
                            domainExceptionLine.indexOf(")")
                        ).toInt()

                        //todo: get commit id from service instance
                        domainExceptions[ArtifactQualifiedName(
                            identifier = location,
                            commitId = "todo",
                            type = ArtifactType.STATEMENT,
                            lineNumber = lineNumber
                        )] = logLines
                    }
                }
            }
        }

        //TODO()
    }
}