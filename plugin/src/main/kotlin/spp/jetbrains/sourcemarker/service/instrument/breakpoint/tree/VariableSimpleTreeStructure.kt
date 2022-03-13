/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package spp.jetbrains.sourcemarker.service.instrument.breakpoint.tree

import com.intellij.ui.treeStructure.SimpleTreeStructure
import spp.jetbrains.sourcemarker.service.instrument.breakpoint.StackFrameManager

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class VariableSimpleTreeStructure : SimpleTreeStructure() {

    private val simpleRoot = VariableRootSimpleNode()

    override fun getRootElement(): VariableRootSimpleNode {
        return simpleRoot
    }

    fun setStackFrameManager(stackFrameManager: StackFrameManager) {
        simpleRoot.setStackFrameManager(stackFrameManager)
    }
}