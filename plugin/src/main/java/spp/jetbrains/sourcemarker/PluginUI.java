package spp.jetbrains.sourcemarker;

import com.intellij.util.ui.UIUtil;

import javax.swing.border.LineBorder;
import java.awt.*;

public class PluginUI {

    public static final LineBorder PANEL_BORDER = new LineBorder(new Color(85, 85, 85));
    public static final Font ROBOTO_LIGHT_BOLD_14 = new Font("Roboto Light", Font.BOLD, 14);
    public static final Font ROBOTO_LIGHT_PLAIN_14 = new Font("Roboto Light", Font.PLAIN, 14);
    public static final Font ROBOTO_LIGHT_PLAIN_15 = new Font("Roboto Light", Font.PLAIN, 15);
    public static final Font ROBOTO_LIGHT_PLAIN_16 = new Font("Roboto Light", Font.PLAIN, 16);
    public static final Font ROBOTO_PLAIN_15 = new Font("Roboto", Font.PLAIN, 15);
    public static final Font ROBOTO_PLAIN_11 = new Font("Roboto", Font.PLAIN, 11);

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
