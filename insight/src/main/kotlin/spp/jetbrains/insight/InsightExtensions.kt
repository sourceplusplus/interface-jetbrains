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
package spp.jetbrains.insight

import spp.jetbrains.artifact.model.ArtifactElement
import spp.protocol.insight.InsightValue

fun ArtifactElement.getInsights(): List<InsightValue<*>> {
    return InsightKeys.ALL_INSIGHTS.mapNotNull { getUserData(it.asPsiKey()) } +
            InsightKeys.ALL_INSIGHTS.mapNotNull { getData(it) }
}

fun ArtifactElement.getDuration(includingPredictions: Boolean = true): Long? {
    if (includingPredictions) {
        val durationPrediction = getUserData(InsightKeys.FUNCTION_DURATION_PREDICTION.asPsiKey())?.value
        if (durationPrediction != null) {
            return durationPrediction
        }
    }

    return getUserData(InsightKeys.FUNCTION_DURATION.asPsiKey())?.value
}
