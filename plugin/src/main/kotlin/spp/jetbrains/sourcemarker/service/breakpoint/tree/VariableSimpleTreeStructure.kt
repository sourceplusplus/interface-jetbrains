package spp.jetbrains.sourcemarker.service.breakpoint.tree

import com.intellij.ui.treeStructure.SimpleTreeStructure
import spp.jetbrains.sourcemarker.service.breakpoint.StackFrameManager

/**
 * todo: description.
 *
 * @since 0.3.0
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
