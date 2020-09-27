package com.sourceplusplus.mentor

import io.vertx.core.Vertx

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
abstract class MentorJob {

    abstract val vertx: Vertx
    var config: MentorJobConfig = MentorJobConfig()
        private set
    abstract val tasks: List<MentorTask>
    val context = HashMap<String, Any>()
    private var currentTask = -1

    fun log(msg: String) {
        println(msg)
    }

    fun nextTask(): MentorTask = tasks[++currentTask]
    fun hasMoreTasks(): Boolean = currentTask < (tasks.size - 1)

    fun resetJob() {
        currentTask = -1
        context.clear()
    }

    fun withConfig(config: MentorJobConfig): MentorJob {
        this.config = config
        return this
    }

    //todo: if jobs share functionality then they should share tasks
    //todo: would need to search for duplicate tasks when setting up all jobs
}