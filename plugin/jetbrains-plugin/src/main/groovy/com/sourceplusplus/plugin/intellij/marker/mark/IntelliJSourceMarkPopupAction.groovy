package com.sourceplusplus.plugin.intellij.marker.mark

import com.intellij.openapi.editor.Editor
import com.sourceplusplus.marker.source.mark.SourceMarkPopupAction
import com.sourceplusplus.marker.source.mark.gutter.GutterMark
import com.sourceplusplus.plugin.intellij.marker.mark.gutter.IntelliJGutterMark
import org.jetbrains.annotations.NotNull

/**
 * Allows the Source++ Portal to be displayed via keyboard action.
 * Ensures the portal is registered before popup.
 *
 * @version 0.3.0
 * @since 0.3.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class IntelliJSourceMarkPopupAction extends SourceMarkPopupAction {

    @Override
    void performPopupAction(@NotNull GutterMark gutterMark, @NotNull Editor editor) {
        (gutterMark as IntelliJGutterMark).registerPortal()
        super.performPopupAction(gutterMark, editor)
    }
}
