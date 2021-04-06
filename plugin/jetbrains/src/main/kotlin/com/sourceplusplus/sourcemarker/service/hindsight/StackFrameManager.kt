package com.sourceplusplus.sourcemarker.service.hindsight

import com.sourceplusplus.protocol.artifact.exception.JvmStackTrace
import com.sourceplusplus.protocol.artifact.exception.JvmStackTraceElement

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class StackFrameManager(val stackTrace: JvmStackTrace) {
    var currentFrame: JvmStackTraceElement? = null
    var currentFrameIndex: Int = 0
}
