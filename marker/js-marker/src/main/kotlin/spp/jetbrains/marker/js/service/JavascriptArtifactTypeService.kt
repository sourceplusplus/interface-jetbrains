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

import com.intellij.lang.javascript.psi.JSExpression
import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.lang.javascript.psi.ecmal4.JSClass
import com.intellij.psi.PsiElement
import spp.jetbrains.artifact.service.define.IArtifactTypeService
import spp.protocol.artifact.ArtifactType

/**
 * Used to determine the type of JavaScript artifacts.
 *
 * @since 0.7.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class JavascriptArtifactTypeService : IArtifactTypeService {

    override fun getAnnotations(element: PsiElement): List<PsiElement> {
        return emptyList() //todo: implement
    }

    override fun getAnnotationOwnerIfAnnotation(element: PsiElement): PsiElement? {
        return null //todo: implement
    }

    override fun getAnnotationOwnerIfAnnotation(element: PsiElement, line: Int): PsiElement? {
        return null //todo: implement
    }

    override fun isComment(element: PsiElement): Boolean = false

    override fun getType(element: PsiElement): ArtifactType? {
        return when (element) {
            is JSClass -> ArtifactType.CLASS
            is JSFunction -> ArtifactType.FUNCTION
            is JSExpression -> ArtifactType.EXPRESSION

            else -> null
        }
    }
}
