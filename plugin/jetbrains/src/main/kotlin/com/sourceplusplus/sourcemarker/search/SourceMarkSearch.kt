package com.sourceplusplus.sourcemarker.search

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.util.TextRange
import com.sourceplusplus.marker.SourceMarker
import com.sourceplusplus.marker.source.SourceFileMarker
import com.sourceplusplus.marker.source.mark.api.ExpressionSourceMark
import com.sourceplusplus.marker.source.mark.api.MethodSourceMark
import com.sourceplusplus.marker.source.mark.api.SourceMark
import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName
import com.sourceplusplus.protocol.artifact.ArtifactType
import com.sourceplusplus.sourcemarker.mark.SourceMarkKeys

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object SourceMarkSearch {

    fun findByLogId(logId: String): SourceMark? {
        return SourceMarker.getSourceMarks()
            .firstOrNull {
                it.getUserData(SourceMarkKeys.LOG_ID) == logId
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
            listOfNotNull(rootMark, methodMark, rootMark.sourceFileMarker.getClassSourceMarks()[0])
        } else if (rootMark.isMethodMark) {
            listOf(rootMark, rootMark.sourceFileMarker.getClassSourceMarks()[0])
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

    fun findMethodSourceMark(editor: Editor, fileMarker: SourceFileMarker, line: Int): MethodSourceMark? {
        return fileMarker.getSourceMarks().find {
            if (it is MethodSourceMark) {
                if (it.configuration.activateOnKeyboardShortcut) {
                    //+1 on end offset so match is made even right after method end
                    val incTextRange = TextRange(
                        it.getPsiMethod().sourcePsi!!.textRange.startOffset,
                        it.getPsiMethod().sourcePsi!!.textRange.endOffset + 1
                    )
                    incTextRange.contains(editor.logicalPositionToOffset(LogicalPosition(line - 1, 0)))
                } else {
                    false
                }
            } else {
                false
            }
        } as MethodSourceMark?
    }
}
