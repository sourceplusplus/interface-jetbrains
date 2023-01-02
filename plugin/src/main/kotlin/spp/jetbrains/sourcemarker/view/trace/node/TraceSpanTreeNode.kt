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
package spp.jetbrains.sourcemarker.view.trace.node

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
class TraceSpanTreeNode(
    project: Project,
    val traceStack: List<TraceSpan>,
    span: TraceSpan
) : AbstractTreeNode<TraceSpan>(project, span) {

    override fun update(presentation: PresentationData) {
        presentation.presentableText = value.endpointName
    }

    override fun getChildren(): Collection<TraceSpanTreeNode> {
        val childSpans = traceStack.filter {
            it.parentSpanId == value.spanId && it.segmentId == value.segmentId
        }.toMutableList()

        //add cross-process refs as children
        childSpans.addAll(traceStack.filter {
            val ref = it.refs.find { it.type == "CROSS_PROCESS" } ?: return@filter false
            ref.parentSegmentId == value.segmentId && ref.parentSpanId == value.spanId
        })

        return childSpans.map { TraceSpanTreeNode(project, traceStack, it) }
    }
}
