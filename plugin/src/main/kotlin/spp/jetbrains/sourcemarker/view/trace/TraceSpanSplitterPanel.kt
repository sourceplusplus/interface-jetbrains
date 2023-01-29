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

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.OnePixelSplitter
import spp.jetbrains.sourcemarker.view.trace.node.TraceSpanTreeNode
import spp.jetbrains.sourcemarker.view.trace.table.TraceSpanTable
import spp.jetbrains.sourcemarker.view.trace.table.TraceSpanTreeTable
import spp.protocol.artifact.trace.TraceSpan
import javax.swing.tree.DefaultMutableTreeNode

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class TraceSpanSplitterPanel(project: Project, spans: List<TraceSpan>) : OnePixelSplitter(0.7f), Disposable {

    init {
        val spanTreeTable = TraceSpanTreeTable(project, spans)
        Disposer.register(this, spanTreeTable)
        firstComponent = spanTreeTable

        val spanInfoTable = TraceSpanTable(spans.first())
        secondComponent = spanInfoTable.component

        spanTreeTable.tree.addTreeSelectionListener {
            val selected =
                (it.newLeadSelectionPath?.lastPathComponent as DefaultMutableTreeNode).userObject as TraceSpanTreeNode
            spanInfoTable.setSpan(selected.value)
        }
    }
}
