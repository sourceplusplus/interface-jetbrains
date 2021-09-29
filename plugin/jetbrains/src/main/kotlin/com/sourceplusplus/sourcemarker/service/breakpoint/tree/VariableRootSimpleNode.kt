package com.sourceplusplus.sourcemarker.service.breakpoint.tree

import com.intellij.ui.treeStructure.SimpleNode
import com.intellij.util.containers.hash.LinkedHashMap
import com.sourceplusplus.sourcemarker.service.breakpoint.StackFrameManager

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class VariableRootSimpleNode : SimpleNode() {

    private lateinit var stackFrameManager: StackFrameManager

    fun setStackFrameManager(currentStackFrameManager: StackFrameManager) {
        stackFrameManager = currentStackFrameManager
    }

    override fun getChildren(): Array<SimpleNode> {
        if (!this::stackFrameManager.isInitialized) {
            return emptyArray() //wait till initialized
        }

        return if (stackFrameManager.currentFrame?.variables.isNullOrEmpty()) {
            NO_CHILDREN
        } else {
            val vars = stackFrameManager.currentFrame!!.variables
            val simpleNodeMap: MutableMap<String, VariableSimpleNode> = LinkedHashMap()
            vars.forEach {
                if (it.name.isNotEmpty()) {
                    simpleNodeMap[it.name] = VariableSimpleNode(it)
                }
            }
            simpleNodeMap.values.sortedWith { p0, p1 ->
                when {
                    p0.variable.name == "this" -> -1
                    p1.variable.name == "this" -> 1
                    else -> 0
                }
            }.toTypedArray()
        }
    }
}
