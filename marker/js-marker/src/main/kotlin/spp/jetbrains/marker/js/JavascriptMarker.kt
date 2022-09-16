/*
 * Source++, the open-source live coding platform.
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
package spp.jetbrains.marker.js

import spp.jetbrains.marker.impl.*

/**
 * Provides JavaScript support for the Marker API.
 *
 * @since 0.6.10
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object JavascriptMarker {

    fun canSetup(): Boolean {
        return try {
            Class.forName("com.intellij.lang.javascript.psi.impl.JSElementImpl")
            true
        } catch (ignore: ClassNotFoundException) {
            false
        }
    }

    fun setup() {
        ArtifactCreationService.addService(JavascriptArtifactCreationService(), "JavaScript", "ECMAScript 6")
        ArtifactNamingService.addService(JavascriptArtifactNamingService(), "JavaScript", "ECMAScript 6")
        ArtifactScopeService.addService(JavascriptArtifactScopeService(), "JavaScript", "ECMAScript 6")
        InstrumentConditionParser.addService(JavascriptConditionParser(), "JavaScript", "ECMAScript 6")
        SourceGuideProvider.addProvider(JavascriptGuideProvider(), "JavaScript", "ECMAScript 6")
    }
}
