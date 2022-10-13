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
package spp.jetbrains.marker.js.service

import com.intellij.lang.Language
import com.intellij.lang.javascript.psi.*
import com.intellij.lang.javascript.psi.ecmal4.JSClass
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.parentOfType
import spp.jetbrains.marker.SourceMarkerUtils
import spp.jetbrains.marker.service.define.IArtifactNamingService
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.protocol.artifact.ArtifactLanguage
import spp.protocol.artifact.ArtifactQualifiedName
import spp.protocol.artifact.ArtifactType
import spp.protocol.artifact.exception.LiveStackTraceElement
import spp.protocol.instrument.LiveSourceLocation
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

    override fun getLocation(
        language: Language,
        artifactQualifiedName: ArtifactQualifiedName,
        shorten: Boolean
    ): String {
        // JS identifiers use virtualFile.path, which is always /-separated
        var location = if (artifactQualifiedName.identifier.contains("(")) {
            artifactQualifiedName.identifier
        } else {
            artifactQualifiedName.identifier.substringAfterLast("/").substringBefore("#")
        }

        if (shorten) {
            if (location.length > 75) {
                location = location.substringAfterLast("/")
            }
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
                type = ArtifactType.METHOD,
                lineNumber = SourceMarkerUtils.getLineNumber(element)
            )

            is JSStatement, is JSStatementList -> getStatementOrExpressionQualifiedName(element, ArtifactType.STATEMENT)
            else -> getStatementOrExpressionQualifiedName(element, ArtifactType.EXPRESSION)
        }
    }

    private fun getStatementOrExpressionQualifiedName(element: PsiElement, type: ArtifactType): ArtifactQualifiedName {
        val name = if (element is PsiNamedElement) {
            element.name
        } else {
            Base64.getEncoder().encodeToString(element.toString().toByteArray())
        }

        val parentElement = element.parentOfType<JSFunction>()
        return if (parentElement != null) {
            ArtifactQualifiedName(
                "${getFullyQualifiedName(parentElement).identifier}#${name}",
                type = type,
                lineNumber = SourceMarkerUtils.getLineNumber(element)
            )
        } else {
            ArtifactQualifiedName(
                "${element.containingFile.virtualFile.path}#$name",
                type = type,
                lineNumber = SourceMarkerUtils.getLineNumber(element)
            )
        }
    }

    override fun getQualifiedClassNames(psiFile: PsiFile): List<ArtifactQualifiedName> {
        val classQualifiedNames = mutableListOf<ArtifactQualifiedName>()
        psiFile.acceptChildren(object : JSRecursiveWalkingElementVisitor() {
            override fun visitJSClass(node: JSClass) { // TODO: check this also works for typescript classes, otherwise use visitTypescriptClass
                super.visitJSClass(node)
                classQualifiedNames.add(
                    ArtifactQualifiedName(
                        node.qualifiedName!!,
                        type = ArtifactType.CLASS,
                        lineNumber = SourceMarkerUtils.getLineNumber(node)
                    )
                )
            }
        })
        return classQualifiedNames
    }

    override fun findPsiFile(language: ArtifactLanguage, project: Project, frame: LiveStackTraceElement): PsiFile? {
        return null //todo: this
    }
}
