package com.sourceplusplus.marker.py

import com.sourceplusplus.marker.ArtifactScopeService
import com.sourceplusplus.marker.source.SourceFileMarker

class PythonArtifactScopeService : ArtifactScopeService {

    override fun getScopeVariables(fileMarker: SourceFileMarker, lineNumber: Int): List<String> {
        return emptyList() //todo: this
    }
}
