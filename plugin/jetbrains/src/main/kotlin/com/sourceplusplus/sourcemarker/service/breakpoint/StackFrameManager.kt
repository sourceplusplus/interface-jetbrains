package com.sourceplusplus.sourcemarker.service.breakpoint

import com.sourceplusplus.protocol.artifact.exception.LiveStackTrace
import com.sourceplusplus.protocol.artifact.exception.LiveStackTraceElement

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class StackFrameManager(val stackTrace: LiveStackTrace) {
    var currentFrame: LiveStackTraceElement? = null
    var currentFrameIndex: Int = 0
}
