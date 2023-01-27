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

import com.intellij.psi.PsiConditionalLoopStatement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiLocalVariable
import com.intellij.psi.PsiLoopStatement
import com.siyeh.ig.psiutils.CountingLoop
import spp.jetbrains.artifact.model.ArtifactElement
import spp.jetbrains.artifact.model.CountingLoopArtifact
import spp.jetbrains.artifact.service.toArtifact

class JVMCountingLoop(private val loop: CountingLoop) : CountingLoopArtifact(loop.loop) {

    override val childArtifacts: MutableList<ArtifactElement> = mutableListOf()
    override val condition: ArtifactElement? = when (psiElement) {
        is PsiConditionalLoopStatement -> psiElement.condition?.let { it.toArtifact() }
        else -> null
    }
    override val body: ArtifactElement? = when (psiElement) {
        is PsiLoopStatement -> psiElement.body?.let { it.toArtifact() }
        else -> null
    }

    override fun getCounter(): PsiLocalVariable = loop.counter
    override fun getInitializer(): PsiExpression = loop.initializer
    override fun getBound(): PsiExpression = loop.bound
    override fun isIncluding(): Boolean = loop.isIncluding
    override fun isDescending(): Boolean = loop.isDescending
    override fun mayOverflow(): Boolean = loop.mayOverflow()

    override fun clone(): ArtifactElement {
        return JVMCountingLoop(loop)
    }
}
