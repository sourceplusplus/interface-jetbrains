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
package spp.jetbrains.marker.js

import com.intellij.lang.javascript.psi.*
import com.intellij.lang.javascript.psi.ecmal4.JSClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.parentOfType
import spp.jetbrains.marker.AbstractArtifactNamingService
import spp.protocol.artifact.ArtifactQualifiedName
import spp.protocol.artifact.ArtifactType
import java.util.*

/**
 * todo: description.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class JavascriptArtifactNamingService : AbstractArtifactNamingService {

    override fun getVariableName(element: PsiElement): String? {
        return if (element is JSVariable) {
            element.name
        } else {
            null
        }
    }

    override fun getFullyQualifiedName(element: PsiElement): ArtifactQualifiedName {
        return when (element) {
            is JSClass -> {
                ArtifactQualifiedName(element.qualifiedName!!, null, ArtifactType.CLASS)
            }

            is JSFunction -> {
                val qualifiedName = element.qualifiedName
                    ?: element.name // TODO: Do something using the file name when the qualified name is null?
                ArtifactQualifiedName("$qualifiedName()", null, ArtifactType.METHOD)
            }

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

        val parentElement = element.parentOfType<JSNamedElement>()
        if (parentElement != null) {
            return ArtifactQualifiedName("${getFullyQualifiedName(parentElement)}.${name}", null, type)
        } else {
            return ArtifactQualifiedName("${element.containingFile.virtualFile.path}:${name}", null, type)
        }
    }

    override fun getQualifiedClassNames(psiFile: PsiFile): List<ArtifactQualifiedName> {
        val classQualifiedNames = mutableListOf<ArtifactQualifiedName>()
        psiFile.acceptChildren(object : JSRecursiveWalkingElementVisitor() {
            override fun visitJSClass(node: JSClass) { // TODO: check this also works for typescript classes, otherwise use visitTypescriptClass
                super.visitJSClass(node)
                classQualifiedNames.add(ArtifactQualifiedName(node.qualifiedName!!, type = ArtifactType.CLASS))
            }
        })
        return classQualifiedNames
    }
}
