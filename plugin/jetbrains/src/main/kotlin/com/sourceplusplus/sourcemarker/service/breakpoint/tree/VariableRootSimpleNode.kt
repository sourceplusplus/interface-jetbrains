package com.sourceplusplus.sourcemarker.service.breakpoint.tree

import com.intellij.ui.treeStructure.SimpleNode
import com.intellij.util.containers.hash.LinkedHashMap
import com.sourceplusplus.sourcemarker.service.breakpoint.StackFrameManager

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class VariableRootSimpleNode : SimpleNode() {

    private lateinit var stackFrameManager: StackFrameManager

    fun setStackFrameManager(currentStackFrameManager: StackFrameManager) {
        stackFrameManager = currentStackFrameManager
    }

    override fun getChildren(): Array<SimpleNode> {
        return if (stackFrameManager.currentFrame?.variables.isNullOrEmpty()) {
            NO_CHILDREN
        } else {
            val vars = stackFrameManager.currentFrame!!.variables
            val simpleNodeMap: MutableMap<String, VariableSimpleNode> = LinkedHashMap()
            vars.forEach {
                if (it.name.isNotEmpty()) {
                    simpleNodeMap[it.name] = VariableSimpleNode(it, true)
                }
            }
            simpleNodeMap.values.toTypedArray()
        }
    }
}
