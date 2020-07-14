package com.sourceplusplus.plugin.intellij.marker.mark.inlay

import com.sourceplusplus.marker.source.mark.inlay.InlayMark
import com.sourceplusplus.marker.source.mark.inlay.config.InlayMarkVirtualText

import java.awt.*

/**
 * Extension of the InlayMarkVirtualText for handling IntelliJ.
 * Used to allow the combination of multiple statuses to a single virtual text instance.
 *
 * @version 0.3.2
 * @since 0.3.2
 * @author <ahref="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class IntelliJVirtualText extends InlayMarkVirtualText {

    private static final Color SPP_RED = Color.decode("#e1483b")
    private String entryMethodStatus = ""
    private String failingArtifactKind = ""
    private String failingArtifactStatus = ""
    private boolean failingOnThrownLine = false

    IntelliJVirtualText(InlayMark inlayMark, String virtualText, boolean inline) {
        super(inlayMark, virtualText)

        textAttributes.setForegroundColor(SPP_RED)
        setUseInlinePresentation(inline)
        inlayMark.configuration.activateOnMouseClick = false
    }

    void updateEntryMethodStatus(String entryMethodStatus) {
        this.entryMethodStatus = entryMethodStatus
        updateVirtualText(makeVirtualText())
    }

    void updateFailingArtifactStatus(String errorKind, String errorStatus, boolean onThrownLine) {
        this.failingArtifactKind = errorKind
        this.failingArtifactStatus = errorStatus
        this.failingOnThrownLine = onThrownLine
        updateVirtualText(makeVirtualText())
    }

    private String makeVirtualText() {
        if (entryMethodStatus && failingArtifactStatus) {
            entryMethodStatus + " | " + failingArtifactKind + " " + failingArtifactStatus
        } else if (entryMethodStatus) {
            return entryMethodStatus
        } else {
            if (failingOnThrownLine) {
                return " //Thrown " + failingArtifactStatus
            } else if (useInlinePresentation) {
                return " //" + failingArtifactKind + " " + failingArtifactStatus
            } else {
                return failingArtifactKind + " " + failingArtifactStatus
            }
        }
    }
}
