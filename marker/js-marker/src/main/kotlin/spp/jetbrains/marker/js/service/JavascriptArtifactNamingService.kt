/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2023 CodeBrig, Inc.
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
package spp.jetbrains.marker.js.service

import com.intellij.lang.Language
import com.intellij.lang.javascript.psi.*
import com.intellij.lang.javascript.psi.ecmal4.JSClass
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.findTopmostParentInFile
import spp.jetbrains.marker.SourceMarkerUtils
import spp.jetbrains.marker.service.define.IArtifactNamingService
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.protocol.artifact.ArtifactLanguage
import spp.protocol.artifact.ArtifactQualifiedName
import spp.protocol.artifact.ArtifactType
import spp.protocol.artifact.exception.LiveStackTraceElement
import spp.protocol.instrument.location.LiveSourceLocation
import java.util.*

/**
 * Used to determine the naming/location of Javascript artifacts.
 *
 * @since 0.7.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class JavascriptArtifactNamingService : IArtifactNamingService {

    override fun getLiveSourceLocation(
        sourceMark: SourceMark,
        lineNumber: Int,
        serviceName: String?
    ): LiveSourceLocation {
        var locationSource = sourceMark.sourceFileMarker.psiFile.virtualFile.name
        val projectBasePath = sourceMark.project.basePath
        if (projectBasePath != null) {
            val relativePath = sourceMark.sourceFileMarker.psiFile.virtualFile.path.substringAfter(projectBasePath)
            locationSource = if (relativePath.startsWith("/")) {
                relativePath.substring(1)
            } else {
                relativePath
            }
        }
        return LiveSourceLocation(locationSource, lineNumber, service = serviceName)
    }

    override fun getDisplayLocation(language: Language, artifactQualifiedName: ArtifactQualifiedName): String {
        var location = artifactQualifiedName.identifier.substringBefore("#")
        if (location.length > 75) {
            // JS identifiers use virtualFile.path, which is always /-separated
            location = location.substringAfterLast("/")
        }
        return location
    }

    override fun getVariableName(element: PsiElement): String? {
        return if (element is JSVariable) {
            element.name
        } else {
            null
        }
    }

    override fun getFullyQualifiedName(element: PsiElement): ArtifactQualifiedName {
        return when (element) {
            is JSClass -> ArtifactQualifiedName(
                element.qualifiedName!!,
                type = ArtifactType.CLASS,
                lineNumber = SourceMarkerUtils.getLineNumber(element)
            )

            is JSFunctionExpression -> getStatementOrExpressionQualifiedName(element, ArtifactType.EXPRESSION)
            is JSFunction -> ArtifactQualifiedName(
                element.containingFile.virtualFile.path + ":" + element.qualifiedName!! + "()",
                type = ArtifactType.FUNCTION,
                lineNumber = SourceMarkerUtils.getLineNumber(element)
            )

            is JSStatement, is JSStatementList -> getStatementOrExpressionQualifiedName(element, ArtifactType.STATEMENT)
            else -> getStatementOrExpressionQualifiedName(element, ArtifactType.EXPRESSION)
        }
    }

    private fun getStatementOrExpressionQualifiedName(element: PsiElement, type: ArtifactType): ArtifactQualifiedName {
        //todo: each element needs a unique name but encoding the literal text and appending the offset is not unique enough
        // - will need to get a unique hash from PSI or generate a unique name and store it in the PSI
        var expressionString = if (element is PsiNamedElement) {
            element.name ?: ""
        } else {
            element.text ?: ""
        }
        element.textRange.startOffset.let {
            expressionString = "$expressionString:$it"
        }
        expressionString = Base64.getEncoder().encodeToString(expressionString.toByteArray())

        val parentElement = element.findTopmostParentInFile {
            it is JSClass || (it is JSFunction && it !is JSFunctionExpression)
        }
        return if (parentElement != null) {
            ArtifactQualifiedName(
                "${getFullyQualifiedName(parentElement).identifier}#$expressionString",
                type = type,
                lineNumber = SourceMarkerUtils.getLineNumber(element)
            )
        } else {
            ArtifactQualifiedName(
                "${element.containingFile.virtualFile.path}#$expressionString",
                type = type,
                lineNumber = SourceMarkerUtils.getLineNumber(element)
            )
        }
    }

    override fun findPsiFile(language: ArtifactLanguage, project: Project, frame: LiveStackTraceElement): PsiFile? {
        return null //todo: this
    }
}
