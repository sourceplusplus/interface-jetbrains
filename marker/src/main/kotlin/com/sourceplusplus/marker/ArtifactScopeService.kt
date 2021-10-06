package com.sourceplusplus.marker

import com.sourceplusplus.marker.source.SourceFileMarker

/**
 * todo: description.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface ArtifactScopeService {

    fun getScopeVariables(fileMarker: SourceFileMarker, lineNumber: Int): List<String>
}
