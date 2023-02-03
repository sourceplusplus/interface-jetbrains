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
package spp.jetbrains.marker.jvm.model

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiReferenceExpression
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtParameter
import spp.jetbrains.artifact.model.ArtifactElement
import spp.jetbrains.artifact.model.ReferenceArtifact
import spp.jetbrains.artifact.service.isFunction
import spp.jetbrains.artifact.service.toArtifact

class JVMReferenceArtifact(psiElement: PsiElement) : ReferenceArtifact(psiElement) {

    override fun isFunctionParameter(): Boolean {
        if (psiElement is PsiReferenceExpression) {
            val resolve = psiElement.resolve()
            if (resolve is PsiParameter) {
                return resolve.declarationScope.toArtifact()?.isFunction() ?: false
            }
        } else if (psiElement is KtNameReferenceExpression) {
            val resolve = psiElement.mainReference.resolve()
            if (resolve is KtParameter) {
                //todo: seems wrong (isFunctionTypeParameter looks like it should work but doesn't)
                val grandParent = resolve.parent.parent
                return grandParent.toArtifact()?.isFunction() ?: false
            }
        }
        return false
    }

    override fun toString(): String {
        return "JVMReferenceArtifact(functionParameter=${isFunctionParameter()})"
    }

    override fun clone(): ArtifactElement {
        return JVMReferenceArtifact(psiElement)
    }
}
