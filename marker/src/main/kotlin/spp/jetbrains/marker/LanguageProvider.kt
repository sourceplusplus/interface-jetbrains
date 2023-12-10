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
package spp.jetbrains.marker

import com.intellij.openapi.project.Project
import java.util.*

interface LanguageProvider {
    fun canSetup(): Boolean
    fun setup(project: Project, setupDetectors: Boolean = true)

    fun getUltimateProvider(project: Project): UltimateProvider? {
        val ultimateProvider: UltimateProvider?
        val originalClassLoader = Thread.currentThread().contextClassLoader
        try {
            Thread.currentThread().contextClassLoader = javaClass.classLoader
            ultimateProvider = ServiceLoader.load(UltimateProvider::class.java).firstOrNull()
        } finally {
            Thread.currentThread().contextClassLoader = originalClassLoader
        }
        return ultimateProvider
    }

    fun classExists(className: String): Boolean {
        return try {
            Class.forName(className)
            true
        } catch (ignore: ClassNotFoundException) {
            false
        }
    }
}
