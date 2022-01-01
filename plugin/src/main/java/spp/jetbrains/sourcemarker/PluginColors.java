package spp.jetbrains.sourcemarker;

import com.intellij.util.ui.UIUtil;

import java.awt.Color;

public class PluginColors {

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
