package com.sourceplusplus.mentor.task.monitor

import com.sourceplusplus.mentor.MentorJob
import com.sourceplusplus.mentor.MentorJob.ContextKey
import com.sourceplusplus.mentor.MentorTask
import com.sourceplusplus.monitor.skywalking.track.EndpointTracker

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class GetEndpoints(
    private val backoffConfig: BackoffConfig? = null
) : MentorTask() {

    companion object {
        val ENDPOINT_IDS: ContextKey<List<String>> = ContextKey("GetEndpoints.ENDPOINT_IDS")
    }

    override val outputContextKeys = listOf(ENDPOINT_IDS)

    override suspend fun executeTask(job: MentorJob) {

        //todo: need way to iterate endpoints
        val endpoints = EndpointTracker.getEndpoints(100, job.vertx)
        endpoints.forEach {
            backoffConfig?.config?.put(it.id, -1)
        }
        job.context.put(ENDPOINT_IDS, endpoints.map { it.id })
    }

    data class BackoffConfig(
        val config: HashMap<String, Int> = HashMap()
    )
}