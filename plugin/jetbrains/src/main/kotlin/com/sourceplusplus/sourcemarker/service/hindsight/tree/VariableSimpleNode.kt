package com.sourceplusplus.sourcemarker.service.hindsight.tree

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.treeStructure.SimpleNode
import com.sourceplusplus.protocol.artifact.debugger.TraceVariable
import io.vertx.core.json.JsonObject
import java.awt.Color
import kotlin.streams.toList

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("MagicNumber")
class VariableSimpleNode(private val variable: TraceVariable, private val root: Boolean) : SimpleNode() {

    private var jsonObject: JsonObject? = null
    private var clazz: String? = null

    init {
        if (root) {
            jsonObject = JsonObject(variable.value.toString())
            clazz = jsonObject!!.getString("encoded-class")!!
        }
    }

    private val primitives = setOf(
        "java.lang.String",
        "java.lang.Boolean",
        "java.lang.Character",
        "java.lang.Byte",
        "java.lang.Short",
        "java.lang.Integer",
        "java.lang.Long",
        "java.lang.Float",
        "java.lang.Double"
    )

    override fun getChildren(): Array<SimpleNode> {
        return try {
            if (root) {
                val data = jsonObject!!.getJsonObject("encoded")
                return data.stream().map {
                    VariableSimpleNode(TraceVariable(it.key, it.value), false)
                }.toList().toTypedArray()
            } else {
                emptyArray()
            }
        } catch (ignored: Throwable) {
            emptyArray()
        }
    }

    override fun update(presentation: PresentationData) {
        presentation.addText(
            variable.name + " = ", SimpleTextAttributes(
                SimpleTextAttributes.STYLE_PLAIN,
                JBColor(Color(255, 141, 129), Color(255, 141, 129))
            )
        )

        if (clazz != null && primitives.contains(clazz)) {
            presentation.addText(jsonObject!!.getString("encoded"), SimpleTextAttributes.REGULAR_ATTRIBUTES)
            presentation.setIcon(AllIcons.Debugger.Db_primitive)
        } else if (clazz != null) {
            if (clazz == "java.lang.Class") {
                presentation.addText(jsonObject!!.getString("encoded"), SimpleTextAttributes.GRAYED_ATTRIBUTES)
                presentation.setIcon(AllIcons.Debugger.Value)
            } else {
                presentation.addText("{$clazz}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                presentation.setIcon(AllIcons.Debugger.Value)
            }
        } else {
            presentation.addText(variable.value.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES)
            presentation.setIcon(AllIcons.Debugger.Db_primitive)
        }

        if (clazz != null && primitives.contains(clazz)) {
            presentation.tooltip = jsonObject!!.getString("encoded")
        }
    }
}
