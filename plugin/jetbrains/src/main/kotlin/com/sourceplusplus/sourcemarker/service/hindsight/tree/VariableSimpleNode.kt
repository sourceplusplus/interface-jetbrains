package com.sourceplusplus.sourcemarker.service.hindsight.tree

import com.intellij.debugger.engine.DebuggerUtils
import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.treeStructure.SimpleNode
import com.sourceplusplus.protocol.artifact.debugger.TraceVariable
import java.awt.Color

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("MagicNumber")
class VariableSimpleNode(private val variable: TraceVariable) : SimpleNode() {

    override fun getChildren(): Array<SimpleNode> {
        return emptyArray()
    }

    override fun update(presentation: PresentationData) {
        presentation.addText(
            variable.name + " = ", SimpleTextAttributes(
                SimpleTextAttributes.STYLE_PLAIN,
                JBColor(Color(255, 141, 129), Color(255, 141, 129))
            )
        )

        val value = variable.value
        if (value is String) {
            presentation.addText(value, SimpleTextAttributes.REGULAR_ATTRIBUTES)
        } else {
            presentation.addText(
                DebuggerUtils.convertToPresentationString(value.toString()),
                SimpleTextAttributes.REGULAR_ATTRIBUTES
            )
        }
        if (value is String) {
            presentation.tooltip = value.toString()
        }

//        presentation.setIcon(AllIcons.Debugger.Value)
//        presentation.setIcon(AllIcons.Debugger.Db_array)
//        presentation.setIcon(AllIcons.Debugger.Db_primitive)
//        presentation.setIcon(AllIcons.Nodes.Enum)
        presentation.setIcon(AllIcons.Debugger.Value)
    }
}
