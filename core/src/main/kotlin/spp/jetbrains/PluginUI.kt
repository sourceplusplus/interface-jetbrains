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
package spp.jetbrains

import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.util.ui.UIUtil
import spp.jetbrains.PluginBundle.LOCALE
import java.awt.Color
import java.awt.Font
import javax.swing.border.LineBorder

object PluginUI {

    @JvmField
    val LABEL_FOREGROUND_COLOR = Color(152, 118, 170)

    @JvmField
    val LABEL_FOREGROUND_COLOR1 = Color(106, 135, 89)

    @JvmField
    val EXPIRY_FOREGROUND_COLOR: Color = Color.decode("#BBBBBB")

    @JvmField
    val SELECT_COLOR_RED: Color = Color.decode("#e1483b")

    @JvmField
    val COMPLETE_COLOR_PURPLE: Color = Color.decode("#9876AA")

    @JvmField
    val STATUS_BAR_TXT_BG_COLOR = JBColor(JBColor.WHITE, Gray._37)

    @JvmField
    val CONTROL_BAR_CMD_FOREGROUND = JBColor(JBColor.BLACK, JBColor.GRAY)

    @JvmField
    val DFLT_BGND_COLOR = JBColor(Gray._242, Gray._50)

    @JvmField
    val CNFG_PANEL_BGND_COLOR = JBColor(Gray._242, Gray._37)

    @JvmField
    val BGND_FOCUS_COLOR = JBColor(Gray._175, Gray._25)

    @JvmField
    val COMMAND_TYPE_COLOR = JBColor(JBColor.BLACK, Gray._125)

    @JvmField
    val COMMAND_HIGHLIGHT_COLOR = JBColor(SELECT_COLOR_RED, Color.decode("#E6E6E6"))

    @JvmStatic
    val commandTypeColor: String
        get() = "#" + Integer.toHexString(COMMAND_TYPE_COLOR.rgb).substring(2)

    @JvmStatic
    val commandHighlightColor: String
        get() = "#" + Integer.toHexString(COMMAND_HIGHLIGHT_COLOR.rgb).substring(2)

    @JvmStatic
    val editCompleteColor: Color
        get() = UIUtil.getWindowColor()

    @JvmField
    val PANEL_BACKGROUND_COLOR: Color = Gray._37

    @JvmField
    val PANEL_BORDER = LineBorder(Gray._85)

    @JvmField
    val ROBOTO_LIGHT_BOLD_14: Font

    private val ROBOTO_LIGHT_PLAIN_13: Font
    private val ROBOTO_LIGHT_PLAIN_14: Font
    private val ROBOTO_LIGHT_PLAIN_15: Font
    private val ROBOTO_LIGHT_PLAIN_16: Font
    private val ROBOTO_LIGHT_PLAIN_17: Font
    private val MICROSOFT_YAHEI_PLAIN_12: Font
    private val MICROSOFT_YAHEI_PLAIN_13: Font

    @JvmField
    val MICROSOFT_YAHEI_PLAIN_14: Font
    private val MICROSOFT_YAHEI_PLAIN_15: Font
    private val MICROSOFT_YAHEI_PLAIN_16: Font

    init {
        try {
            val robotoLightFont = Font.createFont(
                Font.TRUETYPE_FONT,
                PluginUI::class.java.getResourceAsStream("/fonts/Roboto-Light.ttf")
            )
            ROBOTO_LIGHT_BOLD_14 = robotoLightFont.deriveFont(Font.BOLD).deriveFont(14f)
            ROBOTO_LIGHT_PLAIN_13 = robotoLightFont.deriveFont(Font.PLAIN).deriveFont(13f)
            ROBOTO_LIGHT_PLAIN_14 = robotoLightFont.deriveFont(Font.PLAIN).deriveFont(14f)
            ROBOTO_LIGHT_PLAIN_15 = robotoLightFont.deriveFont(Font.PLAIN).deriveFont(15f)
            ROBOTO_LIGHT_PLAIN_16 = robotoLightFont.deriveFont(Font.PLAIN).deriveFont(16f)
            ROBOTO_LIGHT_PLAIN_17 = robotoLightFont.deriveFont(Font.PLAIN).deriveFont(17f)

            val yaheiFont = Font.createFont(
                Font.TRUETYPE_FONT,
                PluginUI::class.java.getResourceAsStream("/fonts/chinese.msyh.ttf")
            )
            MICROSOFT_YAHEI_PLAIN_12 = yaheiFont.deriveFont(Font.PLAIN).deriveFont(12f)
            MICROSOFT_YAHEI_PLAIN_13 = yaheiFont.deriveFont(Font.PLAIN).deriveFont(13f)
            MICROSOFT_YAHEI_PLAIN_14 = yaheiFont.deriveFont(Font.PLAIN).deriveFont(14f)
            MICROSOFT_YAHEI_PLAIN_15 = yaheiFont.deriveFont(Font.PLAIN).deriveFont(15f)
            MICROSOFT_YAHEI_PLAIN_16 = yaheiFont.deriveFont(Font.PLAIN).deriveFont(16f)
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
