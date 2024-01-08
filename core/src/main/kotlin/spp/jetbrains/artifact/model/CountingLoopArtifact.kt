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
package spp.jetbrains.artifact.model

import com.intellij.psi.PsiElement
import spp.jetbrains.artifact.service.toArtifact

abstract class CountingLoopArtifact(psiElement: PsiElement) : ForLoopArtifact(psiElement) {

    /**
     * @return loop counter variable
     */
    abstract fun getCounter(): PsiElement

    /**
     * @return counter variable initial value
     */
    abstract fun getInitializer(): PsiElement

    /**
     * @return loop bound
     */
    abstract fun getBound(): PsiElement

    /**
     * @return true if bound is including
     */
    abstract fun isIncluding(): Boolean

    /**
     * @return true if the loop is descending
     */
    abstract fun isDescending(): Boolean

    /**
     * @return true if the loop variable may experience integer overflow before reaching the bound,
     * like for(int i = 10; i != -10; i++) will go through MAX_VALUE and MIN_VALUE.
     */
    abstract fun mayOverflow(): Boolean

    fun getRepetitionCount(): Long? {
        val initializer = getInitializer().toArtifact()
        val bound = getBound().toArtifact()

        if (initializer is ArtifactLiteralValue && bound is ArtifactLiteralValue) {
            val initValue = initializer.value?.toString()?.toLongOrNull()
            val boundValue = bound.value?.toString()?.toLongOrNull()
            if (initValue != null && boundValue != null) {
                val diff = boundValue - initValue
                return if (isIncluding()) diff + 1 else diff
            }
        }
        return null
    }
}
