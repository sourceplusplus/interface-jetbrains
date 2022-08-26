/*
 * Source++, the open-source live coding platform.
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
package spp.jetbrains.marker.jvm

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.NUMBER
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.STRING
import com.intellij.ui.SimpleTextAttributes.*
import com.intellij.ui.treeStructure.SimpleNode
import com.intellij.xdebugger.impl.ui.DebuggerUIUtil
import com.intellij.xdebugger.impl.ui.XDebuggerUIConstants
import org.apache.commons.lang3.EnumUtils
import spp.jetbrains.marker.ErrorVariableSimpleNode
import spp.protocol.instrument.variable.LiveVariable
import spp.protocol.instrument.variable.LiveVariableScope

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("MagicNumber")
class JVMVariableSimpleNode(
    val variable: LiveVariable,
    private val nodeMap: MutableMap<String, Array<SimpleNode>>
) : SimpleNode() {

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
    private val numerals = setOf(
        "java.lang.Byte",
        "java.lang.Short",
        "java.lang.Integer",
        "java.lang.Long",
        "java.lang.Float",
        "java.lang.Double"
    )
    private val scheme = DebuggerUIUtil.getColorScheme(null)

    override fun getChildren(): Array<SimpleNode> {
        if (variable.value == null && variable.liveIdentity != null) {
            //found reference, use children of referenced node
            return nodeMap[variable.liveIdentity!!] ?: arrayOf()
        }

        val children = if (variable.value is List<*>) {
            (variable.value as List<Map<*, *>>).map {
                if (it["@skip"] != null) {
                    ErrorVariableSimpleNode(it as Map<String, *>)
                } else {
                    JVMVariableSimpleNode(toLiveVariable(it), nodeMap)
                }
            }.toList().toTypedArray()
        } else {
            emptyArray()
        }

        //add children to nodeMap for reference lookup
        if (variable.liveIdentity != null) {
            nodeMap[variable.liveIdentity!!] = children
        }
        return children
    }

    private fun toLiveVariable(it: Map<*, *>): LiveVariable {
        var varValue = it["value"]
        if (varValue is List<*> && varValue.size == 1 &&
            varValue[0] is Map<*, *> && (varValue[0] as Map<*, *>).containsKey("liveClazz")
        ) {
            varValue = toLiveVariable(varValue[0] as Map<*, *>)
        }
        return LiveVariable(
            name = it["name"] as String,
            value = varValue,
            lineNumber = it["lineNumber"] as Int? ?: -1,
            scope = EnumUtils.getEnum(LiveVariableScope::class.java, it["scope"] as String?),
            liveClazz = it["liveClazz"] as String?,
            liveIdentity = it["liveIdentity"] as String?
        )
    }

    override fun update(presentation: PresentationData) {
        if (variable.scope == LiveVariableScope.GENERATED_METHOD) {
            presentation.addText(variable.name + " = ", GRAYED_ATTRIBUTES)
            presentation.setIcon(AllIcons.Nodes.Method)
        } else {
            presentation.addText(variable.name + " = ", XDebuggerUIConstants.VALUE_NAME_ATTRIBUTES)
        }

        setPresentationForVariable(variable, presentation)
    }

    private fun setPresentationForVariable(variable: LiveVariable, presentation: PresentationData) {
        if (variable.value == null && variable.liveIdentity == null) {
            presentation.addText("null", REGULAR_ATTRIBUTES)
        } else if (variable.liveClazz != null && primitives.contains(variable.liveClazz)) {
            if (variable.liveClazz == "java.lang.Boolean") {
                presentation.addText(variable.value.toString(), REGULAR_ATTRIBUTES)
            } else if (variable.liveClazz == "java.lang.Character") {
                presentation.addText(
                    "'" + variable.value + "' " + (variable.value as String).toCharArray()[0].toInt(),
                    REGULAR_ATTRIBUTES
                )
            } else if (variable.liveClazz == "java.lang.String") {
                presentation.addText("\"" + variable.value + "\"", fromTextAttributes(scheme.getAttributes(STRING)))
            } else if (numerals.contains(variable.liveClazz)) {
                presentation.addText(variable.value.toString(), fromTextAttributes(scheme.getAttributes(NUMBER)))
            }
            presentation.setIcon(AllIcons.Debugger.Db_primitive)
        } else if (variable.liveClazz != null) {
            val simpleClassName = variable.liveClazz!!.substringAfterLast(".")
            val identity = variable.liveIdentity
            if (variable.presentation != null) {
                presentation.addText("{ $simpleClassName@$identity } ", GRAYED_ATTRIBUTES)
                presentation.addText("\"${variable.presentation}\"", REGULAR_ATTRIBUTES)
            } else {
                presentation.addText("{ $simpleClassName@$identity }", GRAYED_ATTRIBUTES)
            }
            presentation.setIcon(AllIcons.Debugger.Value)

            val varValue = variable.value
            if (varValue is Map<*, *> && varValue["@skip"] != null) {
                val skipReason = varValue["@skip"]
                presentation.addText(" $skipReason", ERROR_ATTRIBUTES)
            }
        } else {
            if (variable.value is LiveVariable) {
                val liveVar = variable.value as LiveVariable
                setPresentationForVariable(liveVar, presentation)
            } else if (variable.value is Number) {
                presentation.addText(variable.value.toString(), fromTextAttributes(scheme.getAttributes(NUMBER)))
            } else {
                val varValue = variable.value
                if (
                    varValue is List<*> && varValue.size == 1 &&
                    varValue[0] is Map<*, *> && (varValue[0] as Map<*, *>)["@skip"] != null
                ) {
                    val clazz = (varValue[0] as Map<*, *>)["@class"].toString().substringAfterLast(".")
                    val id = (varValue[0] as Map<*, *>)["@id"]
                    presentation.addText("{ ${clazz}@${id} }", GRAYED_ATTRIBUTES)
                } else {
                    presentation.addText("\"" + varValue + "\"", fromTextAttributes(scheme.getAttributes(STRING)))
                }
            }

            if (variable.scope != LiveVariableScope.GENERATED_METHOD) {
                presentation.setIcon(AllIcons.Debugger.Db_primitive)
            }
        }

        if (variable.liveClazz != null && primitives.contains(variable.liveClazz)) {
            if (variable.value is String) {
                presentation.tooltip = variable.value as String
            } else {
                presentation.tooltip = variable.value.toString()
            }
        }
    }

    override fun getEqualityObjects(): Array<Any> = arrayOf(variable)
}
