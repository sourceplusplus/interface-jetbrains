package spp.jetbrains.sourcemarker.service.breakpoint.tree

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.ui.treeStructure.SimpleNode
import com.intellij.util.containers.hash.LinkedHashMap
import spp.jetbrains.marker.jvm.JVMVariableSimpleNode
import spp.jetbrains.marker.py.PythonVariableSimpleNode
import spp.jetbrains.sourcemarker.activities.PluginSourceMarkerStartupActivity.Companion.PYCHARM_PRODUCT_CODES
import spp.jetbrains.sourcemarker.service.breakpoint.StackFrameManager

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
            val productCode = ApplicationInfo.getInstance().build.productCode
            val vars = stackFrameManager.currentFrame!!.variables
            val simpleNodeMap: MutableMap<String, SimpleNode> = LinkedHashMap()
            vars.forEach {
                if (it.name.isNotEmpty()) {
                    if (PYCHARM_PRODUCT_CODES.contains(productCode)) {
                        simpleNodeMap[it.name] = PythonVariableSimpleNode(it)
                    } else {
                        simpleNodeMap[it.name] = JVMVariableSimpleNode(it)
                    }
                }
            }

            if (PYCHARM_PRODUCT_CODES.contains(productCode)) {
                return simpleNodeMap.values.toTypedArray()
            } else {
                (simpleNodeMap.values as Collection<JVMVariableSimpleNode>).sortedWith { p0, p1 ->
                    when {
                        p0.variable.name == "this" -> -1
                        p1.variable.name == "this" -> 1
                        else -> 0
                    }
                }.toTypedArray()
            }
        }
    }
}
