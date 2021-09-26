package com.sourceplusplus.sourcemarker.command

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("unused")
enum class CommandAction(
    val command: String,
    private val description: String,
    val selectedIcon: Icon? = null,
    val unselectedIcon: Icon? = null
) : AutocompleteFieldRow {

    VIEW_ACTIVITY(
        "view-activity",
        "View method activity"
    ),
    WATCH_VARIABLE(
        "watch",
        "Manual Tracing ➛ Watched Variables ➛ Scope: Local / Add *variable* to watched variables"
    ),
    TRACE_METHOD(
        "trace",
        "Add method to distributed tracing system"
    ),
    ADD_LIVE_BREAKPOINT(
        "Add Breakpoint",
        "<html><span style=\"font-size: 80%; color: gray\">Live Instrument ➛ Add ➛ Location: </span><span style=\"font-size: 80%; color: #E6E6E6\">On line *lineNumber*</span></html>",
        IconLoader.findIcon("/icons/command/live-breakpoint_selected.svg"),
        IconLoader.findIcon("/icons/command/live-breakpoint_unselected.svg")
    ),
    ADD_LIVE_LOG(
        "Add Log",
        "<html><span style=\"font-size: 80%; color: gray\">Live Instrument ➛ Add ➛ Location: </span><span style=\"font-size: 80%; color: #E6E6E6\">After line *lineNumber*</span></html>",
        IconLoader.findIcon("/icons/command/live-log_selected.svg"),
        IconLoader.findIcon("/icons/command/live-log_unselected.svg")
    ),
    CLEAR_LIVE_BREAKPOINTS(
        "Clear Breakpoints",
        "Clear all self-created live breakpoints",
        IconLoader.findIcon("/icons/command/clear-instruments_selected.svg"),
        IconLoader.findIcon("/icons/command/clear-instruments_unselected.svg")
    ),
    CLEAR_LIVE_INSTRUMENTS(
        "Clear Instruments",
        "<html><span style=\"font-size: 80%; color: gray\">Live Instrument ➛ Clear All</span></html>",
        IconLoader.findIcon("/icons/command/clear-instruments_selected.svg"),
        IconLoader.findIcon("/icons/command/clear-instruments_unselected.svg")
    ),
    CLEAR_LIVE_LOGS(
        "Clear Logs",
        "Clear all self-created live logs",
        IconLoader.findIcon("/icons/command/clear-instruments_selected.svg"),
        IconLoader.findIcon("/icons/command/clear-instruments_unselected.svg")
    );

    override fun getText(): String {
        return command
    }

    override fun getDescription(): String? {
        return description
    }

    override fun getIcon(): Icon? {
        return unselectedIcon
    }
}
