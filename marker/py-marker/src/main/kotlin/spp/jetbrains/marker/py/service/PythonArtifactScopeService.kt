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
package spp.jetbrains.marker.py.service

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
import com.jetbrains.python.codeInsight.controlflow.ControlFlowCache
import com.jetbrains.python.codeInsight.dataflow.scope.ScopeUtil
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.PyIfStatement
import com.jetbrains.python.psi.PyReferenceExpression
import spp.jetbrains.marker.SourceMarkerUtils
import spp.jetbrains.marker.service.ArtifactTypeService
import spp.jetbrains.marker.service.define.IArtifactScopeService
import spp.jetbrains.marker.source.SourceFileMarker

/**
 * Used to determine the scope of Python artifacts.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("TooManyFunctions")
class PythonArtifactScopeService : IArtifactScopeService {

    override fun getFunctions(element: PsiFile): List<PsiNamedElement> {
        require(ArtifactTypeService.isPython(element))
        return element.descendantsOfType<PyFunction>().toList()
    }

    override fun getChildIfs(element: PsiElement): List<PsiElement> {
        require(ArtifactTypeService.isPython(element))
        return element.descendantsOfType<PyIfStatement>().toList()
    }

    override fun getParentIf(element: PsiElement): PsiElement? {
        require(ArtifactTypeService.isPython(element))
        return element.findParentOfType<PyIfStatement>()
    }

    override fun getParentFunction(element: PsiElement): PsiNamedElement? {
        require(ArtifactTypeService.isPython(element))
        return element.findParentOfType<PyFunction>()
    }

    override fun getCalls(element: PsiElement): List<PsiElement> {
        require(ArtifactTypeService.isPython(element))
        return element.descendantsOfType<PyCallExpression>().toList()
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
                it.element.parentOfType<PyFunction>()
            }.filter { it.isWritable }
        })
    }

    override fun getScopeVariables(fileMarker: SourceFileMarker, lineNumber: Int): List<String> {
        val position = SourceMarkerUtils.getElementAtLine(fileMarker.psiFile, lineNumber) ?: return emptyList()
        val scope = ScopeUtil.getScopeOwner(position) ?: return emptyList()
        return ControlFlowCache.getScope(scope).namedElements.mapNotNull { it.name }
    }

    override fun isInsideFunction(element: PsiElement): Boolean {
        return element.parentOfTypes(PyFunction::class) != null
    }

    override fun isInsideEndlessLoop(element: PsiElement): Boolean {
        return false
    }

    private fun getResolvedCalls(element: PsiElement): Sequence<PyFunction> {
        return element.descendantsOfType<PyCallExpression>()
            .mapNotNull { it.callee }
            .mapNotNull { it as? PyReferenceExpression }
            .mapNotNull { it.reference.resolve() }
            .mapNotNull { it as? PyFunction }
    }
}
