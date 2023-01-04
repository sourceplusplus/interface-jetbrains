/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2023 CodeBrig, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package spp.jetbrains.sourcemarker.instrument.breakpoint.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Key
import com.intellij.ui.ClientProperty
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.tree.ui.DefaultTreeUI
import com.intellij.ui.treeStructure.Tree
import org.joor.Reflect
import spp.jetbrains.sourcemarker.instrument.breakpoint.DebugStackFrameListener
import spp.jetbrains.sourcemarker.instrument.breakpoint.StackFrameManager
import spp.jetbrains.sourcemarker.instrument.breakpoint.tree.VariableSimpleTreeStructure
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

        //todo: temporary fix for #575
        val autoExpandAllowedKey = Reflect.onClass(DefaultTreeUI::class.java)
            .get<Key<Boolean>>("AUTO_EXPAND_ALLOWED")
        ClientProperty.put(tree, autoExpandAllowedKey, false)
    }

    override fun onChanged(stackFrameManager: StackFrameManager) {
        treeStructure.setStackFrameManager(stackFrameManager)
        treeModel.invalidate()
    }

    override fun dispose() = Unit
}
