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
package spp.jetbrains.sourcemarker.view.trace.table

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBTreeTable
import spp.jetbrains.sourcemarker.view.trace.LiveViewTraceModel
import spp.jetbrains.sourcemarker.view.trace.LiveViewTraceRowSorter
import spp.jetbrains.sourcemarker.view.trace.LiveViewTraceTreeStructure
import spp.jetbrains.sourcemarker.view.trace.node.TraceRootTreeNode
import spp.jetbrains.sourcemarker.view.trace.renderer.SpanEventTableCellRenderer
import spp.protocol.artifact.trace.TraceSpan
import java.time.Duration
import javax.swing.RowSorter
import javax.swing.SortOrder

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class TraceSpanTreeTable(
    project: Project,
    spans: List<TraceSpan>,
    private val rootNode: TraceRootTreeNode = TraceRootTreeNode(project, spans),
    model: LiveViewTraceModel = LiveViewTraceModel(LiveViewTraceTreeStructure(rootNode))
) : JBTreeTable(model), Disposable {

    init {
        Disposer.register(this, model)

        table.isFocusable = false
        table.rowSelectionAllowed = false

        setDefaultRenderer(Duration::class.java, SpanEventTableCellRenderer(model))

        val rowSorter = LiveViewTraceRowSorter(this, model)
        setRowSorter(rowSorter)
        val sortKey = RowSorter.SortKey(
            LiveViewTraceModel.COLUMN_INFOS.indexOfFirst { it.name == "Time" },
            SortOrder.DESCENDING
        )
        rowSorter.sortKeys = listOf(sortKey)
    }

    override fun dispose() = Unit
}
