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
enum class PageType(val icon: String, var hasChildren: Boolean) {
    OVERVIEW("icon demo-icon satellite", false),
    ACTIVITY("icon demo-icon dashboard", false),
    TRACES("icon demo-icon code", true),
    LOGS("icon demo-icon align left", false),
    CONFIGURATION("icon configure", false);

    val title = name.toLowerCase().capitalize()
}
