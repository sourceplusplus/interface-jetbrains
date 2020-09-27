package com.sourceplusplus.mentor.task

import com.sourceplusplus.mentor.MentorJob
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
        val SERVICE_INSTANCE: ContextKey<GetServiceInstancesQuery.Result> = ContextKey()
    }

    override suspend fun executeTask(job: MentorJob, context: TaskContext) {
        val serviceId = if (byContext != null) {
            context.get(byContext).id
        } else {
            byId
        }!!

        for (serviceInstance in getServiceInstances(serviceId, job.vertx)) {
            if (isMatch(serviceInstance)) {
                context.put(SERVICE_INSTANCE, serviceInstance)
                break
            }
        }
    }

    private fun isMatch(result: GetServiceInstancesQuery.Result): Boolean {
        return when {
            byId != null && byName != null && byId == result.id && byName == result.name -> true
            byId != null && byId == result.id -> true
            byName != null && byName == result.name -> true
            else -> false
        }
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
