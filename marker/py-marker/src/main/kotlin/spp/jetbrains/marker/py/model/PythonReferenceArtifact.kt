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
package spp.jetbrains.marker.py.model

import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyParameter
import com.jetbrains.python.psi.PyParameterList
import com.jetbrains.python.psi.PyReferenceExpression
import spp.jetbrains.artifact.model.ArtifactElement
import spp.jetbrains.artifact.model.ReferenceArtifact
import spp.jetbrains.artifact.service.toArtifact

class PythonReferenceArtifact(override val psiElement: PyReferenceExpression) : ReferenceArtifact(psiElement) {

    private val resolvedElement: PsiElement? by lazy {
        psiElement.reference.resolve()
    }

    override fun isFunctionParameter(): Boolean {
        return resolvedElement is PyParameter
    }

    override fun getFunctionParameterIndex(): Int {
        if (!isFunctionParameter()) return -1
        val parameter = resolvedElement as PyParameter
        val parameterList = parameter.parent as? PyParameterList
        return parameterList?.parameters?.indexOf(parameter) ?: -1
    }

    override fun resolve(): ArtifactElement? {
        return resolvedElement?.toArtifact()
    }

    override fun clone(): ArtifactElement {
        return PythonReferenceArtifact(psiElement)
    }
}
