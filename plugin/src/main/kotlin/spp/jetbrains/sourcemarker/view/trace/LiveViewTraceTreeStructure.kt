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

import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.ide.util.treeView.AbstractTreeStructure
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.util.ArrayUtil
import spp.jetbrains.view.trace.node.TraceRootTreeNode

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LiveViewTraceTreeStructure(
    private val rootNode: TraceRootTreeNode
) : AbstractTreeStructure() {

    override fun getRootElement(): TraceRootTreeNode = rootNode

    override fun getChildElements(element: Any): Array<Any?> {
        return if (element is AbstractTreeNode<*>) {
            ArrayUtil.toObjectArray(element.children)
        } else ArrayUtil.EMPTY_OBJECT_ARRAY
    }

    override fun getParentElement(element: Any): Any? {
        TODO()
//        return if (element is TraceListNode) {
//            element.parent
//        } else null
    }

    override fun createDescriptor(element: Any, parentDescriptor: NodeDescriptor<*>?): NodeDescriptor<*> {
        return if (element is AbstractTreeNode<*>) {
            element
        } else {
            TODO()
//            return LiveViewTraceDescriptor(project, parentDescriptor, element as Trace)
        }
    }

    override fun commit() = Unit
    override fun hasSomethingToCommit(): Boolean = false
}
