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
package spp.jetbrains.sourcemarker.mark

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.ClassSourceMark
import spp.jetbrains.marker.source.mark.api.ExpressionSourceMark
import spp.jetbrains.marker.source.mark.api.MethodSourceMark
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.protocol.artifact.ArtifactQualifiedName
import spp.protocol.artifact.ArtifactType

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object SourceMarkSearch {

    fun getClosestSourceMark(sourceFileMarker: SourceFileMarker, editor: Editor): SourceMark? {
        var classSourceMark: ClassSourceMark? = null
        val sourceMark = sourceFileMarker.getSourceMarks().find {
            if (it is ClassSourceMark) {
                classSourceMark = it //todo: probably doesn't handle inner classes well
                false
            } else if (it is MethodSourceMark) {
                if (it.configuration.activateOnKeyboardShortcut) {
                    //+1 on end offset so match is made even right after method end
                    val incTextRange = TextRange(
                        it.getPsiMethod().textRange.startOffset,
                        it.getPsiMethod().textRange.endOffset + 1
                    )
                    incTextRange.contains(editor.logicalPositionToOffset(editor.caretModel.logicalPosition))
                } else {
                    false
                }
            } else {
                false
            }
        }
        return sourceMark ?: classSourceMark
    }

    fun findByEndpointName(endpointName: String): SourceMark? {
        return SourceMarker.getSourceMarks().firstOrNull {
            it.getUserData(SourceMarkKeys.ENDPOINT_DETECTOR)?.getEndpointName(it) == endpointName
        }
    }

    fun findByInstrumentId(instrumentId: String): SourceMark? {
        return SourceMarker.getSourceMarks().firstOrNull {
            it.getUserData(SourceMarkKeys.INSTRUMENT_ID) == instrumentId
        }
    }

    fun findBySubscriptionId(subscriptionId: String): SourceMark? {
        return SourceMarker.getSourceMarks().firstOrNull {
            it.getUserData(SourceMarkKeys.VIEW_SUBSCRIPTION_ID) == subscriptionId
        }
    }

    suspend fun findSourceMark(artifact: ArtifactQualifiedName): SourceMark? {
        return when (artifact.type) {
            ArtifactType.ENDPOINT -> findEndpointSourceMark(artifact)
            ArtifactType.STATEMENT -> findExpressionSourceMark(artifact)
            ArtifactType.EXPRESSION -> findExpressionSourceMark(artifact)
            else -> TODO("impl")
        }
    }

    fun findSourceMarks(artifact: ArtifactQualifiedName): List<SourceMark> {
        return SourceMarker.getSourceMarks().filter { it.artifactQualifiedName == artifact }
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

    private fun findExpressionSourceMark(artifact: ArtifactQualifiedName): ExpressionSourceMark? {
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
