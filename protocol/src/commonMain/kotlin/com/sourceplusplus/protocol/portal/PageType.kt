package com.sourceplusplus.protocol.portal

/**
 * Contains the available possible portal tabs.
 *
 * @since 0.0.1
 * @author <a href="mailto:bfergerson@apache.org">Brandon Fergerson</a>
 */
enum class PageType {
    OVERVIEW,
    ACTIVITY,
    TRACES,
    CONFIGURATION;

    val title = name.toLowerCase().capitalize()
    val location = "${name.toLowerCase()}.html"
}
