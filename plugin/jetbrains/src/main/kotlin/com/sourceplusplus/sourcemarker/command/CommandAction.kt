package com.sourceplusplus.sourcemarker.command

import javax.swing.Icon

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("unused")
enum class CommandAction(
    private val command: String,
    private val description: String
) : AutocompleteFieldRow {

//    ADD_LIVE_BREAKPOINT(
//        "/add-live-breakpoint",
//        "Add live breakpoint instrument after line *lineNumber*"
//    ),
    ADD_LIVE_LOG(
        "/add-live-log",
        "Add live log instrument after line *lineNumber*"
    ),
//    CLEAR_LIVE_BREAKPOINTS(
//        "/clear-live-breakpoints",
//        "Clear all self-created live breakpoints"
//    ),
    CLEAR_LIVE_INSTRUMENTS(
        "/clear-live-instruments",
        "Clear all self-created live instruments"
    );
//    CLEAR_LIVE_LOGS(
//        "/clear-live-logs",
//        "Clear all self-created live logs"
//    );

    override fun getText(): String = command
    override fun getDescription(): String? = description
    override fun getIcon(): Icon? = null
}
