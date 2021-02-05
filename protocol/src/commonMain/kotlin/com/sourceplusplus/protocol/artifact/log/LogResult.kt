package com.sourceplusplus.protocol.artifact.log

import com.sourceplusplus.protocol.Serializers
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.2.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class LogResult(
    val artifactQualifiedName: String? = null,
    val orderType: LogOrderType,
    @Serializable(with = Serializers.InstantKSerializer::class)
    val timestamp: Instant,
    val logs: List<Log> = emptyList(),
    val total: Int = 0
) {
    fun mergeWith(logResult: LogResult): LogResult {
        val result: LogResult = logResult
        val combinedLogs: MutableSet<Log> = HashSet(logs)
        combinedLogs.addAll(result.logs)
        val finalLogs = ArrayList(combinedLogs).sortedWith(Comparator { l1: Log, l2: Log ->
            when (orderType) {
                LogOrderType.NEWEST_LOGS -> return@Comparator l2.timestamp.compareTo(l1.timestamp)
                LogOrderType.OLDEST_LOGS -> return@Comparator l1.timestamp.compareTo(l2.timestamp)
            }
        })
        return result.copy(logs = finalLogs, total = finalLogs.size)
    }

    fun truncate(amount: Int): LogResult {
        return if (logs.size > amount) {
            copy(logs = logs.subList(0, amount), total = logs.size)
        } else this
    }
}
