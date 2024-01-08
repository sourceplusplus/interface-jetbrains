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
package spp.jetbrains.marker.py.detect

import com.intellij.openapi.project.Project
import spp.jetbrains.marker.source.info.LoggerDetector
import spp.jetbrains.marker.source.mark.guide.MethodGuideMark

/**
 * todo: description.
 *
 * @since 0.7.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class PythonLoggerDetector(project: Project) : LoggerDetector {

    override suspend fun determineLoggerStatements(guideMark: MethodGuideMark): List<LoggerDetector.DetectedLogger> {
        return emptyList()
    }
}
