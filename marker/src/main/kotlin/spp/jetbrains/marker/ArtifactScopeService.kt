package spp.jetbrains.marker

import spp.jetbrains.marker.source.SourceFileMarker

/**
 * todo: description.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface ArtifactScopeService {

    fun getScopeVariables(fileMarker: SourceFileMarker, lineNumber: Int): List<String>
}
