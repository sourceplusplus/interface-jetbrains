package com.sourceplusplus.sourcemarker.service.breakpoint

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface DebugStackFrameListener {
    fun onChanged(stackFrameManager: StackFrameManager)
}
