/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package spp.jetbrains.sourcemarker.service.instrument.breakpoint.ui

import com.intellij.openapi.Disposable
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.treeStructure.Tree
import spp.jetbrains.sourcemarker.service.instrument.breakpoint.DebugStackFrameListener
import spp.jetbrains.sourcemarker.service.instrument.breakpoint.StackFrameManager
import spp.jetbrains.sourcemarker.service.instrument.breakpoint.tree.VariableSimpleTreeStructure
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

    override fun dispose() = Unit
}
