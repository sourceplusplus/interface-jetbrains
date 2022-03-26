package spp.jetbrains.sourcemarker;

import com.intellij.DynamicBundle;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;

import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.IOException;

public class PluginUI {

    public static final LineBorder PANEL_BORDER = new LineBorder(Gray._85);

    public static Font ROBOTO_LIGHT_BOLD_14;
    public static Font ROBOTO_LIGHT_PLAIN_14;
    public static Font ROBOTO_LIGHT_PLAIN_15;
    public static Font ROBOTO_LIGHT_PLAIN_16;
    public static Font ROBOTO_LIGHT_PLAIN_17;
    public static Font MICROSOFT_YAHEI_PLAIN_13;
    public static Font MICROSOFT_YAHEI_PLAIN_14;
    public static Font MICROSOFT_YAHEI_PLAIN_15;
    public static Font MICROSOFT_YAHEI_PLAIN_16;

    static {
        try {
            Font ROBOTO_LIGHT = Font.createFont(Font.TRUETYPE_FONT, PluginUI.class.getResourceAsStream("/fonts/Roboto-Light.ttf"));
            ROBOTO_LIGHT_BOLD_14 = ROBOTO_LIGHT.deriveFont(Font.BOLD).deriveFont(14f);
            ROBOTO_LIGHT_PLAIN_14 = ROBOTO_LIGHT.deriveFont(Font.PLAIN).deriveFont(14f);
            ROBOTO_LIGHT_PLAIN_15 = ROBOTO_LIGHT.deriveFont(Font.PLAIN).deriveFont(15f);
            ROBOTO_LIGHT_PLAIN_16 = ROBOTO_LIGHT.deriveFont(Font.PLAIN).deriveFont(16f);
            ROBOTO_LIGHT_PLAIN_17 = ROBOTO_LIGHT.deriveFont(Font.PLAIN).deriveFont(17f);

            Font YAHEI = Font.createFont(Font.TRUETYPE_FONT, PluginUI.class.getResourceAsStream("/fonts/chinese.msyh.ttf"));
            MICROSOFT_YAHEI_PLAIN_13 =  YAHEI.deriveFont(Font.PLAIN).deriveFont(13f);
            MICROSOFT_YAHEI_PLAIN_14 =  YAHEI.deriveFont(Font.PLAIN).deriveFont(14f);
            MICROSOFT_YAHEI_PLAIN_15 =  YAHEI.deriveFont(Font.PLAIN).deriveFont(15f);
            MICROSOFT_YAHEI_PLAIN_16 =  YAHEI.deriveFont(Font.PLAIN).deriveFont(16f);
        } catch (FontFormatException | IOException e) {
            e.printStackTrace();
        }
    }

    public static final Font BIG_FONT = (DynamicBundle.getLocale().getLanguage().equals("zh")) ? MICROSOFT_YAHEI_PLAIN_16 : ROBOTO_LIGHT_PLAIN_17;
    public static final Font SMALL_FONT = (DynamicBundle.getLocale().getLanguage().equals("zh")) ? MICROSOFT_YAHEI_PLAIN_15 : ROBOTO_LIGHT_PLAIN_16;
    public static final Font SMALLER_FONT = (DynamicBundle.getLocale().getLanguage().equals("zh")) ? MICROSOFT_YAHEI_PLAIN_14 : ROBOTO_LIGHT_PLAIN_15;
    public static final Font SMALLEST_FONT = (DynamicBundle.getLocale().getLanguage().equals("zh")) ? MICROSOFT_YAHEI_PLAIN_13 : ROBOTO_LIGHT_PLAIN_14;
    public static final Color PANEL_BACKGROUND_COLOR = Gray._37;
    public static final Color LABEL_FOREGROUND_COLOR = new Color(152, 118, 170);
    public static final Color LABEL_FOREGROUND_COLOR1 = new Color(106, 135, 89);
    public static final Color EXPIRY_FOREGROUND_COLOR = Color.decode("#BBBBBB");
    public static final Color SELECT_COLOR_RED = Color.decode("#e1483b");
    public static final Color COMPLETE_COLOR_PURPLE = Color.decode("#9876AA");
    public static final JBColor STATUS_BAR_TXT_BG_COLOR = new JBColor(JBColor.WHITE, Gray._37);
    public static final JBColor AUTO_COMPLETE_HIGHLIGHT_COLOR = new JBColor(new Color(10, 108, 161), Gray._25);
    public static final JBColor CONTROL_BAR_CMD_FOREGROUND = new JBColor(JBColor.BLACK, JBColor.GRAY);
    public static final JBColor DFLT_BGND_COLOR = new JBColor(Gray._242, Gray._50);
    public static final JBColor CNFG_PANEL_BGND_COLOR = new JBColor(Gray._242, Gray._37);
    public static final JBColor CNFG_PANEL_FOCUS_COLOR = new JBColor(new Color(10, 108, 161), Gray._0);
    public static final JBColor BGND_FOCUS_COLOR = new JBColor(new Color(10, 108, 161), Gray._25);

    public static Color getEditCompleteColor() {
        return UIUtil.getWindowColor();//Color.decode("#2B2B2B");
    }
}
