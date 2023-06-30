/*
 * Source++, the continuous feedback platform for developers.
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
package spp.jetbrains.sourcemarker.command.status.ui;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.UIUtil;
import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.*;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.NotNull;
import spp.jetbrains.UserData;
import spp.jetbrains.icons.PluginIcons;
import spp.jetbrains.marker.source.mark.gutter.GutterMark;
import spp.jetbrains.marker.plugin.LiveStatusBarManager;
import spp.protocol.instrument.LiveMeter;
import spp.protocol.service.listen.LiveInstrumentListener;
import spp.protocol.service.listen.LiveViewEventListener;
import spp.protocol.view.LiveViewEvent;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static spp.jetbrains.PluginBundle.message;
import static spp.jetbrains.PluginUI.*;
import static spp.jetbrains.utils.ViewUtils.addRecursiveMouseListener;

public class LiveMeterStatusPanel extends JBPanel<LiveMeterStatusPanel> implements LiveInstrumentListener, LiveViewEventListener {

    private final LiveMeter liveMeter;
    private final GutterMark gutterMark;

    public LiveMeterStatusPanel(LiveMeter liveMeter, GutterMark gutterMark) {
        this.liveMeter = liveMeter;
        this.gutterMark = gutterMark;

        initComponents();
        setupComponents();

        UIManager.addPropertyChangeListener(propertyChangeEvent -> {
            setBackground(UIUtil.getPanelBackground());
            minuteLabel.setForeground(UIUtil.getLabelForeground());
            minuteValueLabel.setForeground(UIUtil.getLabelForeground());
            hourLabel.setForeground(UIUtil.getLabelForeground());
            hourValueLabel.setForeground(UIUtil.getLabelForeground());
            dayLabel.setForeground(UIUtil.getLabelForeground());
            dayValueLabel.setForeground(UIUtil.getLabelForeground());
        });

//        if (liveMeter.getMeterDescription() != null) {
//            meterDescriptionTextField.setText(liveMeter.getMeterDescription());
//        } else {
//            meterDescriptionTextField.setText(liveMeter.getId());
//        }

        String meterType = liveMeter.getMeterType().name().toLowerCase();
        meterType = meterType.substring(0, 1).toUpperCase() + meterType.substring(1);
        meterTypeValueLabel.setText(message(meterType.toLowerCase()));

        minuteLabel.setText("Value");
        hourLabel.setVisible(false);
        hourValueLabel.setVisible(false);
        dayLabel.setVisible(false);
        dayValueLabel.setVisible(false);
        LiveStatusBarManager.getInstance(gutterMark.getProject()).addViewEventListener(gutterMark, this);
    }

    @Override
    public void accept(@NotNull LiveViewEvent event) {
        JsonObject rawMetrics = new JsonObject(event.getMetricsData());
        String meterValue = rawMetrics.getValue("value").toString();
        if (NumberUtils.isCreatable(meterValue)) {
            minuteValueLabel.setText(getShortNumber(meterValue));
        } else {
            minuteValueLabel.setText(meterValue);
        }
//        hourValueLabel.setText(getShortNumber(rawMetrics.getString("last_hour")));
//        dayValueLabel.setText(getShortNumber(rawMetrics.getString("last_day")));
    }

    private String getShortNumber(String number) {
        long value = Long.parseLong(number);
        if (value > 1000000) {
            return String.format("%.1fM", value / 1000000.0);
        } else if (value > 1000) {
            return String.format("%.1fK", value / 1000.0);
        }
        return String.valueOf(value);
    }

    private void removeActiveDecorations() {
        SwingUtilities.invokeLater(() -> {
            closeLabel.setIcon(PluginIcons.close);
        });
    }

    private void setupComponents() {
        closeLabel.setCursor(Cursor.getDefaultCursor());
        closeLabel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                closeLabel.setIcon(PluginIcons.closeHovered);
            }
        });
        addRecursiveMouseListener(closeLabel, new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                UserData.liveInstrumentService(gutterMark.getProject()).removeLiveInstrument(liveMeter.getId()).onComplete(it -> {
                    if (it.succeeded()) {
                        gutterMark.dispose();
                        LiveStatusBarManager.getInstance(gutterMark.getProject()).removeActiveLiveInstrument(liveMeter);
                    } else {
                        it.cause().printStackTrace();
                    }
                });
            }

            @Override
            public void mousePressed(MouseEvent e) {
                closeLabel.setIcon(PluginIcons.closePressed);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                closeLabel.setIcon(PluginIcons.closeHovered);
            }
        }, () -> {
            removeActiveDecorations();
            return null;
        });
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        panel4 = new JPanel();
        panel1 = new JPanel();
        meterTypeValueLabel = new JBLabel();
        separator1 = new JSeparator();
        minuteLabel = new JBLabel();
        minuteValueLabel = new JBLabel();
        hourLabel = new JBLabel();
        hourValueLabel = new JBLabel();
        panel3 = new JPanel();
        dayLabel = new JBLabel();
        dayValueLabel = new JBLabel();
        panel2 = new JPanel();
        meterDescriptionTextField = new JBTextField();
        closeLabel = new JBLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(new EtchedBorder());
        setFont(SMALLER_FONT);
        setMinimumSize(new Dimension(385, 70));
        setPreferredSize(new Dimension(385, 70));
        setLayout(new FormLayout(
            "default:grow",
            "fill:default:grow"));

        //======== panel4 ========
        {
            panel4.setBackground(null);
            panel4.setLayout(new FormLayout(
                "default:grow",
                "fill:default:grow, 1dlu, default"));

            //======== panel1 ========
            {
                panel1.setBackground(null);
                panel1.setFont(SMALLER_FONT);
                panel1.setLayout(new FormLayout(
                    new ColumnSpec[] {
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        new ColumnSpec(ColumnSpec.CENTER, Sizes.DLUX4, FormSpec.NO_GROW),
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        new ColumnSpec(ColumnSpec.LEFT, Sizes.DEFAULT, FormSpec.DEFAULT_GROW)
                    },
                    RowSpec.decodeSpecs("fill:default:grow")));

                //---- meterTypeValueLabel ----
                meterTypeValueLabel.setText(message("count"));
                meterTypeValueLabel.setFont(SMALL_FONT);
                meterTypeValueLabel.setForeground(LABEL_FOREGROUND_COLOR);
                meterTypeValueLabel.setMinimumSize(new Dimension(46, 25));
                panel1.add(meterTypeValueLabel, cc.xy(1, 1));

                //---- separator1 ----
                separator1.setOrientation(SwingConstants.VERTICAL);
                separator1.setMinimumSize(new Dimension(2, 1));
                separator1.setMaximumSize(new Dimension(2, 1));
                separator1.setPreferredSize(new Dimension(2, 1));
                separator1.setForeground(JBColor.DARK_GRAY);
                panel1.add(separator1, new CellConstraints(3, 1, 1, 1, CellConstraints.DEFAULT, CellConstraints.DEFAULT, new Insets(5, 0, 5, 0)));

                //---- minuteLabel ----
                minuteLabel.setText(WordUtils.capitalize(message("minute")));
                minuteLabel.setFont(SMALLER_FONT);
                panel1.add(minuteLabel, cc.xy(5, 1));

                //---- minuteValueLabel ----
                minuteValueLabel.setText(message("not_available"));
                minuteValueLabel.setFont(SMALL_FONT);
                panel1.add(minuteValueLabel, cc.xy(7, 1));

                //---- hourLabel ----
                hourLabel.setText(WordUtils.capitalize(message("hour")));
                hourLabel.setFont(SMALLER_FONT);
                panel1.add(hourLabel, cc.xy(9, 1));

                //---- hourValueLabel ----
                hourValueLabel.setText(message("not_available"));
                hourValueLabel.setFont(SMALL_FONT);
                panel1.add(hourValueLabel, cc.xy(11, 1));

                //======== panel3 ========
                {
                    panel3.setBackground(null);
                    panel3.setLayout(new FormLayout(
                        new ColumnSpec[] {
                            FormFactory.DEFAULT_COLSPEC,
                            FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                            FormFactory.DEFAULT_COLSPEC
                        },
                        RowSpec.decodeSpecs("fill:default:grow")));

                    //---- dayLabel ----
                    dayLabel.setText(WordUtils.capitalize(message("day")));
                    dayLabel.setFont(SMALLER_FONT);
                    panel3.add(dayLabel, cc.xy(1, 1));

                    //---- dayValueLabel ----
                    dayValueLabel.setText(message("not_available"));
                    dayValueLabel.setFont(SMALL_FONT);
                    panel3.add(dayValueLabel, cc.xy(3, 1));
                }
                panel1.add(panel3, cc.xy(13, 1));
            }
            panel4.add(panel1, new CellConstraints(1, 1, 1, 1, CellConstraints.DEFAULT, CellConstraints.DEFAULT, new Insets(2, 4, 2, 2)));

            //======== panel2 ========
            {
                panel2.setBackground(null);
                panel2.setLayout(new FormLayout(
                    new ColumnSpec[] {
                        new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC
                    },
                    RowSpec.decodeSpecs("default")));

                //---- meterDescriptionTextField ----
                meterDescriptionTextField.setEditable(false);
                panel2.add(meterDescriptionTextField, cc.xy(1, 1));

                //---- closeLabel ----
                closeLabel.setIcon(PluginIcons.close);
                panel2.add(closeLabel, cc.xy(3, 1));
            }
            panel4.add(panel2, cc.xy(1, 3));
        }
        add(panel4, new CellConstraints(1, 1, 1, 1, CellConstraints.DEFAULT, CellConstraints.DEFAULT, new Insets(5, 5, 5, 10)));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel panel4;
    private JPanel panel1;
    private JBLabel meterTypeValueLabel;
    private JSeparator separator1;
    private JBLabel minuteLabel;
    private JBLabel minuteValueLabel;
    private JBLabel hourLabel;
    private JBLabel hourValueLabel;
    private JPanel panel3;
    private JBLabel dayLabel;
    private JBLabel dayValueLabel;
    private JPanel panel2;
    private JBTextField meterDescriptionTextField;
    private JBLabel closeLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
