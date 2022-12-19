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
package spp.jetbrains.marker.source.mark.api.key

import com.intellij.openapi.util.Key
import spp.jetbrains.marker.source.mark.guide.GuideMark
import spp.jetbrains.marker.source.mark.gutter.GutterMark
import spp.jetbrains.marker.source.mark.inlay.InlayMark

/**
 * Used to associate custom data to PSI elements.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
data class SourceKey<T>(val name: String) {
    companion object {
        val GutterMark = Key.create<GutterMark>("sm.GutterMark")
        val InlayMarks = Key.create<Set<InlayMark>>("sm.InlayMarks")
        val GuideMark = Key.create<GuideMark>("sm.GuideMark")

        private val keyCache = mutableMapOf<String, Key<*>>()
    }

    fun asPsiKey(): Key<T> {
        return keyCache.computeIfAbsent(name) { Key.create<T>(it) } as Key<T>
    }
}
