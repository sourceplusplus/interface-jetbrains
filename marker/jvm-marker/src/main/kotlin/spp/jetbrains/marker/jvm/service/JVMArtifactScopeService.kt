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
package spp.jetbrains.marker.jvm.service

import com.intellij.codeEditor.JavaEditorFileSwapper
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.scope.processor.VariablesProcessor
import com.intellij.psi.scope.util.PsiScopesUtil
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.*
import com.siyeh.ig.psiutils.ControlFlowUtils
import org.jetbrains.kotlin.backend.jvm.ir.psiElement
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.psi.*
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrCall
import org.joor.Reflect
import spp.jetbrains.artifact.service.ArtifactTypeService
import spp.jetbrains.artifact.service.define.IArtifactScopeService
import spp.jetbrains.marker.SourceMarkerUtils

/**
 * Used to determine the scope of JVM artifacts.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("TooManyFunctions") // public API
class JVMArtifactScopeService : IArtifactScopeService {

    override fun getFunctions(element: PsiFile): List<PsiNamedElement> {
        return when {
            ArtifactTypeService.isJava(element) -> element.descendantsOfType<PsiMethod>().toList()
            ArtifactTypeService.isKotlin(element) -> element.descendantsOfType<KtNamedFunction>().toList()
            else -> throw IllegalArgumentException("Unsupported language: ${element.language}")
        }
    }

    override fun getChildIfs(element: PsiElement): List<PsiElement> {
        return when {
            ArtifactTypeService.isJava(element) -> element.descendantsOfType<PsiIfStatement>().toList()
            ArtifactTypeService.isKotlin(element) -> element.descendantsOfType<KtIfExpression>().toList()
            else -> throw IllegalArgumentException("Unsupported language: ${element.language}")
        }
    }

    override fun getParentIf(element: PsiElement): PsiElement? {
        return when {
            ArtifactTypeService.isJava(element) -> element.findParentOfType<PsiIfStatement>()
            ArtifactTypeService.isKotlin(element) -> element.findParentOfType<KtIfExpression>()
            else -> throw IllegalArgumentException("Unsupported language: ${element.language}")
        }
    }

    override fun getParentFunction(element: PsiElement): PsiNamedElement? {
        return when {
            ArtifactTypeService.isJava(element) -> element.findParentOfType<PsiMethod>()
            ArtifactTypeService.isKotlin(element) -> element.findParentOfType<KtNamedFunction>()
            else -> throw IllegalArgumentException("Unsupported language: ${element.language}")
        }
    }

    override fun getCalls(element: PsiElement): List<PsiElement> {
        return when {
            ArtifactTypeService.isJava(element) -> element.descendantsOfType<PsiCallExpression>().toList()
            ArtifactTypeService.isKotlin(element) -> element.descendantsOfType<KtCallExpression>().toList()
            else -> throw IllegalArgumentException("Unsupported language: ${element.language}")
        }
    }

    override fun getCalledFunctions(
        element: PsiElement,
        includeExternal: Boolean,
        includeIndirect: Boolean
    ): List<PsiNameIdentifierOwner> {
        if (includeIndirect) {
            return DumbService.getInstance(element.project).runReadActionInSmartMode(Computable {
                val calledFunctions = getResolvedCalls(element)
                val filteredFunctions = calledFunctions.filter { includeExternal || it.isWritable }
                return@Computable (filteredFunctions + filteredFunctions.flatMap {
                    getCalledFunctions(it, includeExternal, true)
                }).toList()
            })
        }

        return DumbService.getInstance(element.project).runReadActionInSmartMode(Computable {
            return@Computable getResolvedCalls(element).filter { includeExternal || it.isWritable }.toList()
        })
    }

    override fun getCallerFunctions(element: PsiElement, includeIndirect: Boolean): List<PsiNameIdentifierOwner> {
        val references = ProgressManager.getInstance().runProcess(Computable {
            if (ApplicationManager.getApplication().isReadAccessAllowed) {
                ReferencesSearch.search(element, GlobalSearchScope.projectScope(element.project)).toList()
            } else {
                DumbService.getInstance(element.project).runReadActionInSmartMode(Computable {
                    ReferencesSearch.search(element, GlobalSearchScope.projectScope(element.project)).toList()
                })
            }
        }, EmptyProgressIndicator(ModalityState.defaultModalityState()))
        return ReadAction.compute(ThrowableComputable {
            references.mapNotNull {
                if (ArtifactTypeService.isKotlin(element)) {
                    it.element.parentOfType<KtNamedFunction>()
                } else {
                    it.element.parentOfType<PsiMethod>()
                }
            }.filter { it.isWritable() }
        })
    }

    override fun getScopeVariables(file: PsiFile, lineNumber: Int): List<String> {
        //skip blank lines (if present) till valid minimum scope
        var checkLine = lineNumber
        var minScope: PsiElement? = null
        while (minScope == null) {
            minScope = SourceMarkerUtils.getElementAtLine(file, --checkLine)
        }

        return if (file is KtFile) {
            getScopeVariables(minScope)
        } else {
            val scopeVars = mutableListOf<String>()
            val variablesProcessor: VariablesProcessor = object : VariablesProcessor(false) {
                override fun check(`var`: PsiVariable, state: ResolveState): Boolean = true
            }
            PsiScopesUtil.treeWalkUp(variablesProcessor, minScope, null)
            for (i in 0 until variablesProcessor.size()) {
                scopeVars.add(variablesProcessor.getResult(i).name!!)
            }
            scopeVars
        }
    }

    private fun getScopeVariables(minScope: PsiElement): List<String> {
        val scopeVars = mutableListOf<String>()
        minScope.parents(true).filterIsInstance<KtBlockExpression>().forEach {
            it.statements.filterIsInstance<KtProperty>().forEach {
                if (it.textRange.startOffset <= minScope.textRange.startOffset) {
                    it.name?.let { scopeVars.add(it) }
                }
            }
        }
        minScope.parents(true).filterIsInstance<KtCatchClause>().forEach {
            it.parameterList?.parameters?.forEach {
                it.name?.let { scopeVars.add(it) }
            }
        }
        return scopeVars
    }

    override fun isInsideFunction(element: PsiElement): Boolean {
        if (ArtifactTypeService.isKotlin(element)) {
            return element.parentOfTypes(KtNamedFunction::class) != null
        }
        return element.parentOfTypes(PsiMethod::class) != null
    }

    override fun isInsideEndlessLoop(element: PsiElement): Boolean {
        val parentLoop = element.parentOfTypes(PsiConditionalLoopStatement::class)
        return parentLoop != null && ControlFlowUtils.isEndlessLoop(parentLoop)
    }

    override fun canShowControlBar(element: PsiElement): Boolean {
        return when (element::class.java.name) {
            "org.jetbrains.kotlin.psi.KtObjectDeclaration" -> false
            "org.jetbrains.kotlin.psi.KtProperty" -> {
                Reflect.on(element).call("isLocal").get<Boolean>() == true
            }

            else -> super.canShowControlBar(element)
        }
    }

    override fun findSourceFile(element: PsiFile): VirtualFile? {
        return JavaEditorFileSwapper.findSourceFile(element.project, element.virtualFile) ?: element.virtualFile
    }

    private fun getResolvedCalls(element: PsiElement): Sequence<PsiNameIdentifierOwner> {
        return when {
            ArtifactTypeService.isGroovy(element) -> element.descendantsOfType<GrCall>().map {
                it.resolveMethod()
            }.filterNotNull()

            ArtifactTypeService.isKotlin(element) -> element.descendantsOfType<KtCallExpression>().map {
                it.resolveToCall()?.candidateDescriptor?.psiElement as? PsiNameIdentifierOwner
            }.filterNotNull()

            else -> element.descendantsOfType<PsiCall>().map { call ->
                call.resolveMethod()
            }.filterNotNull()
        }
    }
}
