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
package spp.jetbrains.marker.js.service

import com.intellij.lang.javascript.psi.*
import com.intellij.lang.javascript.psi.ecmal4.JSClass
import com.intellij.lang.javascript.psi.util.JSTreeUtil
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.descendantsOfType
import com.intellij.psi.util.findParentOfType
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.parentOfTypes
import spp.jetbrains.artifact.service.ArtifactTypeService
import spp.jetbrains.artifact.service.define.IArtifactScopeService
import spp.jetbrains.marker.SourceMarkerUtils

/**
 * Used to determine the scope of JavaScript artifacts.
 *
 * @since 0.7.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("TooManyFunctions") // public API
class JavascriptArtifactScopeService : IArtifactScopeService {

    override fun getFunctions(element: PsiFile): List<PsiNamedElement> {
        require(ArtifactTypeService.isJavaScript(element))
        return element.descendantsOfType<JSFunction>().toList()
    }

    override fun getChildIfs(element: PsiElement): List<PsiElement> {
        require(ArtifactTypeService.isJavaScript(element))
        return element.descendantsOfType<JSIfStatement>().toList()
    }

    override fun getParentIf(element: PsiElement): PsiElement? {
        require(ArtifactTypeService.isJavaScript(element))
        return element.findParentOfType<JSIfStatement>()
    }

    override fun getParentFunction(element: PsiElement): PsiNamedElement? {
        require(ArtifactTypeService.isJavaScript(element))
        return element.findParentOfType<JSFunction>()
    }

    override fun getParentClass(element: PsiElement): PsiNamedElement? {
        require(ArtifactTypeService.isJavaScript(element))
        return element.findParentOfType<JSClass>()
    }

    override fun getCalls(element: PsiElement): List<PsiElement> {
        require(ArtifactTypeService.isJavaScript(element))
        return element.descendantsOfType<JSCallExpression>().toList()
    }

    override fun getCalledFunctions(
        element: PsiElement,
        includeExternal: Boolean,
        includeIndirect: Boolean
    ): List<PsiNameIdentifierOwner> {
        if (includeIndirect) {
            val projectFileIndex = ProjectFileIndex.getInstance(element.project)
            return ReadAction.compute(ThrowableComputable {
                val calledFunctions = getResolvedCalls(element)
                val filteredFunctions = calledFunctions.filter {
                    includeExternal || projectFileIndex.isInSource(it.containingFile.virtualFile)
                }
                return@ThrowableComputable (filteredFunctions + filteredFunctions.flatMap {
                    getCalledFunctions(it, includeExternal, true)
                }).toList()
            })
        }

        return ReadAction.compute(ThrowableComputable {
            return@ThrowableComputable getResolvedCalls(element).toList()
        })
    }

    override fun getCallerFunctions(element: PsiElement, includeIndirect: Boolean): List<PsiNameIdentifierOwner> {
        val references = ProgressManager.getInstance().runProcess(Computable {
            ReferencesSearch.search(element, GlobalSearchScope.projectScope(element.project)).toList()
        }, EmptyProgressIndicator(ModalityState.defaultModalityState()))
        return ReadAction.compute(ThrowableComputable {
            references.mapNotNull {
                it.element.parentOfType<JSFunction>()
            }.filter { it.isWritable }
        })
    }

    override fun getScopeVariables(file: PsiFile, lineNumber: Int): List<String> {
        val vars = mutableListOf<JSVariable>()
        val position = SourceMarkerUtils.getElementAtLine(file, lineNumber)!!
        JSTreeUtil.processDeclarationsInScope(position, position, { element, _ ->
            if (element is JSVariable) {
                vars.add(element)
            }
            true
        }, null, true)
        return vars.mapNotNull { it.name }
    }

    override fun isInsideFunction(element: PsiElement): Boolean {
        return element.parentOfTypes(JSFunction::class) != null
    }

    private fun getResolvedCalls(element: PsiElement): Sequence<JSFunction> {
        return element.descendantsOfType<JSCallExpression>()
            .mapNotNull { it.methodExpression }
            .mapNotNull { it as? JSReferenceExpression }
            .mapNotNull { it.resolve() }
            .mapNotNull { it as? JSFunction }
    }
}
