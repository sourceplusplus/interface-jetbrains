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
package spp.jetbrains.sourcemarker.service.view.trace.renderer

import com.intellij.ui.components.JBLabel
import com.intellij.ui.render.RenderingUtil
import com.intellij.util.ui.JBUI
import spp.jetbrains.PluginUI
import java.awt.Color
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

    companion object {
        const val SPACING_SIZE = 8
    }

    private val percentage = PercentageState()

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
        val comp = this as JBLabel
        val var10000 = table.columnModel.getColumn(column)
        val width = var10000.width
        background = RenderingUtil.getBackground(table, isSelected)
        foreground = RenderingUtil.getForeground(table, isSelected)
        percentage.clear()
        val var10001: String?
        percentage.apply {
            this.x = 100
            this.width = 200
            this.color = PluginUI.purple
        }
        var10001 = this.formatValue(11020)
        comp.text = var10001
        return this
    }

    private fun formatValue(value: Number): String {
        val var2 = if (value !is Int && value !is Long) {
            if (value !is Float && value !is Double) {
                "%s"
            } else "%.2f"
        } else "%,d"
        val var3 = arrayOf<Any>(value)
        return String.format(var2, *var3.copyOf(var3.size))
    }

    override fun isOpaque(): Boolean = false

    override fun paintComponent(g: Graphics) {
        g.color = background
        g.fillRect(0, 0, width, height)
        g.color = percentage.color
        val pad = JBUI.scale(1)
        val var10001: Int = percentage.x
        val var3: Int = percentage.width
        val var4: Byte = 1
        g.fillRect(var10001, pad, var3.coerceAtLeast(var4.toInt()), height - pad * 2)
        super.paintComponent(g)
    }

    private class PercentageState(
        var x: Int = 0,
        var width: Int = 0,
        var color: Color? = null
    ) {

        fun updateState(x: Int, width: Int, color: Color?) {
            this.x = x
            this.width = width
            this.color = color
        }

        fun clear() {
            updateState(0, 0, null as Color?)
        }
    }
}
