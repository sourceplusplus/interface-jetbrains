package com.sourceplusplus.portal.display.views

import spp.protocol.artifact.QueryTimeFrame

/**
 * Holds the current view for the Overview portal tab.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class OverviewView {

    var timeFrame = QueryTimeFrame.LAST_5_MINUTES

    fun cloneView(view: OverviewView) {
        timeFrame = view.timeFrame
    }
}
