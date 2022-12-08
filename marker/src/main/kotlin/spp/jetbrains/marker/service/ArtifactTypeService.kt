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
package spp.jetbrains.marker.service

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import spp.jetbrains.marker.SourceMarkerUtils
import spp.jetbrains.marker.service.define.AbstractSourceMarkerService
import spp.jetbrains.marker.service.define.IArtifactTypeService
import spp.protocol.artifact.ArtifactType

object ArtifactTypeService : AbstractSourceMarkerService<IArtifactTypeService>(), IArtifactTypeService {

    override fun getNameIdentifier(element: PsiElement): PsiElement {
        return getService(element.language).getNameIdentifier(element)
    }

    override fun getAnnotationOwnerIfAnnotation(element: PsiElement, line: Int): PsiElement? {
        return getService(element.language).getAnnotationOwnerIfAnnotation(element, line)
    }

    override fun isComment(element: PsiElement): Boolean {
        //language-agnostic check
        if (element is PsiComment) return true

        //language-specific check
        return getService(element.language).isComment(element)
    }

    override fun getType(element: PsiElement): ArtifactType? {
        return getService(element.language).getType(element)
    }

    fun isFunction(element: PsiElement): Boolean {
        return getType(element) == ArtifactType.METHOD
    }

    fun isPython(element: PsiElement): Boolean {
        return element.language.id == "Python"
    }

    fun isJvm(element: PsiElement): Boolean {
        return SourceMarkerUtils.getJvmLanguages().contains(element.language.id)
    }

    fun isKotlin(element: PsiElement): Boolean {
        return element.language.id == "kotlin"
    }

    fun isGroovy(element: PsiElement): Boolean {
        return element.language.id == "Groovy"
    }

    fun isJavaScript(element: PsiElement): Boolean {
        return SourceMarkerUtils.getJavaScriptLanguages().contains(element.language.id)
    }
}
