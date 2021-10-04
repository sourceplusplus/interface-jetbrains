package com.sourceplusplus.marker.py

import com.sourceplusplus.marker.ArtifactScopeService
import com.sourceplusplus.marker.source.SourceFileMarker

/**
 * todo: description.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class PythonArtifactScopeService : ArtifactScopeService {

    override fun getScopeVariables(fileMarker: SourceFileMarker, lineNumber: Int): List<String> {
        return emptyList() //todo: this
    }
}
