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
package spp.jetbrains.marker.rs

import com.intellij.openapi.project.Project
import org.rust.lang.core.psi.RsFile
import spp.jetbrains.artifact.service.ArtifactModelService
import spp.jetbrains.artifact.service.ArtifactTypeService
import spp.jetbrains.marker.LanguageProvider
import spp.jetbrains.marker.rs.service.RustArtifactModelService
import spp.jetbrains.marker.rs.service.RustArtifactTypeService
import spp.jetbrains.marker.source.SourceFileMarker

/**
 * Provides Rust support for the Marker API.
 *
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class RustLanguageProvider : LanguageProvider {

    override fun canSetup() = classExists("org.rust.lang.core.psi.ext.RsElement")

    override fun setup(project: Project, setupDetectors: Boolean) {
        SourceFileMarker.SUPPORTED_FILE_TYPES.add(RsFile::class.java)

        ArtifactModelService.addService(RustArtifactModelService(), "Rust")
        ArtifactTypeService.addService(RustArtifactTypeService(), "Rust")
    }
}
