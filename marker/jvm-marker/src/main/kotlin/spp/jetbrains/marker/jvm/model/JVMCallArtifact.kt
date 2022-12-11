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
package spp.jetbrains.marker.jvm.model

import com.intellij.psi.PsiCall
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.nj2k.postProcessing.resolve
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import spp.jetbrains.marker.model.CallArtifact
import spp.jetbrains.marker.model.FunctionArtifact
import spp.jetbrains.marker.service.toArtifact

class JVMCallArtifact(private val psiElement: PsiElement) : CallArtifact(psiElement) {

    override fun resolveMethod(): FunctionArtifact? {
        return when (psiElement) {
            is PsiCall -> psiElement.resolveMethod()?.toArtifact() as? FunctionArtifact
            is KtCallExpression -> {
                (psiElement.calleeExpression as KtNameReferenceExpression).resolve()
                    ?.toArtifact() as? FunctionArtifact
            }

            else -> TODO()
        }
    }
}
