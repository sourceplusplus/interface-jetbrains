package spp.jetbrains.sourcemarker;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public interface PluginIcons {
    Icon expand = IconLoader.getIcon("/icons/expand.svg", PluginIcons.class);
    Icon expandHovered = IconLoader.getIcon("/icons/expandHovered.svg", PluginIcons.class);
    Icon expandPressed = IconLoader.getIcon("/icons/expandPressed.svg", PluginIcons.class);

    Icon close = IconLoader.getIcon("/icons/closeIcon.svg", PluginIcons.class);
    Icon closeHovered = IconLoader.getIcon("/icons/closeIconHovered.svg", PluginIcons.class);
    Icon closePressed = IconLoader.getIcon("/icons/closeIconPressed.svg", PluginIcons.class);

    Icon clock = IconLoader.getIcon("/icons/clock.svg", PluginIcons.class);
    Icon alignLeft = IconLoader.getIcon("/icons/align-left.svg", PluginIcons.class);
    Icon angleDown = IconLoader.getIcon("/icons/angle-down.svg", PluginIcons.class);

    Icon eye = IconLoader.getIcon("/icons/eye.svg", PluginIcons.class);
    Icon eyeSlash = IconLoader.getIcon("/icons/eye-slash.svg", PluginIcons.class);

    Icon analytics = IconLoader.getIcon("/icons/analytics.svg", PluginIcons.class);

    Icon config = IconLoader.getIcon("/icons/configIcon.svg", PluginIcons.class);
    Icon configHovered = IconLoader.getIcon("/icons/configIconHovered.svg", PluginIcons.class);
    Icon configPressed = IconLoader.getIcon("/icons/configIconPressed.svg", PluginIcons.class);

    Icon exclamationTriangle = IconLoader.getIcon("/icons/exclamation-triangle.svg", PluginIcons.class);
    Icon performanceRamp = IconLoader.getIcon("/icons/sort-amount-up.svg", PluginIcons.class);
    Icon activeException = IconLoader.getIcon("/icons/map-marker-exclamation.svg", PluginIcons.class);
    Icon count = IconLoader.getIcon("/icons/count.svg", PluginIcons.class);
    Icon gauge = IconLoader.getIcon("/icons/gauge.svg", PluginIcons.class);
    Icon histogram = IconLoader.getIcon("/icons/histogram.svg", PluginIcons.class);
    Icon code = IconLoader.getIcon("/icons/code.svg", PluginIcons.class);
    Icon tachometer = IconLoader.getIcon("/icons/tachometer-alt.svg", PluginIcons.class);

    interface Nodes {
        Icon variable = IconLoader.getIcon("/nodes/variable.png", PluginIcons.class);
    }

    interface Command {
        Icon logo = IconLoader.getIcon("/icons/command/logo.svg", PluginIcons.class);
        Icon clearInstrumentSelected = IconLoader.getIcon("/icons/command/clear-instruments_selected.svg", PluginIcons.class);
        Icon clearInstrumentUnSelected = IconLoader.getIcon("/icons/command/clear-instruments_unselected.svg", PluginIcons.class);

        Icon liveBreakpointSelected = IconLoader.getIcon("/icons/command/live-breakpoint_selected.svg", PluginIcons.class);
        Icon liveBreakpointUnSelected = IconLoader.getIcon("/icons/command/live-breakpoint_unselected.svg", PluginIcons.class);

        Icon livelogSelected = IconLoader.getIcon("/icons/command/live-log_selected.svg", PluginIcons.class);
        Icon livelogUnSelected = IconLoader.getIcon("/icons/command/live-log_unselected.svg", PluginIcons.class);

        Icon liveMeterSelected = IconLoader.getIcon("/icons/command/live-meter_selected.svg", PluginIcons.class);
        Icon liveMeterUnSelected = IconLoader.getIcon("/icons/command/live-meter_unselected.svg", PluginIcons.class);

        Icon liveSpanSelected = IconLoader.getIcon("/icons/command/live-span_selected.svg", PluginIcons.class);
        Icon liveSpanUnSelected = IconLoader.getIcon("/icons/command/live-span_unselected.svg", PluginIcons.class);

        Icon viewActivitySelected = IconLoader.getIcon("/icons/command/view-activity_selected.svg", PluginIcons.class);
        Icon viewActivityUnSelected = IconLoader.getIcon("/icons/command/view-activity_unselected.svg", PluginIcons.class);
        Icon viewTracesSelected = IconLoader.getIcon("/icons/command/view-traces_selected.svg", PluginIcons.class);
        Icon viewTracesUnSelected = IconLoader.getIcon("/icons/command/view-traces_unselected.svg", PluginIcons.class);
        Icon viewLogsSelected = IconLoader.getIcon("/icons/command/view-logs_selected.svg", PluginIcons.class);
        Icon viewLogsUnSelected = IconLoader.getIcon("/icons/command/view-logs_unselected.svg", PluginIcons.class);
    }

    interface Instrument {
        Icon save = IconLoader.getIcon("/icons/instrument/live-log/save.svg", PluginIcons.class);
        Icon saveHovered = IconLoader.getIcon("/icons/instrument/live-log/saveHovered.svg", PluginIcons.class);
        Icon savePressed = IconLoader.getIcon("/icons/instrument/live-log/savePressed.svg", PluginIcons.class);
    }

    interface Breakpoint {
        Icon active = IconLoader.getIcon("/icons/breakpoint/live-breakpoint-active.svg", PluginIcons.class);
        Icon complete = IconLoader.getIcon("/icons/breakpoint/live-breakpoint-complete.svg", PluginIcons.class);
        Icon disabled = IconLoader.getIcon("/icons/breakpoint/live-breakpoint-disabled.svg", PluginIcons.class);
        Icon error = IconLoader.getIcon("/icons/breakpoint/live-breakpoint-error.svg", PluginIcons.class);
        Icon pending = IconLoader.getIcon("/icons/breakpoint/live-breakpoint-pending.svg", PluginIcons.class);
    }
}
