package com.sourceplusplus.mentor.impl.task.filter

import com.sourceplusplus.mentor.base.ContextKey
import com.sourceplusplus.mentor.base.MentorJob
import com.sourceplusplus.mentor.base.MentorTask
import com.sourceplusplus.protocol.artifact.trace.TraceSpan
import org.jooq.Parser
import org.jooq.Query
import org.jooq.impl.DSL
import org.jooq.impl.DefaultConfiguration
import org.jooq.impl.ParserException


/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class FilterBoundedDatabaseQueries(
    private val byTraceSpansContext: ContextKey<List<TraceSpan>>
) : MentorTask() {

    companion object {
        val TRACE_SPANS: ContextKey<List<TraceSpan>> = ContextKey("FilterBoundedDatabaseQueries.TRACE_SPANS")
    }

    override val inputContextKeys: List<ContextKey<*>> = listOfNotNull(byTraceSpansContext)
    override val outputContextKeys = listOf(TRACE_SPANS)
    private val parser: Parser = DSL.using(DefaultConfiguration()).parser()

    override suspend fun executeTask(job: MentorJob) {
        val filteredSpans = mutableListOf<TraceSpan>()

        val traceSpans = job.context.get(byTraceSpansContext)
        traceSpans.forEach { traceSpan ->
            try {
                //todo: impl more scenarios, etc
                val query: Query = parser.parseQuery(traceSpan.tags["db.statement"])
                if (!query.hasLimit()) {
                    filteredSpans.add(traceSpan)
                }
            } catch (ex: ParserException) {
                job.warn("Unsupported query: ${ex.sql()}")
            }
        }

        job.context.put(TRACE_SPANS, filteredSpans)
    }

    //todo: remove when jooq offers public access to parsed queries
    private fun Query.hasLimit(): Boolean {
        val limitField = javaClass.getDeclaredField("limit")
        limitField.isAccessible = true
        val limit = limitField.get(this)
        val numberOfRowsField = limit.javaClass.getDeclaredField("numberOfRows")
        numberOfRowsField.isAccessible = true
        val numberOfRows = numberOfRowsField.get(limit)
        return numberOfRows != null
    }
}
