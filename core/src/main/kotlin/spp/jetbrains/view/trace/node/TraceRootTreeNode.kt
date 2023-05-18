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
package spp.jetbrains.view.trace.node

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import spp.protocol.artifact.trace.TraceSpan

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class TraceRootTreeNode(
    project: Project,
    traceStack: List<TraceSpan>
) : AbstractTreeNode<List<TraceSpan>>(project, traceStack) {

    override fun update(presentation: PresentationData) = Unit

    override fun getChildren(): Collection<TraceSpanTreeNode> {
        val rootSpans = value.filter { it.parentSpanId == -1 }.toMutableList()

        //remove root spans when cross-process refs parented to root span
        rootSpans.removeAll {
            val ref = it.refs.find { it.type == "CROSS_PROCESS" } ?: return@removeAll false
            rootSpans.any { it.segmentId == ref.parentSegmentId }
        }

        return rootSpans.map { TraceSpanTreeNode(project, value, it) }
    }
}
