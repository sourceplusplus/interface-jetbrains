package com.sourceplusplus.sourcemarker.service.breakpoint

import com.sourceplusplus.protocol.artifact.exception.JvmStackTrace
import com.sourceplusplus.protocol.artifact.exception.JvmStackTraceElement

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class StackFrameManager(val stackTrace: JvmStackTrace) {
    var currentFrame: JvmStackTraceElement? = null
    var currentFrameIndex: Int = 0
}
