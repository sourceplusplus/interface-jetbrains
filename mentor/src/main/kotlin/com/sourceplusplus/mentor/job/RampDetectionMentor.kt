package com.sourceplusplus.mentor.job

import com.sourceplusplus.mentor.MentorJob
import com.sourceplusplus.mentor.MentorTask
import com.sourceplusplus.mentor.task.GetService
import com.sourceplusplus.mentor.task.GetServiceInstance
import io.vertx.core.Vertx

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class RampDetectionMentor(
    override val vertx: Vertx
) : MentorJob() {

    override val tasks: List<MentorTask> = listOf(
        //get active service instance
        GetService(),
        GetServiceInstance(
            GetService.SERVICE
        )

        //todo: find endpoints with consistently increasing response time of a certain threshold
        //todo: search source code of endpoint for culprits
    )
}