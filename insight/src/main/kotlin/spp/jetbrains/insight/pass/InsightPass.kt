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
package spp.jetbrains.insight.pass

import spp.jetbrains.artifact.model.ArtifactElement
import spp.jetbrains.insight.InsightPassConfig
import spp.jetbrains.insight.InsightPassProvider
import spp.jetbrains.insight.ProceduralAnalyzer

/**
 * Abstract class for passes that analyze data.
 */
abstract class InsightPass {
    lateinit var analyzer: ProceduralAnalyzer
    lateinit var rootArtifact: ArtifactElement
    val provider: InsightPassProvider
        get() = analyzer.passProvider
    val config: InsightPassConfig
        get() = analyzer.passConfig
}
