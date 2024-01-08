/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2024 CodeBrig, Inc.
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
package spp.jetbrains.sourcemarker.instrument.breakpoint.tree

import com.intellij.ui.treeStructure.SimpleNode
import spp.jetbrains.marker.service.ArtifactMarkService
import spp.jetbrains.sourcemarker.instrument.breakpoint.model.ActiveStackTrace

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class VariableRootSimpleNode : SimpleNode() {

    private lateinit var activeStack: ActiveStackTrace

    fun setActiveStackTrace(activeStack: ActiveStackTrace) {
        this.activeStack = activeStack
    }

    override fun getChildren(): Array<SimpleNode> {
        if (!this::activeStack.isInitialized) {
            return emptyArray() //wait till initialized
        }

        val currentFrame = activeStack.currentFrame
        return if (currentFrame == null || currentFrame.variables.isEmpty()) {
            NO_CHILDREN
        } else {
            ArtifactMarkService.toPresentationNodes(activeStack.stackTrace.language!!, currentFrame.variables)
        }
    }
}
