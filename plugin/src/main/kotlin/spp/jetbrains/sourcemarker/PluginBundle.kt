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
package spp.jetbrains.sourcemarker

import com.intellij.AbstractBundle
import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import java.util.*

@NonNls
private const val BUNDLE = "messages.PluginBundle"

/**
 * todo: description.
 *
 * @since 0.2.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object PluginBundle : AbstractBundle(BUNDLE) {

    val LOCALE: Locale by lazy {
        val chineseLanguagePlugin = "com.intellij.zh"
        if (PluginManager.isPluginInstalled(PluginId.getId(chineseLanguagePlugin))) {
            Locale.CHINA
        } else {
            Locale.ROOT
        }
    }

    //todo: shouldn't need to manually load bundle.
    val LOCALE_BUNDLE: ResourceBundle by lazy {
        ResourceBundle.getBundle(BUNDLE, LOCALE, PluginBundle::class.java.classLoader)
    }

    @Suppress("SpreadOperator")
    @JvmStatic
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String {
        return try {
            LOCALE_BUNDLE.getString(key) ?: getMessage(key, *params)
        } catch (ignore: MissingResourceException) {
            key // no translation found
        }
    }
}
