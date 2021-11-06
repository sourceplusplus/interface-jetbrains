package com.sourceplusplus.sourcemarker.search

import com.sourceplusplus.marker.SourceMarker
import com.sourceplusplus.marker.source.mark.api.ExpressionSourceMark
import com.sourceplusplus.marker.source.mark.api.MethodSourceMark
import com.sourceplusplus.marker.source.mark.api.SourceMark
import spp.protocol.artifact.ArtifactQualifiedName
import spp.protocol.artifact.ArtifactType
import com.sourceplusplus.sourcemarker.mark.SourceMarkKeys

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object SourceMarkSearch {

    fun findByEndpointName(endpointName: String): SourceMark? {
        return SourceMarker.getSourceMarks()
            .firstOrNull {
                it.getUserData(SourceMarkKeys.ENDPOINT_DETECTOR)?.getEndpointName(it) == endpointName
            }
    }

    fun findByLogId(logId: String): SourceMark? {
        return SourceMarker.getSourceMarks()
            .firstOrNull {
                it.getUserData(SourceMarkKeys.LOG_ID) == logId
            }
    }

    fun findByBreakpointId(breakpointId: String): SourceMark? {
        return SourceMarker.getSourceMarks()
            .firstOrNull {
                it.getUserData(SourceMarkKeys.BREAKPOINT_ID) == breakpointId
            }
    }

    fun findByInstrumentId(instrumentId: String): SourceMark? {
        return SourceMarker.getSourceMarks()
            .firstOrNull {
                it.getUserData(SourceMarkKeys.BREAKPOINT_ID) == instrumentId
                        || it.getUserData(SourceMarkKeys.LOG_ID) == instrumentId
            }
    }

    suspend fun findSourceMark(artifact: ArtifactQualifiedName): SourceMark? {
        return when (artifact.type) {
            ArtifactType.ENDPOINT -> findEndpointSourceMark(artifact)
            ArtifactType.STATEMENT -> findExpressionAdvice(artifact)
            else -> TODO("impl")
        }
    }

    suspend fun findSourceMark(logPattern: String): MethodSourceMark? {
        return SourceMarker.getSourceMarks()
            .filterIsInstance<MethodSourceMark>()
            .firstOrNull {
                it.getUserData(SourceMarkKeys.LOGGER_DETECTOR)!!.getOrFindLoggerStatements(it)
                    .map { it.logPattern }.contains(logPattern)
            }
    }

    suspend fun findInheritedSourceMarks(logPattern: String): List<SourceMark> {
        val rootMark = findSourceMark(logPattern) ?: return emptyList()
        return findInheritedSourceMarks(rootMark)
    }

    fun findInheritedSourceMarks(rootMark: SourceMark): List<SourceMark> {
        return if (rootMark.isExpressionMark) {
            val methodMark = SourceMarker.getSourceMark(
                rootMark.artifactQualifiedName.substringBefore("#"), SourceMark.Type.GUTTER
            )
            //todo: proper class crawl
            listOfNotNull(rootMark, methodMark) + rootMark.sourceFileMarker.getClassSourceMarks()
        } else if (rootMark.isMethodMark) {
            //todo: proper class crawl
            listOf(rootMark) + rootMark.sourceFileMarker.getClassSourceMarks()
        } else {
            listOf(rootMark)
        }
    }

    private suspend fun findEndpointSourceMark(artifact: ArtifactQualifiedName): MethodSourceMark? {
        val operationName = artifact.identifier
        return SourceMarker.getSourceMarks()
            .filterIsInstance<MethodSourceMark>()
            .firstOrNull {
                it.getUserData(SourceMarkKeys.ENDPOINT_DETECTOR)!!.getOrFindEndpointName(it) == operationName
            }
    }

    private fun findExpressionAdvice(artifact: ArtifactQualifiedName): ExpressionSourceMark? {
        val qualifiedClassName = artifact.identifier.substring(0, artifact.identifier.lastIndexOf("."))
        val fileMarker = SourceMarker.getSourceFileMarker(qualifiedClassName)
        return if (fileMarker != null) {
            fileMarker.getSourceMarks().find { it.lineNumber == artifact.lineNumber!! } as ExpressionSourceMark?
        } else {
            null
        }
    }
}
