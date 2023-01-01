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
package spp.jetbrains.artifact.service

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiNamedElement
import spp.jetbrains.artifact.service.define.AbstractSourceMarkerService
import spp.jetbrains.artifact.service.define.IArtifactScopeService
import spp.protocol.artifact.ArtifactQualifiedName
import spp.protocol.artifact.ArtifactType

/**
 * todo: description.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("MemberVisibilityCanBePrivate", "TooManyFunctions") // public API
object ArtifactScopeService : AbstractSourceMarkerService<IArtifactScopeService>(), IArtifactScopeService {

    override fun getFunctions(element: PsiFile): List<PsiNamedElement> {
        return getService(element.language).getFunctions(element)
    }

    override fun getChildIfs(element: PsiElement): List<PsiElement> {
        return getService(element.language).getChildIfs(element)
    }

    override fun getParentIf(element: PsiElement): PsiElement? {
        return getService(element.language).getParentIf(element)
    }

    override fun getParentFunction(element: PsiElement): PsiNamedElement? {
        return getService(element.language).getParentFunction(element)
    }

    override fun getCalls(element: PsiElement): List<PsiElement> {
        return getService(element.language).getCalls(element)
    }

    override fun getCalledFunctions(
        element: PsiElement,
        includeExternal: Boolean,
        includeIndirect: Boolean
    ): List<PsiNameIdentifierOwner> {
        return getService(element.language).getCalledFunctions(element, includeExternal, includeIndirect)
    }

    override fun getCallerFunctions(element: PsiElement, includeIndirect: Boolean): List<PsiNameIdentifierOwner> {
        return getService(element.language).getCallerFunctions(element, includeIndirect)
    }

    override fun getScopeVariables(file: PsiFile, lineNumber: Int): List<String> {
        return getService(file.language).getScopeVariables(file, lineNumber)
    }

    fun isOnFunction(qualifiedName: ArtifactQualifiedName): Boolean {
        return qualifiedName.type == ArtifactType.METHOD
    }

    override fun isInsideFunction(element: PsiElement): Boolean {
        return getService(element.language).isInsideFunction(element)
    }

    fun isOnOrInsideFunction(qualifiedName: ArtifactQualifiedName, element: PsiElement): Boolean {
        return isOnFunction(qualifiedName) || isInsideFunction(element)
    }

    override fun isInsideEndlessLoop(element: PsiElement): Boolean {
        return getService(element.language).isInsideEndlessLoop(element)
    }
}

// Extensions

fun PsiFile.getFunctions(): List<PsiNamedElement> {
    return ArtifactScopeService.getService(language).getFunctions(this)
}

fun PsiElement.getChildIfs(): List<PsiElement> {
    return ArtifactScopeService.getService(language).getChildIfs(this)
}

fun PsiElement.getParentIf(): PsiElement? {
    return ArtifactScopeService.getService(language).getParentIf(this)
}

fun PsiElement.getParentFunction(): PsiNamedElement? {
    return ArtifactScopeService.getService(language).getParentFunction(this)
}

fun PsiElement.getCalls(): List<PsiElement> {
    return ArtifactScopeService.getService(language).getCalls(this)
}
