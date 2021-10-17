package com.sourceplusplus.marker.jvm

import com.intellij.psi.*
import com.intellij.psi.scope.processor.VariablesProcessor
import com.intellij.psi.scope.util.PsiScopesUtil
import com.sourceplusplus.marker.ArtifactScopeService
import com.sourceplusplus.marker.SourceMarkerUtils
import com.sourceplusplus.marker.source.SourceFileMarker

/**
 * todo: description.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class JVMArtifactScopeService : ArtifactScopeService {

    fun isMethodAtLine(psiFile: PsiFile, lineNumber: Int): Boolean {
        var el =  SourceMarkerUtils.getElementAtLine(psiFile, lineNumber)
        while (el is PsiKeyword || el is PsiModifierList) {
            el = el.parent
        }
        return el is PsiMethod
    }

    override fun getScopeVariables(fileMarker: SourceFileMarker, lineNumber: Int): List<String> {
        //determine available vars
        val scopeVars = mutableListOf<String>()
        val minScope = SourceMarkerUtils.getElementAtLine(fileMarker.psiFile, lineNumber - 1)!!
        val variablesProcessor: VariablesProcessor = object : VariablesProcessor(false) {
            override fun check(`var`: PsiVariable, state: ResolveState): Boolean = true
        }
        PsiScopesUtil.treeWalkUp(variablesProcessor, minScope, null)
        for (i in 0 until variablesProcessor.size()) {
            scopeVars.add(variablesProcessor.getResult(i).name!!)
        }
        return scopeVars
    }
}
