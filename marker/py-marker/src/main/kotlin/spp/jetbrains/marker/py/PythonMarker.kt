/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package spp.jetbrains.marker.py

import spp.jetbrains.marker.impl.*

/**
 * todo: description.
 *
 * @since 0.5.5
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object PythonMarker {

    fun canSetup(): Boolean {
        return try {
            Class.forName("com.jetbrains.python.psi.PyElement")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    fun setup() {
        ArtifactCreationService.addService(PythonArtifactCreationService(), "Python")
        ArtifactNamingService.addService(PythonArtifactNamingService(), "Python")
        ArtifactScopeService.addService(PythonArtifactScopeService(), "Python")
        InstrumentConditionParser.addService(PythonConditionParser(), "Python")
        SourceGuideProvider.addProvider(PythonGuideProvider(), "Python")
    }
}
