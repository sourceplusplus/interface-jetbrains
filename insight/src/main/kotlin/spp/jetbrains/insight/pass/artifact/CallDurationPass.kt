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
package spp.jetbrains.insight.pass.artifact

import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import spp.jetbrains.artifact.model.ArtifactElement
import spp.jetbrains.artifact.model.CallArtifact
import spp.jetbrains.insight.InsightKeys
import spp.jetbrains.insight.getDuration
import spp.jetbrains.insight.pass.ArtifactPass
import spp.protocol.insight.InsightType.FUNCTION_DURATION
import spp.protocol.insight.InsightType.PATH_DURATION
import spp.protocol.insight.InsightValue

/**
 * Sets the [FUNCTION_DURATION] on [CallArtifact]s which can be resolved and have a known function duration.
 */
class CallDurationPass : ArtifactPass {

    override fun analyze(element: ArtifactElement) {
        if (element !is CallArtifact) return //only interested in calls

        val resolvedFunction = element.getResolvedFunction()
        if (resolvedFunction != null) {
            val multiPath = element.getData(InsightKeys.PROCEDURAL_MULTI_PATH)
            if (multiPath != null) {
                //artifact has already been analyzed, use pre-determined duration (if available)
                val duration = multiPath.mapNotNull {
                    it.getInsights().find { it.type == PATH_DURATION }?.value as Long?
                }.ifNotEmpty { sum() }
                if (duration != null) {
                    element.putUserData(
                        InsightKeys.FUNCTION_DURATION.asPsiKey(),
                        InsightValue.of(FUNCTION_DURATION, duration).asDerived()
                    )
                }
            }

            val duration = resolvedFunction.getDuration()
            if (duration != null) {
                element.putUserData(
                    InsightKeys.FUNCTION_DURATION.asPsiKey(),
                    InsightValue.of(FUNCTION_DURATION, duration).asDerived()
                )
            }
        }
    }
}
