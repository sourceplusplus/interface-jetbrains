/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2024 CodeBrig, Inc.
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
package spp.jetbrains.marker.rs.service

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.rust.lang.core.psi.RsExpr
import org.rust.lang.core.psi.RsFunction
import spp.jetbrains.artifact.service.define.IArtifactTypeService
import spp.protocol.artifact.ArtifactType

/**
 * Used to determine the type of Rust artifacts.
 *
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class RustArtifactTypeService : IArtifactTypeService {

    override fun getAnnotations(element: PsiElement): List<PsiElement> {
        return emptyList() //todo: implement
    }

    override fun getAnnotationOwnerIfAnnotation(element: PsiElement): PsiElement? {
        return null //todo: implement
    }

    override fun getAnnotationOwnerIfAnnotation(element: PsiElement, line: Int): PsiElement? {
        return null //todo: implement
    }

    override fun isComment(element: PsiElement): Boolean {
        val comment = element is PsiComment
        if (comment) return true

        return if (element is LeafPsiElement) {
            isComment(element.parent)
        } else false
    }

    override fun getType(element: PsiElement): ArtifactType? {
        return when (element) {
            is RsFunction -> ArtifactType.FUNCTION
            is RsExpr -> ArtifactType.EXPRESSION

            else -> null
        }
    }

    override fun isLiteral(element: PsiElement): Boolean {
        return super.isLiteral(element) //todo: implement
    }
}
