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
package spp.jetbrains.marker.py.presentation

import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.treeStructure.SimpleNode
import com.intellij.xdebugger.impl.ui.XDebuggerUIConstants
import io.vertx.core.json.JsonObject
import spp.jetbrains.marker.presentation.LiveVariableNode
import spp.protocol.instrument.variable.LiveVariable

/**
 * todo: description.
 *
 * @since 0.2.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("MagicNumber")
class PythonVariableNode(
    variable: LiveVariable
) : LiveVariableNode(variable, mutableMapOf()) {

    override fun createVariableNode(
        variable: LiveVariable,
        nodeMap: MutableMap<String, Array<SimpleNode>>
    ): SimpleNode {
        return PythonVariableNode(variable)
    }

    override fun getChildren(): Array<SimpleNode> {
        if (variable.liveClazz == "<class 'dict'>") {
            val dict = JsonObject(parseDict(variable.value as String))
            val children = mutableListOf<SimpleNode>()
            dict.map.forEach {
                children.add(PythonVariableNode(LiveVariable("'" + it.key + "'", it.value)))
            }
            return children.toTypedArray()
        }
        return emptyArray()
    }

    override fun update(presentation: PresentationData) {
        presentation.addText(variable.name, XDebuggerUIConstants.VALUE_NAME_ATTRIBUTES)
        presentation.addText(
            " = ",
            SimpleTextAttributes.fromTextAttributes(scheme.getAttributes(DefaultLanguageHighlighterColors.IDENTIFIER))
        )

        if (variable.liveClazz?.startsWith("<class '") == true) {
            presentation.addText(
                "{" + variable.liveClazz!!.substringAfter("'").substringBefore("'") + "} ",
                SimpleTextAttributes.GRAYED_ATTRIBUTES
            )
        }

        when {
            variable.liveClazz == "<class 'int'>" || variable.value is Number -> {
                presentation.addText(
                    variable.value.toString(),
                    SimpleTextAttributes.fromTextAttributes(
                        scheme.getAttributes(DefaultLanguageHighlighterColors.NUMBER)
                    )
                )
            }

            variable.liveClazz == "<class 'str'>" -> {
                presentation.addText(
                    "\"" + variable.value + "\"",
                    SimpleTextAttributes.fromTextAttributes(
                        scheme.getAttributes(DefaultLanguageHighlighterColors.STRING)
                    )
                )
            }

            else -> {
                presentation.addText(
                    variable.value.toString(),
                    SimpleTextAttributes.fromTextAttributes(
                        scheme.getAttributes(DefaultLanguageHighlighterColors.IDENTIFIER)
                    )
                )
            }
        }
    }

    companion object {
        fun parseDict(value: String): Map<String, String> {
            var dictVar = value

            //remove outer braces
            if (dictVar.startsWith("{")) {
                dictVar = dictVar.substring(1)
            }
            if (dictVar.endsWith("}")) {
                dictVar = dictVar.substring(0, dictVar.length - 1)
            }

            //split by , but not inside quotes
            val tokens = dictVar.split(Regex(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"))

            //each token should contain ":", for those that don't we need to merge them with the previous token
            val mergedTokens = mutableListOf<String>()
            for (i in tokens.indices) {
                if (tokens[i].contains(":")) {
                    mergedTokens.add(tokens[i])
                } else {
                    mergedTokens[mergedTokens.size - 1] += "," + tokens[i]
                }
            }

            //now we can split each token into key and value
            val dict = mutableMapOf<String, String>()
            for (token in mergedTokens) {
                var key = token.substringBefore(":").trim()
                var value = token.substringAfter(":").trim()

                //remove quotes
                if (key.startsWith("\"")) {
                    key = key.substring(1)
                }
                if (key.endsWith("\"")) {
                    key = key.substring(0, key.length - 1)
                }
                if (value.startsWith("\"")) {
                    value = value.substring(1)
                }
                if (value.endsWith("\"")) {
                    value = value.substring(0, value.length - 1)
                }

                dict[key] = value
            }

            return dict
        }
    }
}
