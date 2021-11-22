package spp.jetbrains.marker.py

import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.treeStructure.SimpleNode
import com.intellij.xdebugger.impl.ui.DebuggerUIUtil
import spp.protocol.instrument.LiveVariable
import spp.protocol.instrument.LiveVariableScope

class PythonVariableRootNode(val variables: List<LiveVariable>, val scope: LiveVariableScope) : SimpleNode() {

    private val scheme = DebuggerUIUtil.getColorScheme(null)

    override fun getChildren(): Array<SimpleNode> {
        return variables.map { PythonVariableSimpleNode(it) }.toTypedArray()
    }

    override fun update(presentation: PresentationData) {
        presentation.addText(
            let { if (scope == LiveVariableScope.GLOBAL_VARIABLE) "Global" else "Local" },
            SimpleTextAttributes.fromTextAttributes(scheme.getAttributes(DefaultLanguageHighlighterColors.IDENTIFIER))
        )
    }
}
