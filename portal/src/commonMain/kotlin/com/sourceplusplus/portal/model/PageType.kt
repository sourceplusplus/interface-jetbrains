package com.sourceplusplus.portal.model

/**
 * Contains the available possible portal tabs.
 *
 * @since 0.1.0
 * @author <a href="mailto:bfergerson@apache.org">Brandon Fergerson</a>
 */
enum class PageType(val icon: String) {
    OVERVIEW("icon demo-icon satellite"),
    ACTIVITY("icon demo-icon dashboard"),
    TRACES("icon demo-icon code"),
    CONFIGURATION("icon configure");

    val title = name.toLowerCase().capitalize()
    val location = "${name.toLowerCase()}.html"
}
