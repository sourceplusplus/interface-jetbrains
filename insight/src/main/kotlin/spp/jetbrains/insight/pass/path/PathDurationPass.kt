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
package spp.jetbrains.insight.pass.path

import spp.jetbrains.insight.RuntimePathImpl
import spp.jetbrains.insight.pass.RuntimePathPass
import spp.protocol.insight.InsightType.PATH_DURATION
import spp.protocol.insight.InsightValue

/**
 * Calculates [PATH_DURATION] based on the sum of the durations of the artifacts in the runtime path.
 */
class PathDurationPass : RuntimePathPass {

    override fun analyze(path: RuntimePathImpl) {
        var duration: Long? = null
        path.forEach {
            val artifactDuration = it.getDuration()
            if (artifactDuration != null) {
                duration = (duration ?: 0) + artifactDuration
            }
        }

        if (duration != null) {
            path.insights.add(InsightValue.of(PATH_DURATION, duration).asDerived())
        }
    }
}
