package com.sourceplusplus.mentor.impl.task.monitor

import com.sourceplusplus.mentor.base.MentorJob
import com.sourceplusplus.mentor.base.MentorJob.ContextKey
import com.sourceplusplus.mentor.base.MentorJob.TaskContext
import com.sourceplusplus.mentor.base.MentorTask
import com.sourceplusplus.monitor.skywalking.track.ServiceTracker.Companion.getActiveServices
import com.sourceplusplus.monitor.skywalking.track.ServiceTracker.Companion.getActiveServicesAwait
import com.sourceplusplus.monitor.skywalking.track.ServiceTracker.Companion.getCurrentService
import com.sourceplusplus.monitor.skywalking.track.ServiceTracker.Companion.getCurrentServiceAwait
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
    private val current: Boolean = true,
    private val await: Boolean = true
) : MentorTask() {

    override val remainValidDuration: Long = 5 * 60 * 1000

    companion object {
        val SERVICE: ContextKey<GetAllServicesQuery.Result> = ContextKey("GetService.SERVICE")
    }

    override val outputContextKeys = listOf(SERVICE)

    override suspend fun executeTask(job: MentorJob) {
        job.log("Task configuration\n\tbyId: $byId\n\tbyName: $byName\n\tcurrent: $current")

        if (current) {
            val service = if (await) {
                getCurrentServiceAwait(job.vertx)
            } else {
                getCurrentService(job.vertx)
            }
            if (service != null && isMatch(service)) {
                job.context.put(SERVICE, service)
                job.log("Added context\n\tKey: $SERVICE\n\tValue: $service")
            } else {
                job.log("Failed to add context: $SERVICE")
            }
        } else {
            var addedContext = false
            val services = if (await) {
                getActiveServicesAwait(job.vertx)
            } else {
                getActiveServices(job.vertx)
            }
            for (service in services) {
                if (isMatch(service)) {
                    job.context.put(SERVICE, service)
                    job.log("Added context\n\tKey: $SERVICE\n\tValue: $service")
                    addedContext = true
                    break
                }
            }
            if (!addedContext) {
                job.log("Failed to add context: $SERVICE")
            }
        }
    }

    private fun isMatch(result: GetAllServicesQuery.Result): Boolean {
        return when {
            byId == null && byName == null -> true
            byId != null && byName != null && byId == result.id && byName == result.name -> true
            byId != null && byId == result.id -> true
            byName != null && byName == result.name -> true
            else -> false
        }
    }

    /**
     * This task doesn't use any context but should be consider using the same context if any tasks are equal to it.
     */
    override fun usingSameContext(
        selfContext: TaskContext,
        otherContext: TaskContext,
        task: MentorTask
    ): Boolean = true

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
