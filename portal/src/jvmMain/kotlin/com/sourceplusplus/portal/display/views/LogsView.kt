package com.sourceplusplus.portal.display.views

import com.sourceplusplus.protocol.artifact.log.LogOrderType
import com.sourceplusplus.protocol.artifact.log.LogResult

/**
 * Holds the current view for the Logs portal tab.
 *
 * @since 0.1.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LogsView {

    var orderType = LogOrderType.NEWEST_LOGS
    var logResult: LogResult? = null

    fun cloneView(view: LogsView) {
        orderType = view.orderType
        logResult = view.logResult
    }
}
