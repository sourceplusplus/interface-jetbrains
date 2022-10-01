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
package spp.jetbrains.sourcemarker.service.instrument.breakpoint.tree

import com.intellij.ui.treeStructure.SimpleNode
import com.intellij.util.containers.hash.LinkedHashMap
import spp.jetbrains.marker.js.presentation.JavascriptVariableRootNode
import spp.jetbrains.marker.jvm.presentation.JVMVariableNode
import spp.jetbrains.marker.py.presentation.PythonVariableRootNode
import spp.jetbrains.sourcemarker.service.instrument.breakpoint.StackFrameManager
import spp.protocol.artifact.ArtifactLanguage
import spp.protocol.instrument.variable.LiveVariableScope

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class VariableRootSimpleNode : SimpleNode() {

    private lateinit var stackFrameManager: StackFrameManager

    fun setStackFrameManager(currentStackFrameManager: StackFrameManager) {
        stackFrameManager = currentStackFrameManager
    }

    override fun getChildren(): Array<SimpleNode> {
        if (!this::stackFrameManager.isInitialized) {
            return emptyArray() //wait till initialized
        }

        return if (stackFrameManager.currentFrame?.variables.isNullOrEmpty()) {
            NO_CHILDREN
        } else {
            val vars = stackFrameManager.currentFrame!!.variables
            when (stackFrameManager.stackTrace.language) {
                ArtifactLanguage.NODEJS -> {
                    return arrayOf(
                        JavascriptVariableRootNode(
                            vars.filter { it.scope == LiveVariableScope.LOCAL_VARIABLE },
                            LiveVariableScope.LOCAL_VARIABLE
                        ),
                        JavascriptVariableRootNode(
                            vars.filter { it.scope == LiveVariableScope.GLOBAL_VARIABLE },
                            LiveVariableScope.GLOBAL_VARIABLE
                        )
                    )
                }

                ArtifactLanguage.PYTHON -> {
                    return arrayOf(
                        PythonVariableRootNode(
                            vars.filter { it.scope == LiveVariableScope.GLOBAL_VARIABLE },
                            LiveVariableScope.GLOBAL_VARIABLE
                        ),
                        PythonVariableRootNode(
                            vars.filter { it.scope == LiveVariableScope.LOCAL_VARIABLE },
                            LiveVariableScope.LOCAL_VARIABLE
                        )
                    )
                }

                else -> {
                    val simpleNodeMap: MutableMap<String, JVMVariableNode> = LinkedHashMap()
                    val nodeReferenceMap = mutableMapOf<String, Array<SimpleNode>>()
                    vars.forEach {
                        if (it.name.isNotEmpty()) {
                            simpleNodeMap[it.name] = JVMVariableNode(it, nodeReferenceMap)
                        }
                    }
                    simpleNodeMap.values.sortedWith { p0, p1 ->
                        when {
                            p0.variable.name == "this" -> -1
                            p1.variable.name == "this" -> 1
                            else -> 0
                        }
                    }.toTypedArray()
                }
            }
        }
    }
}
