package com.sourceplusplus.sourcemarker.service.hindsight.breakpoint

import com.intellij.debugger.ui.breakpoints.LineBreakpoint
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.breakpoints.XBreakpoint
import com.sourceplusplus.sourcemarker.icons.SourceMarkerIcons
import javax.swing.Icon

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class HindsightLineBreakpoint(project: Project, xBreakpoint: XBreakpoint<HindsightBreakpointProperties>) :
    LineBreakpoint<HindsightBreakpointProperties>(project, xBreakpoint) {

    override fun getIcon(): Icon {
        return determineIcon()
    }

    override fun getDisabledIcon(isMuted: Boolean): Icon {
        return determineIcon()
    }

    override fun getInvalidIcon(isMuted: Boolean): Icon {
        return determineIcon()
    }

    override fun getSetIcon(isMuted: Boolean): Icon {
        return determineIcon()
    }

    override fun getVerifiedIcon(isMuted: Boolean): Icon {
        return determineIcon()
    }

    override fun getVerifiedWarningsIcon(isMuted: Boolean): Icon {
        return determineIcon()
    }

    override fun getValidatingIcon(muted: Boolean): Icon {
        return determineIcon()
    }

    private fun determineIcon(): Icon {
        val properties = xBreakpoint.properties
        return when {
            !properties.getSuspend() -> SourceMarkerIcons.YELLOW_EYE_ICON
            properties.getFinished() -> SourceMarkerIcons.GREEN_EYE_ICON
            properties.getActive() -> SourceMarkerIcons.EYE_ICON
            else -> SourceMarkerIcons.GREY_EYE_ICON
        }
    }
}
