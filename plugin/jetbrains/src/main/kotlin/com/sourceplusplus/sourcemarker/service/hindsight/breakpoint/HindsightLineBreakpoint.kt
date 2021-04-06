package com.sourceplusplus.sourcemarker.service.hindsight.breakpoint

import com.intellij.debugger.ui.breakpoints.LineBreakpoint
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.breakpoints.XBreakpoint

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class HindsightLineBreakpoint(project: Project, xBreakpoint: XBreakpoint<HindsightBreakpointProperties>) :
    LineBreakpoint<HindsightBreakpointProperties>(project, xBreakpoint)
