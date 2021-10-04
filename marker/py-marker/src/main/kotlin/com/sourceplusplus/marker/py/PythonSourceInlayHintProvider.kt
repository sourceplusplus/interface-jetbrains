package com.sourceplusplus.marker.py

import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.psi.PsiElement
import com.sourceplusplus.marker.plugin.SourceInlayHintProvider
import com.sourceplusplus.marker.source.mark.inlay.InlayMark
import com.sourceplusplus.marker.source.mark.inlay.config.InlayMarkVirtualText

class PythonSourceInlayHintProvider : SourceInlayHintProvider() {
    override fun createInlayMarkIfNecessary(element: PsiElement): InlayMark? {
        TODO("Not yet implemented")
    }

    override fun doThing(
        element: PsiElement,
        virtualText: InlayMarkVirtualText,
        sink: InlayHintsSink,
        representation: InlayPresentation
    ) {
        TODO("Not yet implemented")
    }
}
