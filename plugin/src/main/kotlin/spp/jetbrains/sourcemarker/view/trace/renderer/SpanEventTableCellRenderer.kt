/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2024 CodeBrig, Inc.
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
package spp.jetbrains.sourcemarker.view.trace.renderer

import com.intellij.ui.ColorUtil
import com.intellij.ui.DarculaColors
import com.intellij.ui.components.JBLabel
import com.intellij.ui.render.RenderingUtil
import com.intellij.util.ui.JBUI
import spp.jetbrains.sourcemarker.view.trace.LiveViewTraceModel
import spp.jetbrains.view.trace.node.TraceSpanTreeNode
import java.awt.Component
import java.awt.Graphics
import javax.swing.JTable
import javax.swing.table.TableCellRenderer
import javax.swing.tree.DefaultMutableTreeNode

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SpanEventTableCellRenderer(private val model: LiveViewTraceModel) : JBLabel(), TableCellRenderer {

    private var x = 0
    private var width = 0
    private var color = ColorUtil.withAlpha(DarculaColors.BLUE, 0.5)

    init {
        border = JBUI.Borders.empty(0, 4)
        horizontalAlignment = 4
    }

    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        val treeNode = model.getRowValue(table.rowSorter.convertRowIndexToModel(row)) as DefaultMutableTreeNode
        val spanNode = (treeNode.userObject as? TraceSpanTreeNode) ?: return this
        val span = spanNode.value

        color = if (span.error == true) {
            ColorUtil.withAlpha(DarculaColors.RED, 0.5)
        } else {
            ColorUtil.withAlpha(DarculaColors.BLUE, 0.5)
        }

        val traceStart = spanNode.traceStack.minOf { it.startTime.toEpochMilli() }
        val traceEnd = spanNode.traceStack.maxOf { it.endTime.toEpochMilli() }
        val traceDuration = traceEnd - traceStart
        val spanDuration = span.endTime.toEpochMilli() - span.startTime.toEpochMilli()
        val spanStart = span.startTime.toEpochMilli() - traceStart
        val spanPercent = spanDuration.toDouble() / traceDuration.toDouble()
        val spanPercentWidth = (spanPercent * table.width).toInt()
        x = (spanStart.toDouble() / traceDuration.toDouble() * table.width).toInt()

        val comp = this as JBLabel
        background = RenderingUtil.getBackground(table, isSelected)
        foreground = RenderingUtil.getForeground(table, isSelected)
        width = spanPercentWidth
        comp.text = value.toString()
        return this
    }

    override fun isOpaque(): Boolean = false

    override fun paintComponent(g: Graphics) {
        g.color = background
        g.fillRect(0, 0, width, height)
        g.color = color
        val pad = JBUI.scale(1)
        g.fillRect(x, pad, width.coerceAtLeast(1), height - pad * 2)
        super.paintComponent(g)
    }
}
