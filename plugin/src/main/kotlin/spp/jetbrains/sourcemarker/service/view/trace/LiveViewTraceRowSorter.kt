/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022 CodeBrig, Inc.
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
package spp.jetbrains.sourcemarker.service.view.trace

import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.ui.components.JBTreeTable
import com.intellij.util.ui.ColumnInfo
import javax.swing.RowSorter
import javax.swing.SortOrder
import javax.swing.table.TableModel

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LiveViewTraceRowSorter(
    private val myTreeTable: JBTreeTable,
    private val myModel: LiveViewTraceModel
) : RowSorter<TableModel>() {

    private var mySortKey: SortKey? = null

    override fun getModel(): TableModel {
        return myTreeTable.table.model
    }

    override fun toggleSortOrder(column: Int) {
        val sortOrder =
            if (mySortKey != null && mySortKey!!.column == column && mySortKey!!.sortOrder == SortOrder.ASCENDING) {
                SortOrder.DESCENDING
            } else SortOrder.ASCENDING
        sortKeys = listOf(SortKey(column, sortOrder))
    }

    override fun convertRowIndexToModel(index: Int): Int {
        return index
    }

    override fun convertRowIndexToView(index: Int): Int {
        return index
    }

    override fun getSortKeys(): List<SortKey> {
        return if (mySortKey == null) {
            emptyList()
        } else listOf(
            mySortKey!!
        )
    }

    override fun setSortKeys(keys: List<SortKey>) {
        if (keys.isEmpty()) return
        val key = keys[0]
        if (key.sortOrder == SortOrder.UNSORTED) return
        mySortKey = key
        val columnInfo: ColumnInfo<*, *> = myModel.columnInfos[key.column]
        val comparator = columnInfo.comparator as Comparator<in NodeDescriptor<*>?>?
        if (comparator != null) {
            fireSortOrderChanged()
            myModel.setComparator(reverseComparator(comparator, key.sortOrder))
        }
    }

    override fun getViewRowCount(): Int {
        return myTreeTable.tree.rowCount
    }

    override fun getModelRowCount(): Int {
        return myTreeTable.tree.rowCount
    }

    override fun modelStructureChanged() {}
    override fun allRowsChanged() {}
    override fun rowsInserted(firstRow: Int, endRow: Int) {}
    override fun rowsDeleted(firstRow: Int, endRow: Int) {}
    override fun rowsUpdated(firstRow: Int, endRow: Int) {}
    override fun rowsUpdated(firstRow: Int, endRow: Int, column: Int) {}

    companion object {
        private fun <T> reverseComparator(comparator: Comparator<T?>, order: SortOrder): Comparator<T?> {
            return if (order != SortOrder.DESCENDING) comparator else Comparator { o1: T?, o2: T? ->
                -comparator.compare(
                    o1,
                    o2
                )
            }
        }
    }
}
