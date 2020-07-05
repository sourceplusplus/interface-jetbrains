package com.sourceplusplus.plugin.intellij.marker.mark

import com.intellij.openapi.editor.Editor
import com.sourceplusplus.marker.source.mark.SourceMarkPopupAction
import com.sourceplusplus.marker.source.mark.api.SourceMark
import com.sourceplusplus.plugin.intellij.marker.mark.gutter.IntelliJGutterMark

/**
 * Allows the Source++ Portal to be displayed via keyboard action.
 * Ensures the portal is registered before popup.
 *
 * @version 0.3.1
 * @since 0.3.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class IntelliJSourceMarkPopupAction extends SourceMarkPopupAction {

    @Override
    void performPopupAction(SourceMark sourceMark, Editor editor) {
        (sourceMark as IntelliJGutterMark).registerPortal()
        super.performPopupAction(sourceMark, editor)
    }
}
