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
package spp.jetbrains.marker.extend

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import io.vertx.core.json.JsonObject
import spp.protocol.marshall.ProtocolMarshaller
import java.io.File
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.Function

@Suppress("unused")
abstract class LiveCommand(val project: Project) {
    abstract val name: String
    abstract val description: String
    open val aliases: Set<String> = emptySet()
    abstract val selectedIcon: String?
    abstract val unselectedIcon: String?

    val triggerConsumer: BiConsumer<String, Consumer<Array<Any?>>> =
        BiConsumer<String, Consumer<Array<Any?>>> { context, eventConsumer ->
            val contextMap = JsonObject(context)
            val liveCommandContext = LiveCommandContext(
                File(contextMap.getString("sourceFile")),
                contextMap.getInteger("lineNumber"),
                ProtocolMarshaller.deserializeArtifactQualifiedName(contextMap.getJsonObject("artifactQualifiedName")),
                contextMap.getJsonObject("guideArtifactQualifiedName")?.let {
                    ProtocolMarshaller.deserializeArtifactQualifiedName(it)
                },
                eventConsumer
            )
            contextMap.getJsonObject("userData")?.fieldNames()?.forEach {
                val value = contextMap.getJsonObject("userData").getValue(it)
                liveCommandContext.putUserData(it, value)
            }
            trigger(liveCommandContext)
        }

    abstract fun trigger(context: LiveCommandContext)

    fun toJson(): String {
        return JsonObject().apply {
            put("name", name)
            put("description", description)
            put("aliases", aliases)
            put("selectedIcon", selectedIcon)
            put("unselectedIcon", unselectedIcon)
        }.toString()
    }

    fun message(message: String): String {
        val pluginUIFunctions = Key.findKeyByName("PLUGIN_UI_FUNCTIONS")!!
        val consumer = project.getUserData(pluginUIFunctions) as Function<Array<Any?>, String>
        return consumer.apply(arrayOf("message", message))
    }

    fun getCommandTypeColor(): String {
        val pluginUIFunctions = Key.findKeyByName("PLUGIN_UI_FUNCTIONS")!!
        val consumer = project.getUserData(pluginUIFunctions) as Function<Array<Any?>, String>
        return consumer.apply(arrayOf("getCommandTypeColor"))
    }

    fun getCommandHighlightColor(): String {
        val pluginUIFunctions = Key.findKeyByName("PLUGIN_UI_FUNCTIONS")!!
        val consumer = project.getUserData(pluginUIFunctions) as Function<Array<Any?>, String>
        return consumer.apply(arrayOf("getCommandHighlightColor"))
    }
}
