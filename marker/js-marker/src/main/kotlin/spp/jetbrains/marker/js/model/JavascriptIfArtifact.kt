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
package spp.jetbrains.marker.js.model

import com.intellij.lang.javascript.psi.JSIfStatement
import spp.jetbrains.artifact.model.ArtifactElement
import spp.jetbrains.artifact.model.IfArtifact
import spp.jetbrains.artifact.service.toArtifact

class JavascriptIfArtifact(override val psiElement: JSIfStatement) : IfArtifact(psiElement) {

    override val condition: ArtifactElement?
        get() = psiElement.condition?.toArtifact()

    override val thenBranch: ArtifactElement?
        get() = psiElement.thenBranch?.toArtifact()

    override val elseBranch: ArtifactElement?
        get() = psiElement.elseBranch?.toArtifact()

    override fun clone(): JavascriptIfArtifact {
        return JavascriptIfArtifact(psiElement)
    }
}
