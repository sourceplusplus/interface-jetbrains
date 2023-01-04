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
package spp.jetbrains.marker.js.presentation

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.treeStructure.SimpleNode
import com.intellij.xdebugger.impl.ui.XDebuggerUIConstants
import spp.jetbrains.marker.presentation.LiveVariableNode
import spp.protocol.instrument.variable.LiveVariable
import spp.protocol.instrument.variable.LiveVariableScope

/**
 * todo: description.
 *
 * @since 0.7.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("MagicNumber")
class JavascriptVariableNode(
    variable: LiveVariable,
    nodeMap: MutableMap<String, Array<SimpleNode>>
) : LiveVariableNode(variable, nodeMap) {

    override fun createVariableNode(
        variable: LiveVariable,
        nodeMap: MutableMap<String, Array<SimpleNode>>
    ): SimpleNode {
        return JavascriptVariableNode(variable, nodeMap)
    }

    override fun update(presentation: PresentationData) {
        if (variable.scope == LiveVariableScope.GENERATED_METHOD) {
            presentation.addText(variable.name + " = ", SimpleTextAttributes.GRAYED_ATTRIBUTES)
            presentation.setIcon(AllIcons.Nodes.Method)
        } else {
            presentation.addText(variable.name + " = ", XDebuggerUIConstants.VALUE_NAME_ATTRIBUTES)
        }

        if (childCount > 0) {
            presentation.setIcon(AllIcons.Nodes.Variable)
            presentation.addText("todo", SimpleTextAttributes.REGULAR_ATTRIBUTES)
        } else {
            presentation.setIcon(AllIcons.Nodes.Field)
            presentation.addText(variable.value.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES)
        }
    }
}
