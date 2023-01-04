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

import com.codahale.metrics.Meter;
import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.*;
import spp.protocol.utils.TimeUtilsKt;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static spp.jetbrains.PluginBundle.message;
import static spp.jetbrains.PluginUI.*;

public class LiveBreakpointStatusPanel extends JPanel {

    private final Meter meter = new Meter();
    private int hitLimit;

    public LiveBreakpointStatusPanel() {
        initComponents();
    }

    public void setHitLimit(int hitLimit) {
        this.hitLimit = hitLimit;
    }

    public void setStatus(String status, Color statusColor) {
        statusValueLabel.setText(status);
        statusValueLabel.setForeground(statusColor);

        if ("Error".equals(status) || "Complete".equals(status)) {
            expiresLabel.setVisible(false);
            expiresValueLabel.setVisible(false);

            if (hitLimit == 1) {
                rateLabel.setVisible(false);
                rateValueLabel.setVisible(false);
            }
        }
    }

    public void incrementHits() {
        meter.mark();
        setHits(meter.getCount());
        setRate(meter.getMeanRate());
    }

    private void setHits(long hits) {
        hitsValueLabel.setText(Long.toString(hits));
    }

    private void setRate(double rate) {
        rateValueLabel.setText(TimeUtilsKt.fromPerSecondToPrettyFrequency(rate));
    }

    public void setExpires(Long expiresAt) {
        if (expiresAt == null || expiresAt == -1) {
            expiresValueLabel.setForeground(EXPIRY_FOREGROUND_COLOR);
            expiresValueLabel.setText("n/a");
            return;
        }
        updateExpiresLabel(expiresAt);

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(() -> {
            if (expiresValueLabel.isVisible()) {
                updateExpiresLabel(expiresAt);
            } else {
                executor.shutdown();
            }
        }, 0, 1, TimeUnit.SECONDS);

        new Thread(() -> {
            try {
                Thread.sleep(expiresAt - System.currentTimeMillis());
            } catch (InterruptedException ignored) {
            }
            executor.shutdown();
        }).start();
    }

    private void updateExpiresLabel(long expiresTime) {
        long diffMs = expiresTime - System.currentTimeMillis();
        long diffSec = diffMs / 1000;
        long min = diffSec / 60;
        long sec = diffSec % 60;
        if (min > 0) {
            expiresValueLabel.setForeground(EXPIRY_FOREGROUND_COLOR);
            expiresValueLabel.setText(min + message("min") + " " + sec + message("sec_letter"));
        } else {
            expiresValueLabel.setForeground(SELECT_COLOR_RED);
            expiresValueLabel.setText(sec + message("sec_letter"));
        }
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        panel1 = new JPanel();
        statusLabel = new JLabel();
        statusValueLabel = new JLabel();
        separator1 = new JSeparator();
        hitsLabel = new JLabel();
        hitsValueLabel = new JLabel();
        rateLabel = new JLabel();
        rateValueLabel = new JLabel();
        expiresLabel = new JLabel();
        expiresValueLabel = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========

        //setBorder(PluginUI.PANEL_BORDER);
        setFont(SMALLER_FONT);
        setLayout(new FormLayout(
            "default:grow",
            "fill:default:grow"));

        //======== panel1 ========
        {
            panel1.setBackground(null);
            panel1.setFont(SMALLER_FONT);
            panel1.setLayout(new FormLayout(
                new ColumnSpec[] {
                    FormFactory.DEFAULT_COLSPEC,
                    FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                    FormFactory.DEFAULT_COLSPEC,
                    FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                    new ColumnSpec(ColumnSpec.CENTER, Sizes.DEFAULT, FormSpec.NO_GROW),
                    FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                    FormFactory.DEFAULT_COLSPEC,
                    FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                    FormFactory.DEFAULT_COLSPEC,
                    FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                    FormFactory.DEFAULT_COLSPEC,
                    FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                    FormFactory.DEFAULT_COLSPEC,
                    FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                    FormFactory.DEFAULT_COLSPEC,
                    FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                    FormFactory.DEFAULT_COLSPEC
                },
                RowSpec.decodeSpecs("fill:default:grow")));

            //---- statusLabel ----
            statusLabel.setText(message("status"));
            statusLabel.setFont(SMALLER_FONT);
            panel1.add(statusLabel, cc.xy(1, 1));

            //---- statusValueLabel ----
            statusValueLabel.setText(message("active"));
            statusValueLabel.setFont(SMALLER_FONT.deriveFont(Font.BOLD));
            statusValueLabel.setForeground(LABEL_FOREGROUND_COLOR1);
            panel1.add(statusValueLabel, cc.xy(3, 1));

            //---- separator1 ----
            separator1.setOrientation(SwingConstants.VERTICAL);
            separator1.setMinimumSize(new Dimension(2, 1));
            separator1.setMaximumSize(new Dimension(2, 1));
            separator1.setPreferredSize(new Dimension(2, 1));
            separator1.setForeground(Color.darkGray);
            panel1.add(separator1, new CellConstraints(5, 1, 1, 1, CellConstraints.DEFAULT, CellConstraints.DEFAULT, new Insets(5, 0, 5, 0)));

            //---- hitsLabel ----
            hitsLabel.setText(message("hits"));
            hitsLabel.setFont(SMALLER_FONT);
            panel1.add(hitsLabel, cc.xy(7, 1));

            //---- hitsValueLabel ----
            hitsValueLabel.setText(message("not_available"));
            hitsValueLabel.setFont(SMALLER_FONT.deriveFont(Font.BOLD));
            panel1.add(hitsValueLabel, cc.xy(9, 1));

            //---- rateLabel ----
            rateLabel.setText(message("rate"));
            rateLabel.setFont(SMALLER_FONT);
            panel1.add(rateLabel, cc.xy(11, 1));

            //---- rateValueLabel ----
            rateValueLabel.setText(message("not_available"));
            rateValueLabel.setFont(SMALLER_FONT.deriveFont(Font.BOLD));
            panel1.add(rateValueLabel, cc.xy(13, 1));

            //---- expiresLabel ----
            expiresLabel.setText(message("expires"));
            expiresLabel.setFont(SMALLER_FONT);
            panel1.add(expiresLabel, cc.xy(15, 1));

            //---- expiresValueLabel ----
            expiresValueLabel.setText(message("not_available"));
            expiresValueLabel.setFont(SMALLER_FONT.deriveFont(Font.BOLD));
            panel1.add(expiresValueLabel, cc.xy(17, 1));
        }
        add(panel1, new CellConstraints(1, 1, 1, 1, CellConstraints.DEFAULT, CellConstraints.DEFAULT, new Insets(2, 4, 2, 2)));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel panel1;
    private JLabel statusLabel;
    private JLabel statusValueLabel;
    private JSeparator separator1;
    private JLabel hitsLabel;
    private JLabel hitsValueLabel;
    private JLabel rateLabel;
    private JLabel rateValueLabel;
    private JLabel expiresLabel;
    private JLabel expiresValueLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
