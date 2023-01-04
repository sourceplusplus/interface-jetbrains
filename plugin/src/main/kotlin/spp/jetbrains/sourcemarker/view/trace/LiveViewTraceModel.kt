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
package spp.jetbrains.sourcemarker.view.trace

import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.tree.TreeVisitor
import com.intellij.ui.treeStructure.treetable.TreeTableModel
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.SortableColumnModel
import com.intellij.util.ui.tree.AbstractTreeModel
import org.jetbrains.concurrency.Promise
import spp.jetbrains.sourcemarker.view.trace.column.TraceSpanTreeNodeColumnInfo
import spp.jetbrains.sourcemarker.view.trace.node.TraceSpanTreeNode
import javax.swing.JTree
import javax.swing.RowSorter
import javax.swing.SortOrder
import javax.swing.event.TreeModelEvent
import javax.swing.event.TreeModelListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LiveViewTraceModel(
    structure: LiveViewTraceTreeStructure
) : AbstractTreeModel(), TreeTableModel, SortableColumnModel, TreeModelListener, TreeVisitor.Acceptor {

    companion object {
        val COLUMN_INFOS: Array<ColumnInfo<TraceSpanTreeNode, String>> = arrayOf(
            TraceSpanTreeNodeColumnInfo("Trace"),
            TraceSpanTreeNodeColumnInfo("Type"),
            TraceSpanTreeNodeColumnInfo("Duration"),
            TraceSpanTreeNodeColumnInfo("Time"),
        )
    }

    private val myAsyncModel: AsyncTreeModel //todo: likely don't need async model
    private var myStructureModel: StructureTreeModel<LiveViewTraceTreeStructure>
    private var myTree: JTree? = null

    init {
        myStructureModel = StructureTreeModel(structure, this)
        myAsyncModel = AsyncTreeModel(myStructureModel, true, this)
        myAsyncModel.addTreeModelListener(this)
    }

    fun setComparator(comparator: Comparator<in NodeDescriptor<*>?>?) {
        myStructureModel.setComparator(comparator)
    }

    override fun getRoot(): Any? = myAsyncModel.root

    override fun getChild(parent: Any?, index: Int): Any? {
        return myAsyncModel.getChild(parent, index)
    }

    override fun getChildCount(parent: Any?): Int {
        return myAsyncModel.getChildCount(parent)
    }

    override fun isLeaf(node: Any?): Boolean {
        return myAsyncModel.isLeaf(node)
    }

    override fun getIndexOfChild(parent: Any?, child: Any?): Int {
        return myAsyncModel.getIndexOfChild(parent, child)
    }

    override fun getColumnCount(): Int = COLUMN_INFOS.size
    override fun getColumnName(column: Int): String = COLUMN_INFOS[column].name

    override fun getColumnClass(column: Int): Class<*>? {
        return if (column == 0) TreeTableModel::class.java else COLUMN_INFOS[column].columnClass
    }

    override fun getValueAt(node: Any?, column: Int): Any? {
        val traceSpanTreeNode = getNode(node)
        return if (traceSpanTreeNode != null) {
            COLUMN_INFOS[column].valueOf(traceSpanTreeNode)
        } else ""
    }

    private fun getNode(node: Any?): TraceSpanTreeNode? {
        if (node is DefaultMutableTreeNode) {
            val userObject = node.userObject
            if (userObject is TraceSpanTreeNode) {
                return userObject
            }
        }
        return null
    }

    override fun isCellEditable(node: Any?, column: Int): Boolean = false

    override fun setValueAt(aValue: Any?, node: Any?, column: Int) = Unit

    override fun setTree(tree: JTree?) {
        myTree = tree
    }

    override fun treeNodesChanged(e: TreeModelEvent) = treeNodesChanged(e.treePath, e.childIndices, e.children)
    override fun treeNodesInserted(e: TreeModelEvent) = treeNodesInserted(e.treePath, e.childIndices, e.children)
    override fun treeNodesRemoved(e: TreeModelEvent) = treeNodesRemoved(e.treePath, e.childIndices, e.children)
    override fun treeStructureChanged(e: TreeModelEvent) {
        treeStructureChanged(e.treePath, e.childIndices, e.children)

        //todo: pretty sure this is wrong
        //auto-select first row
        ApplicationManager.getApplication().invokeLater {
            val node = (e.treePath.path[0] as DefaultMutableTreeNode)
            if (node.childCount == 0) return@invokeLater
            myTree?.selectionPath = e.treePath.pathByAddingChild(node.firstChild)
        }
    }

    override fun getColumnInfos(): Array<ColumnInfo<TraceSpanTreeNode, String>> = COLUMN_INFOS
    override fun setSortable(aBoolean: Boolean) = Unit
    override fun isSortable(): Boolean = true

    override fun getRowValue(row: Int): Any? {
        if (myTree == null) return null
        val path = myTree!!.getPathForRow(row) ?: return null
        return path.lastPathComponent
    }

    override fun getDefaultSortKey(): RowSorter.SortKey {
        return RowSorter.SortKey(COLUMN_INFOS.indexOfFirst { it.name == "Time" }, SortOrder.DESCENDING)
    }

    override fun accept(visitor: TreeVisitor): Promise<TreePath> = myAsyncModel.accept(visitor)
}
