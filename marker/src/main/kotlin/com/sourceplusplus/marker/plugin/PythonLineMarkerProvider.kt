package com.sourceplusplus.marker.plugin

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.psi.PsiElement

abstract class PythonLineMarkerProvider : SourceLineMarkerProvider() {
    override fun getLineMarkerInfo(parent: PsiElement?, element: PsiElement): LineMarkerInfo<PsiElement>? {
        return null //todo: this
    }
}
