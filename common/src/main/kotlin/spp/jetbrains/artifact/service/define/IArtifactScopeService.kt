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
package spp.jetbrains.artifact.service.define

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiNamedElement

/**
 * todo: description.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("TooManyFunctions") // public API
interface IArtifactScopeService : ISourceMarkerService {

    fun getLoops(element: PsiElement): List<PsiElement>
    fun getFunctions(element: PsiElement): List<PsiNamedElement>
    fun getClasses(element: PsiElement): List<PsiNamedElement>
    fun getChildIfs(element: PsiElement): List<PsiElement>
    fun getParentIf(element: PsiElement): PsiElement?
    fun getParentFunction(element: PsiElement): PsiNamedElement?
    fun getParentClass(element: PsiElement): PsiNamedElement?
    fun getCalls(element: PsiElement): List<PsiElement>
    fun tryResolveCall(element: PsiElement): PsiElement?

    fun getCalledFunctions(
        element: PsiElement,
        includeExternal: Boolean = false,
        includeIndirect: Boolean = false
    ): List<PsiNameIdentifierOwner>

    fun getCallerFunctions(element: PsiElement, includeIndirect: Boolean = false): List<PsiNameIdentifierOwner>
    fun getScopeVariables(file: PsiFile, lineNumber: Int): List<String>
    fun isInsideFunction(element: PsiElement): Boolean
    fun isInsideEndlessLoop(element: PsiElement): Boolean = false
    fun canShowControlBar(element: PsiElement): Boolean = true
    fun findSourceFile(element: PsiFile): VirtualFile? = element.virtualFile
}
