package com.sourceplusplus.sourcemarker.service.hindsight.breakpoint

import com.intellij.debugger.ui.breakpoints.Breakpoint
import com.intellij.debugger.ui.breakpoints.JavaBreakpointType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.xdebugger.breakpoints.XBreakpoint
import com.intellij.xdebugger.breakpoints.XLineBreakpointType
import com.sourceplusplus.protocol.SourceMarkerServices.Instance.Tracing
import com.sourceplusplus.sourcemarker.icons.SourceMarkerIcons
import javax.swing.Icon

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class HindsightBreakpointType : XLineBreakpointType<HindsightBreakpointProperties>(
    "hindsight-breakpoint", "Hindsight Breakpoint"
), JavaBreakpointType<HindsightBreakpointProperties> {

    override fun canPutAt(file: VirtualFile, line: Int, project: Project): Boolean {
        return Tracing.hindsightDebugger != null
    }

    override fun getEnabledIcon(): Icon {
        return SourceMarkerIcons.EYE_ICON
    }

    override fun getDisabledIcon(): Icon {
        return SourceMarkerIcons.EYE_SLASH_ICON
    }

    override fun createBreakpointProperties(file: VirtualFile, line: Int): HindsightBreakpointProperties {
        return HindsightBreakpointProperties()
    }

    override fun createJavaBreakpoint(
        project: Project, breakpoint: XBreakpoint<HindsightBreakpointProperties>
    ): Breakpoint<HindsightBreakpointProperties> {
        return HindsightLineBreakpoint(project, breakpoint)
    }
}
