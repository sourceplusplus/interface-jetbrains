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
                classQualifiedNames.add(ArtifactQualifiedName(node.qualifiedName!!, type = ArtifactType.CLASS))
            }
        })
        return classQualifiedNames
    }
}
