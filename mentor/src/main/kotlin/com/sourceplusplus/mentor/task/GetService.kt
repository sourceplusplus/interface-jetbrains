package com.sourceplusplus.mentor.task

import com.sourceplusplus.mentor.MentorJob
import com.sourceplusplus.mentor.MentorTask
import com.sourceplusplus.monitor.skywalking.track.ServiceTracker.Companion.getActiveServices
import com.sourceplusplus.monitor.skywalking.track.ServiceTracker.Companion.getCurrentService
import monitor.skywalking.protocol.metadata.GetAllServicesQuery
import java.util.*

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class GetService(
    private val byId: String? = null,
    private val byName: String? = null,
    private val current: Boolean = true
) : MentorTask() {

    companion object {
        val SERVICE: ContextKey<GetAllServicesQuery.Result> = ContextKey()
    }

    override suspend fun executeTask(job: MentorJob, context: TaskContext) {
        if (current) {
            val service = getCurrentService(job.vertx)
            if (service != null && isMatch(service)) {
                context.put(SERVICE, service)
            }
        } else {
            for (service in getActiveServices(job.vertx)) {
                if (isMatch(service)) {
                    context.put(SERVICE, service)
                    break
                }
            }
        }
    }

    private fun isMatch(result: GetAllServicesQuery.Result): Boolean {
        return when {
            byId != null && byName != null && byId == result.id && byName == result.name -> true
            byId != null && byId == result.id -> true
            byName != null && byName == result.name -> true
            else -> false
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GetService) return false
        if (byId != other.byId) return false
        if (byName != other.byName) return false
        if (current != other.current) return false
        return true
    }

    override fun hashCode(): Int {
        return Objects.hash(byId, byName, current)
    }
}
