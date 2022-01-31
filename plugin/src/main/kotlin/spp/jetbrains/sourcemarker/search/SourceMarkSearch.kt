/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package spp.jetbrains.sourcemarker.search

import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.source.mark.api.ExpressionSourceMark
import spp.jetbrains.marker.source.mark.api.MethodSourceMark
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.sourcemarker.mark.SourceMarkKeys
import spp.protocol.artifact.ArtifactQualifiedName
import spp.protocol.artifact.ArtifactType

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

    fun findByMeterId(meterId: String): SourceMark? {
        return SourceMarker.getSourceMarks()
            .firstOrNull {
                it.getUserData(SourceMarkKeys.METER_ID) == meterId
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
                        || it.getUserData(SourceMarkKeys.METER_ID) == instrumentId
            }
    }

    suspend fun findSourceMark(artifact: ArtifactQualifiedName): SourceMark? {
        return when (artifact.type) {
            ArtifactType.ENDPOINT -> findEndpointSourceMark(artifact)
            ArtifactType.STATEMENT -> findExpressionAdvice(artifact)
            ArtifactType.EXPRESSION -> findExpressionAdvice(artifact)
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
                rootMark.artifactQualifiedName.copy(
                    identifier = rootMark.artifactQualifiedName.identifier.substringBefore("#"),
                    type = ArtifactType.METHOD
                ),
                SourceMark.Type.GUTTER
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
        if (artifact.type == ArtifactType.EXPRESSION) {
            return SourceMarker.getSourceMarks().find { it.artifactQualifiedName == artifact } as ExpressionSourceMark?
        }

        val qualifiedClassName = artifact.identifier.substring(0, artifact.identifier.lastIndexOf("."))
        val fileMarker = SourceMarker.getSourceFileMarker(qualifiedClassName)
        return if (fileMarker != null) {
            fileMarker.getSourceMarks().find { it.lineNumber == artifact.lineNumber!! } as ExpressionSourceMark?
        } else {
            null
        }
    }
}
