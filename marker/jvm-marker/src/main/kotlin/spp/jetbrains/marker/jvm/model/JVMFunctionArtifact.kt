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

import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.kotlin.psi.KtNamedFunction
import spp.jetbrains.artifact.model.BlockArtifact
import spp.jetbrains.artifact.model.FunctionArtifact
import spp.jetbrains.artifact.service.toArtifact

class JVMFunctionArtifact(override val psiElement: PsiNameIdentifierOwner) : FunctionArtifact(psiElement) {

    override val bodyBlock: BlockArtifact?
        get() {
            return when (psiElement) {
                is PsiMethod -> psiElement.body?.toArtifact() as? BlockArtifact
                is KtNamedFunction -> psiElement.bodyBlockExpression?.toArtifact() as? BlockArtifact
                else -> TODO()
            }
        }

    override fun clone(): JVMFunctionArtifact {
        return JVMFunctionArtifact(psiElement)
    }
}
