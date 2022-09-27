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
package spp.jetbrains.marker.jvm

import spp.jetbrains.marker.SourceMarkerUtils
import spp.jetbrains.marker.impl.*

/**
 * todo: description.
 *
 * @since 0.5.5
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object JVMMarker {

    fun canSetup(): Boolean = true

    fun setup() {
        ArtifactCreationService.addService(JVMArtifactCreationService(), SourceMarkerUtils.getJvmLanguages())
        ArtifactNamingService.addService(JVMArtifactNamingService(), SourceMarkerUtils.getJvmLanguages())
        ArtifactScopeService.addService(JVMArtifactScopeService(), SourceMarkerUtils.getJvmLanguages())
        ArtifactConditionService.addService(JVMArtifactConditionService(), SourceMarkerUtils.getJvmLanguages())
        ArtifactTypeService.addService(JVMArtifactTypeService(), SourceMarkerUtils.getJvmLanguages())
        SourceGuideProvider.addProvider(JVMGuideProvider(), SourceMarkerUtils.getJvmLanguages())
    }
}
