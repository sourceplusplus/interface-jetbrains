package com.sourceplusplus.mentor.task

import com.sourceplusplus.mentor.MentorJob
import com.sourceplusplus.mentor.MentorJob.ContextKey
import com.sourceplusplus.mentor.MentorTask
import com.sourceplusplus.monitor.skywalking.track.ServiceInstanceTracker.Companion.getServiceInstances
import monitor.skywalking.protocol.metadata.GetAllServicesQuery
import monitor.skywalking.protocol.metadata.GetServiceInstancesQuery
import java.util.*

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class GetServiceInstance(
    private val byContext: ContextKey<GetAllServicesQuery.Result>? = null,
    private val byId: String? = null,
    private val byName: String? = null
) : MentorTask() {

    companion object {
        val SERVICE_INSTANCE: ContextKey<GetServiceInstancesQuery.Result> =
            ContextKey("GetServiceInstance.SERVICE_INSTANCE")
    }

    override val contextKeys = listOf(SERVICE_INSTANCE)

    override suspend fun executeTask(job: MentorJob) {
        job.log("Executing task: $this")
        job.log("Task configuration\n\tbyContext: $byContext\n\tbyId: $byId\n\tbyName: $byName")

        val serviceId = if (byContext != null) {
            job.context.get(byContext).id
        } else {
            byId
        }!!

        for (serviceInstance in getServiceInstances(serviceId, job.vertx)) {
            if (isMatch(serviceInstance)) {
                job.context.put(SERVICE_INSTANCE, serviceInstance)
                job.log("Added context\n\tKey: $SERVICE_INSTANCE\n\tValue: $serviceInstance")
                break
            }
        }
    }

    private fun isMatch(result: GetServiceInstancesQuery.Result): Boolean {
        return when {
            byId == null && byName == null -> true
            byId != null && byName != null && byId == result.id && byName == result.name -> true
            byId != null && byId == result.id -> true
            byName != null && byName == result.name -> true
            else -> false
        }
    }

    override fun usingSameContext(selfJob: MentorJob, job: MentorJob, task: MentorTask): Boolean {
        if (task is GetServiceInstance && byContext != null && task.byContext != null) {
            return selfJob.context.get(byContext) == job.context.get(byContext)
        }
        return true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GetServiceInstance) return false
        if (byContext != other.byContext) return false
        if (byId != other.byId) return false
        if (byName != other.byName) return false
        return true
    }

    override fun hashCode(): Int {
        return Objects.hash(byContext, byId, byName)
    }
}
