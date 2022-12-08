/*
 * Source++, the continuous feedback platform for developers.
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
package spp.jetbrains.marker.jvm.service

import com.intellij.psi.*
import com.intellij.psi.impl.light.JavaIdentifier
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.javadoc.PsiDocToken
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunction
import spp.jetbrains.marker.SourceMarkerUtils.getLineNumber
import spp.jetbrains.marker.service.ArtifactTypeService
import spp.jetbrains.marker.service.define.IArtifactTypeService
import spp.protocol.artifact.ArtifactType

/**
 * Used to determine the type of JVM artifacts.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class JVMArtifactTypeService : IArtifactTypeService {

    override fun getNameIdentifier(element: PsiElement): PsiElement {
        return if (element is JavaIdentifier) {
            element.navigationElement
        } else element
    }

    override fun getAnnotationOwnerIfAnnotation(element: PsiElement, line: Int): PsiElement? {
        val annotation = element.parentOfType<PsiAnnotation>()
        if (annotation != null && getLineNumber(annotation) == line) {
            if (annotation.owner is PsiModifierList) {
                return (annotation.owner as PsiModifierList).parent
            }
        }
        return null
    }

    override fun isComment(element: PsiElement): Boolean {
        val comment = element is PsiDocToken || element is PsiComment
        if (comment) return true

        return if (element is LeafPsiElement) {
            isComment(element.parent)
        } else false
    }

    override fun getType(element: PsiElement): ArtifactType? {
        return when {
            element is PsiClass -> ArtifactType.CLASS
            element is PsiMethod -> ArtifactType.METHOD
            element is PsiExpression -> ArtifactType.EXPRESSION
            ArtifactTypeService.isKotlin(element) && element is KtClass -> ArtifactType.CLASS
            ArtifactTypeService.isKotlin(element) && element is KtFunction -> ArtifactType.METHOD
            ArtifactTypeService.isKotlin(element) && element is KtExpression -> ArtifactType.EXPRESSION
            else -> null
        }
    }
}
