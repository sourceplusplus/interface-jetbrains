package spp.jetbrains.sourcemarker.service.breakpoint.tree

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.ui.treeStructure.SimpleNode
import com.intellij.util.containers.hash.LinkedHashMap
import spp.jetbrains.marker.jvm.JVMVariableSimpleNode
import spp.jetbrains.marker.py.PythonVariableRootNode
import spp.jetbrains.sourcemarker.activities.PluginSourceMarkerStartupActivity.Companion.PYCHARM_PRODUCT_CODES
import spp.jetbrains.sourcemarker.service.breakpoint.StackFrameManager
import spp.protocol.instrument.LiveVariableScope

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
            val productCode = ApplicationInfo.getInstance().build.productCode
            if (PYCHARM_PRODUCT_CODES.contains(productCode)) {
                return arrayOf(
                    PythonVariableRootNode(
                        vars.filter { it.scope == LiveVariableScope.GLOBAL_VARIABLE },
                        LiveVariableScope.GLOBAL_VARIABLE
                    ),
                    PythonVariableRootNode(
                        vars.filter { it.scope == LiveVariableScope.LOCAL_VARIABLE },
                        LiveVariableScope.LOCAL_VARIABLE
                    )
                )
            } else {
                val simpleNodeMap: MutableMap<String, SimpleNode> = LinkedHashMap()
                vars.forEach {
                    if (it.name.isNotEmpty()) {
                        simpleNodeMap[it.name] = JVMVariableSimpleNode(it)
                    }
                }
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
