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

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import org.joor.Reflect
import spp.protocol.artifact.trace.TraceSpan
import java.awt.Color

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
open class SpanInfoListNode(project: Project, private val span: TraceSpan) : AbstractTreeNode<Any>(project, span) {

    override fun update(presentation: PresentationData) {
        ApplicationManager.getApplication().runReadAction {
            presentation.setPresentableText(value?.toString())
            presentation.setIcon(AllIcons.Ide.Gift)
            presentation.forcedTextForeground = Color.orange
        }
    }

    override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
        return Reflect.on(span).fields().map { field ->
            val value = field.value.get<Any?>()
            SpanValueListNode(project, field.key + " = " + value)
        }.toMutableList()
    }
}
