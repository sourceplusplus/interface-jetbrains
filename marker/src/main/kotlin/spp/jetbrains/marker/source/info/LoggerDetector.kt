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
package spp.jetbrains.marker.source.info

import com.intellij.openapi.editor.Editor
import spp.jetbrains.SourceKey
import spp.jetbrains.marker.source.mark.guide.MethodGuideMark
import spp.jetbrains.marker.source.mark.inlay.InlayMark

/**
 * Detects the presence of log statements within methods and saves log patterns.
 *
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface LoggerDetector {

    companion object {
        @JvmStatic
        val DETECTED_LOGGER = SourceKey<DetectedLogger>("DETECTED_LOGGER")
    }

    fun addLiveLog(editor: Editor, inlayMark: InlayMark, logPattern: String, lineLocation: Int) {
        val detectedLogger = DetectedLogger(logPattern, "live", lineLocation)
        inlayMark.putUserData(DETECTED_LOGGER, detectedLogger)
    }

    suspend fun determineLoggerStatements(guideMark: MethodGuideMark): List<DetectedLogger>

    /**
     * Represents a detected log statement.
     *
     * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
     */
    data class DetectedLogger(
        val logPattern: String,
        val level: String,
        val lineLocation: Int
    )
}
