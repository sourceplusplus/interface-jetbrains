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
package spp.jetbrains.marker.py

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.parentOfType
import com.jetbrains.python.psi.*
import spp.jetbrains.marker.AbstractArtifactNamingService
import spp.protocol.artifact.ArtifactQualifiedName
import spp.protocol.artifact.ArtifactType

/**
 * todo: description.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class PythonArtifactNamingService : AbstractArtifactNamingService {

    override fun getLocation(language: String, artifactQualifiedName: ArtifactQualifiedName): String {
        return artifactQualifiedName.identifier
    }

    override fun getVariableName(element: PsiElement): String? {
        return null //todo: this
    }

    override fun getFullyQualifiedName(element: PsiElement): ArtifactQualifiedName {
        return when (element) {
            is PyClass -> {
                ArtifactQualifiedName(element.qualifiedName!!, null, ArtifactType.CLASS)
            }

            is PyFunction -> {
                val parentQualifiedName = PyPsiFacade.getInstance(element.project)
                    .findShortestImportableName(element.containingFile.virtualFile, element)
                val qualifiedName = element.qualifiedName ?: "$parentQualifiedName.${element.name}"
                ArtifactQualifiedName("$qualifiedName()", null, ArtifactType.METHOD)
            }

            is PyStatement, is PyStatementList -> getStatementOrExpressionQualifiedName(element, ArtifactType.STATEMENT)
            else -> getStatementOrExpressionQualifiedName(element, ArtifactType.EXPRESSION)
        }
    }

    private fun getStatementOrExpressionQualifiedName(element: PsiElement, type: ArtifactType): ArtifactQualifiedName {
        val parentFunction = element.parentOfType<PyFunction>()
        return if (parentFunction != null) {
            val parentQualifiedName = PyPsiFacade.getInstance(element.project)
                .findShortestImportableName(element.containingFile.virtualFile, element)
            val qualifiedName = parentFunction.qualifiedName ?: "$parentQualifiedName.${parentFunction.name!!}"
            ArtifactQualifiedName("$qualifiedName()", null, type)
        } else {
            val qName = PyPsiFacade.getInstance(element.project)
                .findShortestImportableName(element.containingFile.virtualFile, element)
            ArtifactQualifiedName("$qName", null, type)
        }
    }

    override fun getQualifiedClassNames(psiFile: PsiFile): List<ArtifactQualifiedName> {
        val classQualifiedNames = mutableListOf<ArtifactQualifiedName>()
        psiFile.acceptChildren(object : PyRecursiveElementVisitor() {
            override fun visitPyClass(node: PyClass) {
                super.visitPyClass(node)
                node.qualifiedName?.let {
                    classQualifiedNames.add(ArtifactQualifiedName(it, type = ArtifactType.CLASS))
                }
            }
        })
        return classQualifiedNames
    }
}
