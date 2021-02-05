package com.sourceplusplus.protocol.artifact.log

import com.sourceplusplus.protocol.Serializers
import com.sourceplusplus.protocol.artifact.exception.JvmStackTrace
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.2.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class Log(
    @Serializable(with = Serializers.InstantKSerializer::class)
    val timestamp: Instant,
    val content: String,
    val level: String,
    val logger: String? = null,
    val thread: String? = null,
    val exception: JvmStackTrace? = null,
    val arguments: List<String> = listOf()
) {
    fun getFormattedMessage(): String {
        var arg = 0
        var formattedMessage = content
        while (formattedMessage.contains("{}")) {
            formattedMessage = formattedMessage.replaceFirst("{}", arguments[arg++])
        }
        return formattedMessage
    }
}
