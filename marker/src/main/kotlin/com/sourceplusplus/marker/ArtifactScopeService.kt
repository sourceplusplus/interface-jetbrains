package com.sourceplusplus.marker

import com.sourceplusplus.marker.source.SourceFileMarker

interface ArtifactScopeService {

    fun getScopeVariables(fileMarker: SourceFileMarker, lineNumber: Int): List<String>
}
