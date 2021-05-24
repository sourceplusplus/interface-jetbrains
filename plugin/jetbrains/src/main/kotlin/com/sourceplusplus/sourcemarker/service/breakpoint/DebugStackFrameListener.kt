package com.sourceplusplus.sourcemarker.service.breakpoint

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface DebugStackFrameListener {
    fun onChanged(stackFrameManager: StackFrameManager)
}
