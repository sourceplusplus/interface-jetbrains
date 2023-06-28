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
package spp.jetbrains

import com.intellij.codeInsight.lookup.impl.LookupCellRenderer
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.UIUtil.getWindowColor
import spp.jetbrains.PluginBundle.LOCALE
import java.awt.Color
import java.awt.Font
import javax.swing.border.LineBorder

object PluginUI {

    val yellow = JBColor("yellow", Color(190, 145, 23))
    val purple = JBColor("purple", Color(151, 118, 169))
    val green = JBColor("green", Color(98, 150, 85))

    @JvmField
    val LABEL_FOREGROUND_COLOR = JBColor("LABEL_FOREGROUND_COLOR", Color(152, 118, 170))

    @JvmField
    val LABEL_FOREGROUND_COLOR1 = JBColor("LABEL_FOREGROUND_COLOR1", Color(106, 135, 89))

    @JvmField
    val EXPIRY_FOREGROUND_COLOR: JBColor = JBColor("EXPIRY_FOREGROUND_COLOR", Color.decode("#BBBBBB"))

    @JvmField
    val SELECT_COLOR_RED: JBColor = JBColor("SELECT_COLOR_RED", Color.decode("#e1483b"))

    @JvmField
    val COMPLETE_COLOR_PURPLE: JBColor = JBColor("COMPLETE_COLOR_PURPLE", Color.decode("#9876AA"))

    @JvmStatic
    val commandTypeColor: String
        get() = "#" + Integer.toHexString(getLabelForeground().rgb).substring(2)

    @JvmStatic
    val commandHighlightForeground: JBColor
        get() = JBColor("HIGHLIGHT_FOREGROUND", LookupCellRenderer.MATCHED_FOREGROUND_COLOR)

    @JvmStatic
    val commandHighlightColor: String
        get() = "#" + Integer.toHexString(commandHighlightForeground.rgb).substring(2)

    @JvmStatic
    val editCompleteColor: JBColor
        get() = JBColor("WINDOW_COLOR", getWindowColor())

    @JvmField
    val PANEL_BACKGROUND_COLOR: JBColor = JBColor("PANEL_BACKGROUND_COLOR", Gray._37)

    @JvmField
    val ROBOTO_LIGHT_BOLD_14: JBFont

    @JvmStatic
    fun getPlaceholderForeground(): JBColor {
        return JBColor("PLACEHOLDER_FOREGROUND", Color(
            UIUtil.getTextFieldForeground().red,
            UIUtil.getTextFieldForeground().green,
            UIUtil.getTextFieldForeground().blue,
            100
        ))
    }

    @JvmStatic
    fun getBackgroundColor(): JBColor {
        return JBColor("BACKGROUND_COLOR", EditorColorsManager.getInstance().globalScheme.defaultBackground)
    }

    @JvmStatic
    fun getLabelForeground(): JBColor {
        return JBColor("LABEL_FOREGROUND", JBUI.CurrentTheme.Label.foreground())
    }

    @JvmStatic
    fun getBackgroundUnfocusedColor(): JBColor {
        return JBColor("BACKGROUND_UNFOCUSED_COLOR", LookupCellRenderer.BACKGROUND_COLOR)
    }

    @JvmStatic
    fun getBackgroundFocusColor(): JBColor {
        return JBColor("BACKGROUND_FOCUSED_COLOR", LookupCellRenderer.SELECTED_BACKGROUND_COLOR)
    }

    @JvmStatic
    fun getInputBackgroundColor(): JBColor {
        return JBColor("INPUT_BACKGROUND_COLOR", EditorColorsManager.getInstance().globalScheme.getColor(EditorColors.CARET_ROW_COLOR)!!)
    }

    @JvmStatic
    fun getPanelBorder(): LineBorder {
        return LineBorder(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground())
    }

    private val ROBOTO_LIGHT_PLAIN_13: JBFont
    private val ROBOTO_LIGHT_PLAIN_14: JBFont
    private val ROBOTO_LIGHT_PLAIN_15: JBFont
    private val ROBOTO_LIGHT_PLAIN_16: JBFont
    private val ROBOTO_LIGHT_PLAIN_17: JBFont
    private val MICROSOFT_YAHEI_PLAIN_12: JBFont
    private val MICROSOFT_YAHEI_PLAIN_13: JBFont

    @JvmField
    val MICROSOFT_YAHEI_PLAIN_14: JBFont
    private val MICROSOFT_YAHEI_PLAIN_15: JBFont
    private val MICROSOFT_YAHEI_PLAIN_16: JBFont

    init {
        try {
            val robotoLightFont = JBFont.createFont(
                Font.TRUETYPE_FONT,
                PluginUI::class.java.getResourceAsStream("/fonts/Roboto-Light.ttf")
            )
            ROBOTO_LIGHT_BOLD_14 = JBFont.create(robotoLightFont.deriveFont(Font.BOLD).deriveFont(14f))
            ROBOTO_LIGHT_PLAIN_13 = JBFont.create(robotoLightFont.deriveFont(Font.PLAIN).deriveFont(13f))
            ROBOTO_LIGHT_PLAIN_14 = JBFont.create(robotoLightFont.deriveFont(Font.PLAIN).deriveFont(14f))
            ROBOTO_LIGHT_PLAIN_15 = JBFont.create(robotoLightFont.deriveFont(Font.PLAIN).deriveFont(15f))
            ROBOTO_LIGHT_PLAIN_16 = JBFont.create(robotoLightFont.deriveFont(Font.PLAIN).deriveFont(16f))
            ROBOTO_LIGHT_PLAIN_17 = JBFont.create(robotoLightFont.deriveFont(Font.PLAIN).deriveFont(17f))

            val yaheiFont = JBFont.createFont(
                Font.TRUETYPE_FONT,
                PluginUI::class.java.getResourceAsStream("/fonts/chinese.msyh.ttf")
            )
            MICROSOFT_YAHEI_PLAIN_12 = JBFont.create(yaheiFont.deriveFont(Font.PLAIN).deriveFont(12f))
            MICROSOFT_YAHEI_PLAIN_13 = JBFont.create(yaheiFont.deriveFont(Font.PLAIN).deriveFont(13f))
            MICROSOFT_YAHEI_PLAIN_14 = JBFont.create(yaheiFont.deriveFont(Font.PLAIN).deriveFont(14f))
            MICROSOFT_YAHEI_PLAIN_15 = JBFont.create(yaheiFont.deriveFont(Font.PLAIN).deriveFont(15f))
            MICROSOFT_YAHEI_PLAIN_16 = JBFont.create(yaheiFont.deriveFont(Font.PLAIN).deriveFont(16f))
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    @JvmField
    val BIG_FONT = if (LOCALE.language == "zh") MICROSOFT_YAHEI_PLAIN_16 else ROBOTO_LIGHT_PLAIN_17

    @JvmField
    val SMALL_FONT = if (LOCALE.language == "zh") MICROSOFT_YAHEI_PLAIN_15 else ROBOTO_LIGHT_PLAIN_16

    @JvmField
    val SMALLER_FONT = if (LOCALE.language == "zh") MICROSOFT_YAHEI_PLAIN_14 else ROBOTO_LIGHT_PLAIN_15

    @JvmField
    val SMALLEST_FONT = if (LOCALE.language == "zh") MICROSOFT_YAHEI_PLAIN_13 else ROBOTO_LIGHT_PLAIN_14

    @JvmField
    val SUPER_SMALLEST_FONT = if (LOCALE.language == "zh") MICROSOFT_YAHEI_PLAIN_12 else ROBOTO_LIGHT_PLAIN_13
}
