package spp.jetbrains.sourcemarker.service.breakpoint

import spp.protocol.artifact.exception.LiveStackTrace
import spp.protocol.artifact.exception.LiveStackTraceElement

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
