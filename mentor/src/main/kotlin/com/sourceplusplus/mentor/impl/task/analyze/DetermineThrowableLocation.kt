package com.sourceplusplus.mentor.impl.task.analyze

import com.sourceplusplus.mentor.base.ContextKey
import com.sourceplusplus.mentor.base.MentorJob
import com.sourceplusplus.mentor.base.MentorTask
import com.sourceplusplus.protocol.advice.ArtifactAdvice
import com.sourceplusplus.protocol.advice.informative.ActiveExceptionAdvice
import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName
import com.sourceplusplus.protocol.artifact.ArtifactType
import com.sourceplusplus.protocol.artifact.exception.JvmStackTrace
import com.sourceplusplus.protocol.artifact.trace.TraceSpanStackQueryResult

/**
 * Todo: This task doesn't have any persistent memory so it should be able to be shared between contexts.
 *       For that to work it would need to delegate advice adding to a task solely for adding advice.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class DetermineThrowableLocation(
    private val byTraceStacksContext: ContextKey<List<TraceSpanStackQueryResult>>,
    private val rootPackage: String
) : MentorTask() {

    companion object {
        val ARTIFACT_ADVICE: ContextKey<List<ArtifactAdvice>> =
            ContextKey("DetermineThrowableLocation.ARTIFACT_ADVICE")
    }

    override val outputContextKeys = listOf(ARTIFACT_ADVICE)

    override suspend fun executeTask(job: MentorJob) {
        job.log("Task configuration\n\tbyTraceStacksContext: $byTraceStacksContext\n\trootPackage: $rootPackage")

        //todo: ArtifactLocation more appropriate naming than ArtifactQualifiedName
        val foundArtifactLocations = mutableListOf<ActiveExceptionAdvice>()
        val traceStacks = job.context.get(byTraceStacksContext)
        traceStacks.forEach { traceStack ->
            traceStack.traceSpans.forEach { span ->
                span.logs.forEach { logEntry ->
                    val stackTrace = JvmStackTrace.fromString(logEntry.data)
                    if (stackTrace != null) {
                        val domainExceptionLine = stackTrace.elements
                            .find { it.method.startsWith(rootPackage) }
                        if (domainExceptionLine != null) {
                            val location = domainExceptionLine.method
                            val lineNumber = domainExceptionLine.sourceAsLineNumber!!

                            foundArtifactLocations.add(
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

        job.context.put(ARTIFACT_ADVICE, foundArtifactLocations)
    }
}
