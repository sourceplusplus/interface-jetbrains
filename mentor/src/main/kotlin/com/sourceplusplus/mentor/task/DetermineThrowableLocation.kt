package com.sourceplusplus.mentor.task

import com.sourceplusplus.mentor.MentorJob
import com.sourceplusplus.mentor.MentorJob.ContextKey
import com.sourceplusplus.mentor.MentorTask
import com.sourceplusplus.monitor.skywalking.track.EndpointTracesTracker
import com.sourceplusplus.protocol.artifact.ArtifactLocation
import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName
import com.sourceplusplus.protocol.artifact.ArtifactType
import com.sourceplusplus.protocol.artifact.trace.TraceResult

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class DetermineThrowableLocation(
    private val byTracesContext: ContextKey<TraceResult>,
    private val rootPackage: String
) : MentorTask() {

    companion object {
        val ARTIFACT_LOCATION: ContextKey<ArtifactLocation> = ContextKey()
    }

    override val contextKeys = listOf(ARTIFACT_LOCATION)

    override suspend fun executeTask(job: MentorJob) {
        job.log("Executing task: $this")

        //todo: ArtifactLocation more appropriate naming than ArtifactQualifiedName
        val domainExceptions = mutableMapOf<ArtifactQualifiedName, List<String>>()
        val traceResult = job.context.get(byTracesContext)
        traceResult.traces.distinctBy { it.operationNames }.forEach { trace ->
            val traceStack = EndpointTracesTracker.getTraceStack(trace.traceIds[0], job.vertx)
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