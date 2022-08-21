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
package spp.jetbrains.monitor.skywalking.model

data class ServiceInstance(
    val id: String,
    val name: String,
    val language: Language,
    val instanceUUID: String,
    val attributes: List<Attribute>,
) {

    enum class Language {
        JAVA,
        DOTNET,
        NODEJS,
        PYTHON,
        RUBY,
        GO,
        LUA,
        PHP
    }

    data class Attribute(
        val name: String,
        val value: String,
    )
}