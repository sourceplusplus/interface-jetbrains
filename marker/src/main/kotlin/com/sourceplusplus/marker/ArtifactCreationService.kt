package com.sourceplusplus.marker

import com.sourceplusplus.marker.source.SourceFileMarker
import com.sourceplusplus.marker.source.mark.inlay.ExpressionInlayMark
import java.util.*

interface ArtifactCreationService {

    fun getOrCreateExpressionInlayMark(
        fileMarker: SourceFileMarker,
        lineNumber: Int,
        autoApply: Boolean = false
    ): Optional<ExpressionInlayMark>

    fun createExpressionInlayMark(
        fileMarker: SourceFileMarker,
        lineNumber: Int,
        autoApply: Boolean = false
    ): ExpressionInlayMark
}
