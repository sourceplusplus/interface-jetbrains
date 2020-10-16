package com.sourceplusplus.sourcemarker.search

import com.sourceplusplus.marker.plugin.SourceMarkerPlugin
import com.sourceplusplus.marker.source.mark.api.ExpressionSourceMark
import com.sourceplusplus.marker.source.mark.api.MethodSourceMark
import com.sourceplusplus.marker.source.mark.api.SourceMark
import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName
import com.sourceplusplus.protocol.artifact.ArtifactType
import com.sourceplusplus.sourcemarker.SourceMarkKeys

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object SourceMarkSearch {

    suspend fun findSourceMark(artifact: ArtifactQualifiedName): SourceMark? {
        return when (artifact.type) {
            ArtifactType.ENDPOINT -> findEndpointSourceMark(artifact)
            ArtifactType.STATEMENT -> findExpressionAdvice(artifact)
            else -> TODO("impl")
        }
    }

    private suspend fun findEndpointSourceMark(artifact: ArtifactQualifiedName): MethodSourceMark? {
        val operationName = artifact.identifier
        return SourceMarkerPlugin.getSourceMarks()
            .filterIsInstance<MethodSourceMark>()
            .firstOrNull {
                it.getUserData(SourceMarkKeys.ENDPOINT_DETECTOR)!!.getOrFindEndpointName(it) == operationName
            }
    }

    private fun findExpressionAdvice(artifact: ArtifactQualifiedName): ExpressionSourceMark? {
        val qualifiedClassName = artifact.identifier
            .substring(0, artifact.identifier.lastIndexOf("."))
        val fileMarker = SourceMarkerPlugin.getSourceFileMarker(qualifiedClassName)
        return if (fileMarker != null) {
            fileMarker.getSourceMarks().find {
                it.lineNumber == artifact.lineNumber!!
            } as ExpressionSourceMark?
        } else {
            null
        }
    }
}
