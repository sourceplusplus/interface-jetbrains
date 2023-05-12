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
package spp.jetbrains.view.trace.renderer

import com.codahale.metrics.Histogram
import com.codahale.metrics.SlidingWindowReservoir
import com.intellij.ui.ColorUtil
import com.intellij.ui.DarculaColors
import com.intellij.ui.components.JBLabel
import com.intellij.ui.render.RenderingUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.ListTableModel
import spp.protocol.artifact.trace.Trace
import spp.protocol.utils.toPrettyDuration
import java.awt.Component
import java.awt.Graphics
import javax.swing.JTable
import javax.swing.table.TableCellRenderer

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class TraceDurationTableCellRenderer : JBLabel(), TableCellRenderer {

    private val histogram = Histogram(SlidingWindowReservoir(1000))
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
        val duration = value.toString().toInt()
        histogram.update(duration)

        val trace = (table.model as ListTableModel<Trace>).getRowValue(table.rowSorter.convertRowIndexToModel(row))
        color = if (trace.error == true) {
            ColorUtil.withAlpha(DarculaColors.RED, 0.5)
        } else {
            ColorUtil.withAlpha(DarculaColors.BLUE, 0.5)
        }

        val columnWidth = table.columnModel.getColumn(column).width
        background = RenderingUtil.getBackground(table, isSelected)
        foreground = RenderingUtil.getForeground(table, isSelected)
        width = (columnWidth * (duration / histogram.snapshot.max.toDouble())).toInt()

        val comp = this as JBLabel
        comp.text = duration.toPrettyDuration()
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
