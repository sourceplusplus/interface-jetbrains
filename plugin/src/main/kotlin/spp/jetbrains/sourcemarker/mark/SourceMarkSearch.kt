/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package spp.jetbrains.sourcemarker.mark

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.MethodSourceMark
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.guide.ClassGuideMark
import spp.jetbrains.marker.source.mark.guide.ExpressionGuideMark
import spp.jetbrains.marker.source.mark.guide.GuideMark
import spp.jetbrains.marker.source.mark.guide.MethodGuideMark
import spp.protocol.artifact.ArtifactQualifiedName
import spp.protocol.artifact.ArtifactType

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object SourceMarkSearch {

    fun getClosestGuideMark(sourceFileMarker: SourceFileMarker, editor: Editor): GuideMark? {
        var classSourceMark: ClassGuideMark? = null
        val sourceMark = sourceFileMarker.getSourceMarks().filterIsInstance<GuideMark>().find {
            if (it is ClassGuideMark) {
                classSourceMark = it //todo: probably doesn't handle inner classes well
                false
            } else if (it is MethodGuideMark) {
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

    fun findByEndpointName(endpointName: String): GuideMark? {
        return SourceMarker.getSourceMarks().filterIsInstance<GuideMark>().firstOrNull {
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

    suspend fun findSourceMark(artifact: ArtifactQualifiedName): GuideMark? {
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

    private suspend fun findEndpointSourceMark(artifact: ArtifactQualifiedName): MethodGuideMark? {
        val operationName = artifact.identifier
        return SourceMarker.getSourceMarks()
            .filterIsInstance<MethodGuideMark>()
            .firstOrNull {
                it.getUserData(SourceMarkKeys.ENDPOINT_DETECTOR)!!.getOrFindEndpointName(it) == operationName
            }
    }

    private fun findExpressionSourceMark(artifact: ArtifactQualifiedName): ExpressionGuideMark? {
        if (artifact.type == ArtifactType.EXPRESSION) {
            return SourceMarker.getSourceMarks().filterIsInstance<ExpressionGuideMark>()
                .find { it.artifactQualifiedName == artifact }
        }

        val qualifiedClassName = artifact.identifier.substring(0, artifact.identifier.lastIndexOf("."))
        val fileMarker = SourceMarker.getSourceFileMarker(qualifiedClassName)
        return fileMarker?.getSourceMarks()?.filterIsInstance<ExpressionGuideMark>()
            ?.find { it.lineNumber == artifact.lineNumber!! }
    }
}
