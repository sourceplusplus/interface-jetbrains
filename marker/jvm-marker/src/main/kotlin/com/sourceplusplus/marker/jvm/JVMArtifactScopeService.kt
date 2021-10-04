package com.sourceplusplus.marker.jvm

import com.intellij.psi.PsiVariable
import com.intellij.psi.ResolveState
import com.intellij.psi.scope.processor.VariablesProcessor
import com.intellij.psi.scope.util.PsiScopesUtil
import com.sourceplusplus.marker.ArtifactScopeService
import com.sourceplusplus.marker.Utils
import com.sourceplusplus.marker.source.SourceFileMarker

class JVMArtifactScopeService : ArtifactScopeService {

    override fun getScopeVariables(fileMarker: SourceFileMarker, lineNumber: Int): List<String> {
        //determine available vars
        val scopeVars = mutableListOf<String>()
        val minScope = Utils.getElementAtLine(fileMarker.psiFile, lineNumber - 1)!!
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
