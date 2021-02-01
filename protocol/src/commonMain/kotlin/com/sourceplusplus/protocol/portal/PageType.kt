package com.sourceplusplus.protocol.portal

import com.sourceplusplus.protocol.Serializers
import kotlinx.serialization.Serializable

/**
 * Contains the available possible portal tabs.
 *
 * @since 0.1.0
 * @author <a href="mailto:bfergerson@apache.org">Brandon Fergerson</a>
 */
@Serializable(with = Serializers.PageTypeSerializer::class)
enum class PageType(val icon: String) {
    OVERVIEW("icon demo-icon satellite"),
    ACTIVITY("icon demo-icon dashboard"),
    TRACES("icon demo-icon code"),
    LOGS("icon demo-icon align left"),
    CONFIGURATION("icon configure");

    val title = name.toLowerCase().capitalize()
}
