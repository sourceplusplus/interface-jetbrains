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
package spp.jetbrains.sourcemarker.mark

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.SourceMarkerKeys
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.guide.ClassGuideMark
import spp.jetbrains.marker.source.mark.guide.GuideMark
import spp.jetbrains.marker.source.mark.guide.MethodGuideMark

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object SourceMarkSearch {

    fun getClosestGuideMark(sourceFileMarker: SourceFileMarker, editor: Editor): GuideMark? {
        var classSourceMark: ClassGuideMark? = null
        val sourceMark = sourceFileMarker.getSourceMarks().filterIsInstance<GuideMark>().find {
            if (it is ClassGuideMark) {
                classSourceMark = it //todo: probably doesn't handle inner classes well
                false
            } else if (it is MethodGuideMark) {
                if (it.configuration.activateOnKeyboardShortcut) {
                    //+1 on end offset so match is made even right after method end
                    val incTextRange = TextRange(
                        it.getPsiMethod().textRange.startOffset,
                        it.getPsiMethod().textRange.endOffset + 1
                    )
                    incTextRange.contains(editor.logicalPositionToOffset(editor.caretModel.logicalPosition))
                } else {
                    false
                }
            } else {
                false
            }
        }
        return sourceMark ?: classSourceMark
    }

    fun findByInstrumentId(project: Project, instrumentId: String): SourceMark? {
        return SourceMarker.getInstance(project).getSourceMarks().firstOrNull {
            it.getUserData(SourceMarkerKeys.INSTRUMENT_ID) == instrumentId
        }
    }

    fun findBySubscriptionId(project: Project, subscriptionId: String): SourceMark? {
        return SourceMarker.getInstance(project).getSourceMarks().firstOrNull {
            it.getUserData(SourceMarkerKeys.VIEW_SUBSCRIPTION_ID) == subscriptionId
        }
    }
}
