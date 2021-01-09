package com.sourceplusplus.portal.page

import com.sourceplusplus.portal.IPortalPage
import com.sourceplusplus.portal.extensions.jq
import com.sourceplusplus.protocol.artifact.QueryTimeFrame

/**
 * todo: description.
 *
 * @since 0.1.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface PortalPage : IPortalPage {

    fun setActiveTime(interval: QueryTimeFrame) {
        jq("#last_5_minutes_time").removeClass("active")
        jq("#last_15_minutes_time").removeClass("active")
        jq("#last_30_minutes_time").removeClass("active")
        jq("#last_hour_time").removeClass("active")
        jq("#last_3_hours_time").removeClass("active")
        jq("#" + interval.name.toLowerCase() + "_time").addClass("active")
    }
}
