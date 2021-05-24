package com.sourceplusplus.sourcemarker.service.breakpoint.tree

import com.intellij.ui.treeStructure.SimpleTreeStructure
import com.sourceplusplus.sourcemarker.service.breakpoint.StackFrameManager

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class VariableSimpleTreeStructure : SimpleTreeStructure() {

    private val simpleRoot = VariableRootSimpleNode()

    override fun getRootElement(): VariableRootSimpleNode {
        return simpleRoot
    }

    fun setStackFrameManager(stackFrameManager: StackFrameManager) {
        simpleRoot.setStackFrameManager(stackFrameManager)
    }
}
