package com.sourceplusplus.portal.display.views

import com.sourceplusplus.portal.SourcePortal
import spp.protocol.artifact.log.LogOrderType
import spp.protocol.artifact.log.LogResult
import spp.protocol.artifact.log.LogViewType

/**
 * Holds the current view for the Logs portal tab.
 *
 * @since 0.2.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LogsView(
    val portal: SourcePortal
) {

    var viewType = LogViewType.LIVE_TAIL
    var orderType = LogOrderType.NEWEST_LOGS
    var logResult: LogResult? = null
    val viewLogAmount = if (portal.configuration.external) 20 else 10
    var pageNumber = 1

    fun cacheArtifactLogResult(artifactTraceResult: LogResult) {
        val mergedLogResult = logResult?.mergeWith(artifactTraceResult) ?: artifactTraceResult
        logResult = if (pageNumber == 1) {
            mergedLogResult.truncate(viewLogAmount)
        } else {
            mergedLogResult
        }
    }

    fun cloneView(view: LogsView) {
        viewType = view.viewType
        orderType = view.orderType
        logResult = view.logResult
    }
}
