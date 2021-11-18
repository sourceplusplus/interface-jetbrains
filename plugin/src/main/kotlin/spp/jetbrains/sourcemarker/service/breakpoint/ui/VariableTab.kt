package spp.jetbrains.sourcemarker.service.breakpoint.ui

import com.intellij.openapi.Disposable
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.treeStructure.Tree
import spp.jetbrains.sourcemarker.service.breakpoint.DebugStackFrameListener
import spp.jetbrains.sourcemarker.service.breakpoint.StackFrameManager
import spp.jetbrains.sourcemarker.service.breakpoint.tree.VariableSimpleTreeStructure
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class VariableTab : DebugStackFrameListener, Disposable {

    val component: JPanel
    private val treeStructure: VariableSimpleTreeStructure = VariableSimpleTreeStructure()
    private val treeModel: StructureTreeModel<VariableSimpleTreeStructure> = StructureTreeModel(treeStructure, this)

    init {
        val tree: JTree = Tree(DefaultTreeModel(DefaultMutableTreeNode()))
        tree.model = AsyncTreeModel(treeModel, this)
        tree.isRootVisible = false
        component = JPanel(BorderLayout())
        component.add(JBScrollPane(tree), "Center")
    }

    override fun onChanged(stackFrameManager: StackFrameManager) {
        treeStructure.setStackFrameManager(stackFrameManager)
        treeModel.invalidate()
    }

    override fun dispose() {}
}
