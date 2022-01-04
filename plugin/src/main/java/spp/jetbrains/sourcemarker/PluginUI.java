package spp.jetbrains.sourcemarker;

import com.intellij.util.ui.UIUtil;

import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.IOException;

public class PluginUI {

    public static final LineBorder PANEL_BORDER = new LineBorder(new Color(85, 85, 85));

    public static Font ROBOTO_PLAIN_15;
    public static Font ROBOTO_PLAIN_11;
    public static Font ROBOTO_LIGHT_BOLD_14;
    public static Font ROBOTO_LIGHT_PLAIN_14;
    public static Font ROBOTO_LIGHT_PLAIN_15;
    public static Font ROBOTO_LIGHT_PLAIN_16;

    static {
        try {
            Font ROBOTO = Font.createFont(Font.TRUETYPE_FONT, PluginUI.class.getResourceAsStream("/fonts/Roboto-Regular.ttf"));
            Font ROBOTO_LIGHT = Font.createFont(Font.TRUETYPE_FONT, PluginUI.class.getResourceAsStream("/fonts/Roboto-Light.ttf"));

            ROBOTO_PLAIN_15 = ROBOTO.deriveFont(Font.PLAIN).deriveFont(15f);
            ROBOTO_PLAIN_11 = ROBOTO.deriveFont(Font.PLAIN).deriveFont(11f);
            ROBOTO_LIGHT_BOLD_14 = ROBOTO_LIGHT.deriveFont(Font.BOLD).deriveFont(14f);
            ROBOTO_LIGHT_PLAIN_14 = ROBOTO_LIGHT.deriveFont(Font.PLAIN).deriveFont(14f);
            ROBOTO_LIGHT_PLAIN_15 = ROBOTO_LIGHT.deriveFont(Font.PLAIN).deriveFont(15f);
            ROBOTO_LIGHT_PLAIN_16 = ROBOTO_LIGHT.deriveFont(Font.PLAIN).deriveFont(16f);
        } catch (FontFormatException | IOException e) {
            e.printStackTrace();
        }
    }

    public static final Color PANEL_BACKGROUND_COLOR = new Color(37, 37, 37);
    public static final Color LABEL_FOREGROUND_COLOR = new Color(152, 118, 170);
    public static final Color LABEL_FOREGROUND_COLOR1 = new Color(106, 135, 89);
    public static final Color EXPIRY_FOREGROUND_COLOR = Color.decode("#BBBBBB");
    public static final Color SELECT_COLOR_RED = Color.decode("#e1483b");
    public static final Color COMPLETE_COLOR_PURPLE = Color.decode("#9876AA");
    public static final Color AUTO_COMPLETE_SELECT_BACKGROUND = Color.decode("#1C1C1C");

    public static Color getBackgroundDefaultColor() {
        return UIUtil.getLabelBackground();//Color.decode("#252525");
    }

    public static Color getBackgroundFocusColor() {
        return UIUtil.getListSelectionBackground(true);//Color.decode("#3C3C3C");
    }

    public static Color getEditCompleteColor() {
        return UIUtil.getWindowColor();//Color.decode("#2B2B2B");
    }

}
