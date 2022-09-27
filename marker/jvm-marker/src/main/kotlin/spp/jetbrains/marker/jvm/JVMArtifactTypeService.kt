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
package spp.jetbrains.marker.jvm

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunction
import spp.jetbrains.marker.IArtifactTypeService
import spp.protocol.artifact.ArtifactType

class JVMArtifactTypeService : IArtifactTypeService {

    override fun getType(element: PsiElement): ArtifactType? {
        return when (element) {
            is PsiClass -> ArtifactType.CLASS
            is PsiMethod -> ArtifactType.METHOD
            is PsiExpression -> ArtifactType.EXPRESSION

            is KtClass -> ArtifactType.CLASS
            is KtFunction -> ArtifactType.METHOD
            is KtExpression -> ArtifactType.EXPRESSION

            else -> null
        }
    }
}
