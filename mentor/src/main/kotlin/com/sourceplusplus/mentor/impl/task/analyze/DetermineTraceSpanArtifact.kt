package com.sourceplusplus.mentor.impl.task.analyze

import com.sourceplusplus.mentor.base.ContextKey
import com.sourceplusplus.mentor.base.MentorJob
import com.sourceplusplus.mentor.base.MentorTask
import com.sourceplusplus.mentor.extend.SqlProducerSearch
import com.sourceplusplus.mentor.impl.task.monitor.GetTraces
import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName
import com.sourceplusplus.protocol.artifact.ArtifactType
import com.sourceplusplus.protocol.artifact.trace.TraceSpan
import org.jooq.Parser
import org.jooq.impl.DSL
import org.jooq.impl.DefaultConfiguration

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class DetermineTraceSpanArtifact(
    private val byTraceSpansContext: ContextKey<List<TraceSpan>>,
    private val sqlProducerSearch: SqlProducerSearch
) : MentorTask() {

    companion object {
        val ARTIFACTS: ContextKey<List<ArtifactQualifiedName>> = ContextKey(
            "DetermineTraceSpanArtifact.ARTIFACTS"
        )
        val RESOLUTION_MAP: ContextKey<Map<ArtifactQualifiedName, String>> = ContextKey(
            "DetermineTraceSpanArtifact.ARTIFACTS"
        )
        private val parser: Parser = DSL.using(DefaultConfiguration()).parser()
    }

    override val outputContextKeys = listOf(ARTIFACTS)

    override suspend fun executeTask(job: MentorJob) {
        val resolutionMap = mutableMapOf<ArtifactQualifiedName, String>()
        val foundArtifacts = mutableListOf<ArtifactQualifiedName>()
        val traceSpans = job.context.get(byTraceSpansContext)
        traceSpans.forEach { traceSpan ->
            if (traceSpan.tags.containsKey("db.statement")) {
                val fullTrace = job.context.get(GetTraces.TRACE_RESULT).traces.find {
                    it.traceIds[0] == traceSpan.traceId
                }!! //todo: cannot assume GetTraces.TRACE_RESULT exists
                val sqlSource = sqlProducerSearch.determineSource(
                    parser.parseQuery(traceSpan.tags["db.statement"]),
                    ArtifactQualifiedName(fullTrace.operationNames[0], "todo", ArtifactType.ENDPOINT),
                )
                if (sqlSource.isPresent) {
                    //got exact source
                    foundArtifacts.add(sqlSource.get())
                    resolutionMap[sqlSource.get()] = fullTrace.operationNames[0]
                } else {
                    //endpoint method is best guess
                    TODO()
                }
            }
        }
        job.context.put(ARTIFACTS, foundArtifacts)
        job.context.put(RESOLUTION_MAP, resolutionMap)
    }
}
