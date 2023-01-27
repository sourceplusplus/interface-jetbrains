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

import spp.jetbrains.SourceKey
import spp.protocol.insight.InsightType
import spp.protocol.insight.InsightValue

/**
 * Keys useful for storing and/or facilitating insight processing
 */
object InsightKeys {

    val RUNTIME_PATHS = SourceKey<Set<ProceduralPath>>("RUNTIME_PATHS")

    val FUNCTION_DURATION = SourceKey<InsightValue<Long>>(InsightType.FUNCTION_DURATION.name)
    val FUNCTION_DURATION_PREDICTION = SourceKey<InsightValue<Long>>(InsightType.FUNCTION_DURATION_PREDICTION.name)
    val CONTROL_STRUCTURE_PROBABILITY = SourceKey<InsightValue<Double>>(InsightType.CONTROL_STRUCTURE_PROBABILITY.name)
    val PATH_EXECUTION_PROBABILITY = SourceKey<InsightValue<Double>>(InsightType.PATH_EXECUTION_PROBABILITY.name)
    val PATH_DURATION = SourceKey<InsightValue<Double>>(InsightType.PATH_DURATION.name)
    val RECURSIVE_CALL = SourceKey<InsightValue<Boolean>>(InsightType.RECURSIVE_CALL.name)
    val CONDITION_EVALUATION = SourceKey<Boolean>("CONDITION_EVALUATION")

    val ALL_INSIGHTS: List<SourceKey<out InsightValue<out Any>>> = listOf(
        FUNCTION_DURATION,
        FUNCTION_DURATION_PREDICTION,
        CONTROL_STRUCTURE_PROBABILITY,
        PATH_EXECUTION_PROBABILITY,
        PATH_DURATION,
        RECURSIVE_CALL
    )
}
