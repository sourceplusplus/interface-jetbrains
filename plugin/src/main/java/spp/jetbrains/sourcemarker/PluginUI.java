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

    public static Color getBackgroundDefault() {
        return UIUtil.getLabelBackground();//Color.decode("#252525");
    }

    public static Color getBackgroundFocus() {
        return UIUtil.getListSelectionBackground(true);//Color.decode("#3C3C3C");
    }

    public static Color getEditComplete() {
        return UIUtil.getWindowColor();//Color.decode("#2B2B2B");
    }
}
