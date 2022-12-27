/*
 * Source++, the continuous feedback platform for developers.
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
package spp.jetbrains.insight.pass.artifact

import spp.jetbrains.insight.InsightKeys
import spp.jetbrains.insight.pass.ArtifactPass
import spp.jetbrains.marker.model.ArtifactElement
import spp.jetbrains.marker.model.ArtifactLiteralValue
import spp.jetbrains.marker.model.CallArtifact
import spp.protocol.insight.InsightType.FUNCTION_DURATION
import spp.protocol.insight.InsightValue

/**
 * Calculates [FUNCTION_DURATION] for Thread.sleep() [CallArtifact]s that can be statically determined.
 */
class ThreadSleepPass : ArtifactPass {

    override fun analyze(element: ArtifactElement) {
        if (element !is CallArtifact) return //only interested in calls

        //todo: verify better
        if (element.text.contains("sleep(")) {
            val args = element.getArguments()
            if (args.size == 1 && args.first() is ArtifactLiteralValue) {
                val duration = ((args.first() as ArtifactLiteralValue).value as? Number)?.toLong() ?: return
                element.putUserData(
                    InsightKeys.FUNCTION_DURATION.asPsiKey(),
                    InsightValue.of(FUNCTION_DURATION, duration).asDerived()
                )
            }
        }
    }
}
